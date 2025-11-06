package com.matillion.techtest2025;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matillion.techtest2025.repository.DataAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Part 3 – Type inference, descriptive statistics, Data Quality Score (DQS),
 * and Outlier Detection (IQR).
 *
 * Conventions this test expects from the /api/analysis/{id}/stats endpoint:
 * - Response JSON:
 * {
 * "id": <long>,
 * "columns": [
 * {
 * "columnName": "age",
 * "dataType": "INTEGER", // or DECIMAL/BOOLEAN/DATE/STRING
 * "nullCount": 0,
 * "uniqueCount": 3,
 * // numeric
 * "min": 18.0, "max": 40.0, "mean": 26.666, "median": 22.0, "stddev": 9.055,
 * // boolean
 * "trueCount": 2, "falseCount": 1,
 * // date
 * "minDate": "2024-01-01", "maxDate": "2024-12-31",
 * // string
 * "minLength": 0, "maxLength": 5,
 * // quality + outliers
 * "qualityScore": 0..100, "qualityGrade": "A".."F",
 * "outlierSummary": { "outlierCount": 0..n, "lowerFence": <double>,
 * "upperFence": <double> }
 * }
 * ]
 * }
 */
@SpringBootTest
@AutoConfigureMockMvc
class Part3Tests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataAnalysisRepository dataAnalysisRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        dataAnalysisRepository.deleteAll();
    }

    @Test
    void statsEndpointReturnsTypesAndBasicStats() throws Exception {
        // Header covers all supported inferred types:
        // - age: numeric
        // - is_active: boolean
        // - signup_date: date
        // - notes: string
        String csv = String.join("\n",
                "age,is_active,signup_date,notes",
                "18,true,2024-01-01,ok",
                "22,false,2024-03-05,hello",
                "40,true,2024-12-31," // empty notes
        );

        // Ingest
        mockMvc.perform(post("/api/analysis/ingestCsv")
                .contentType(TEXT_PLAIN)
                .content(csv))
                .andExpect(status().isOk());

        long id = dataAnalysisRepository.findAll().getFirst().getId();

        // Get stats
        String statsJson = mockMvc.perform(get("/api/analysis/{id}/stats", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(statsJson);
        JsonNode columns = root.path("columns");
        assertThat(columns.isArray()).as("columns array present").isTrue();
        assertThat(columns).hasSize(4);

        // Assertions by column
        JsonNode age = findColumn(columns, "age");
        assertThat(age).isNotNull();
        assertThat(text(age, "dataType")).isIn("INTEGER", "DECIMAL");
        assertThat(num(age, "min")).isNotNull();
        assertThat(num(age, "max")).isNotNull();
        assertThat(num(age, "mean")).isNotNull();
        assertThat(num(age, "median")).isNotNull();
        assertThat(num(age, "stddev")).isNotNull();

        JsonNode active = findColumn(columns, "is_active");
        assertThat(active).isNotNull();
        assertThat(text(active, "dataType")).isEqualTo("BOOLEAN");
        assertThat(num(active, "trueCount")).isNotNull();
        assertThat(num(active, "falseCount")).isNotNull();

        JsonNode date = findColumn(columns, "signup_date");
        assertThat(date).isNotNull();
        assertThat(text(date, "dataType")).isEqualTo("DATE");
        assertThat(text(date, "minDate")).isNotBlank();
        assertThat(text(date, "maxDate")).isNotBlank();

        JsonNode notes = findColumn(columns, "notes");
        assertThat(notes).isNotNull();
        assertThat(text(notes, "dataType")).isEqualTo("STRING");
        assertThat(num(notes, "minLength")).isNotNull();
        assertThat(num(notes, "maxLength")).isNotNull();
    }

    @Test
    void statsIncludeDataQualityScoreAndGrade() throws Exception {
        // Create a column with some nulls and one invalid to ensure the score isn't
        // trivially 100.
        String csv = String.join("\n",
                "score,comment",
                "10,ok",
                "20,good",
                ",blank-note", // null numeric
                "xyz,bad-number", // invalid numeric
                "30," // null string
        );

        mockMvc.perform(post("/api/analysis/ingestCsv")
                .contentType(TEXT_PLAIN)
                .content(csv))
                .andExpect(status().isOk());

        long id = dataAnalysisRepository.findAll().getFirst().getId();

        String statsJson = mockMvc.perform(get("/api/analysis/{id}/stats", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode columns = objectMapper.readTree(statsJson).path("columns");
        JsonNode score = findColumn(columns, "score");
        JsonNode comment = findColumn(columns, "comment");

        assertThat(score).isNotNull();
        assertQuality(score);

        assertThat(comment).isNotNull();
        assertQuality(comment);
    }

    @Test
    void numericIqrOutlierDetectionFlagsExtremeValues() throws Exception {
        // Strong outlier present (1000)
        String csv = String.join("\n",
                "value",
                "1",
                "2",
                "2",
                "3",
                "2",
                "1000");

        mockMvc.perform(post("/api/analysis/ingestCsv")
                .contentType(TEXT_PLAIN)
                .content(csv))
                .andExpect(status().isOk());

        long id = dataAnalysisRepository.findAll().getFirst().getId();

        String statsJson = mockMvc.perform(get("/api/analysis/{id}/stats", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode value = findColumn(objectMapper.readTree(statsJson).path("columns"), "value");
        assertThat(value).isNotNull();

        JsonNode outliers = value.path("outlierSummary");
        assertThat(outliers).as("outlierSummary present").isNotNull();
        assertThat(num(outliers, "lowerFence")).isNotNull();
        assertThat(num(outliers, "upperFence")).isNotNull();

        Double outlierCount = num(outliers, "outlierCount");
        assertThat(outlierCount).isNotNull();
        // Expect at least one outlier (the 1000)
        assertThat(outlierCount).isGreaterThanOrEqualTo(1.0);
    }

    // --------------------
    // Helpers
    // --------------------

    private static JsonNode findColumn(JsonNode columns, String name) {
        for (JsonNode n : columns) {
            if (name.equals(n.path("columnName").asText()))
                return n;
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private static Double num(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return (v.isMissingNode() || v.isNull()) ? null : v.asDouble();
    }

    private static void assertQuality(JsonNode column) {
        Double score = num(column, "qualityScore");
        String grade = text(column, "qualityGrade");
        assertThat(score).as("qualityScore present").isNotNull();
        assertThat(score).isBetween(0.0, 100.0);
        assertThat(grade).as("qualityGrade present").isNotBlank();
        assertThat(grade).matches("[ABCDF]");
    }
}
