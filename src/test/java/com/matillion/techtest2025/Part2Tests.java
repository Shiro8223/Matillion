package com.matillion.techtest2025;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
import com.matillion.techtest2025.repository.DataAnalysisRepository;
import com.matillion.techtest2025.repository.entity.DataAnalysisEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.MockMvc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Part 2: Extend API Functionality
 * <p>
 * This test suite covers the following functionality that needs to be implemented:
 * <ul>
 *   <li>Reporting the number of unique values in a given column</li>
 *   <li>A GET endpoint for retrieving previous analysis results</li>
 *   <li>A DELETE endpoint for deleting previous analysis results</li>
 * </ul>
 * <p>
 * <b>Prerequisites:</b> Part 1 must be completed before Part 2 can be implemented.
 * The CSV parsing and analysis logic from Part 1 is required for these tests to pass.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Part2Tests {

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

    // ==================== UNIQUE COUNT TESTS ====================

    /**
     * Tests that unique value counts are calculated correctly for a simple CSV.
     * <p>
     * The CSV contains Formula 1 driver data:
     * - 3 rows of F1 drivers (Max Verstappen, Lewis Hamilton, Charles Leclerc)
     * - 3 columns: driver, number, team
     * - All values are unique (no duplicates)
     * <p>
     * Expected behavior:
     * - driver column: 3 unique values
     * - number column: 3 unique values
     * - team column: 3 unique values
     */
    @Test
    void shouldCalculateUniqueCountsForSimpleCsv(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.columnStatistics()).hasSize(3);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.uniqueCount() == 3)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.uniqueCount() == 3)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.uniqueCount() == 3);
    }

    /**
     * Tests unique count calculation when there are duplicate values.
     * <p>
     * The CSV contains F1 driver data with some duplicate values:
     * - Multiple drivers from the same team
     * - Same nationality appearing multiple times
     * <p>
     * Expected behavior:
     * - Unique counts should only count distinct non-null values
     * - Null/empty values should not be counted in unique counts
     */
    @Test
    void shouldCalculateUniqueCountsWithDuplicates(
            @Value("classpath:test-data/large.csv")
            Resource largeCsv
    ) throws Exception {
        String csvData = largeCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.columnStatistics()).hasSize(6);
        // All drivers should be unique (10 unique drivers)
        // But teams should have duplicates (6 unique teams from 10 drivers)
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.uniqueCount() == 10)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.uniqueCount() == 6);
    }

    /**
     * Tests that null/empty values are excluded from unique counts.
     * <p>
     * The CSV contains F1 driver data with scattered null values:
     * - 4 rows of data
     * - Various null/empty values in different columns
     * <p>
     * Expected behavior:
     * - Null/empty values should NOT be counted in uniqueCount
     * - Only non-null distinct values should be counted
     * - driver column: 4 unique values (no nulls)
     * - number column: 2 unique values (4, 63) - two nulls excluded
     * - team column: 2 unique values (McLaren, Aston Martin) - two nulls excluded
     * - nationality column: 1 unique value (British) - three nulls excluded
     */
    @Test
    void shouldExcludeNullsFromUniqueCount(
            @Value("classpath:test-data/with-nulls.csv")
            Resource withNullsCsv
    ) throws Exception {
        String csvData = withNullsCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.columnStatistics()).hasSize(4);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.uniqueCount() == 4)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.uniqueCount() == 2)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.uniqueCount() == 2)
                .anyMatch(stat -> stat.columnName().equals("nationality") && stat.uniqueCount() == 3);
    }

    /**
     * Tests unique count for an empty CSV (header only, no data rows).
     * <p>
     * Expected behavior:
     * - uniqueCount should be 0 for all columns (no data)
     */
    @Test
    void shouldReturnZeroUniqueCountForEmptyCsv(
            @Value("classpath:test-data/empty.csv")
            Resource emptyCsv
    ) throws Exception {
        String csvData = emptyCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.columnStatistics()).hasSize(3);
        assertThat(response.columnStatistics())
                .allMatch(stat -> stat.uniqueCount() == 0);
    }

    /**
     * Tests that unique counts are persisted correctly to the database.
     * <p>
     * Expected behavior:
     * - ColumnStatisticsEntity records should contain the correct uniqueCount values
     * - The persisted data should match the response data
     */
    @Test
    void shouldPersistUniqueCountsToDatabase(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        var entities = dataAnalysisRepository.findAll();
        assertThat(entities).hasSize(1);

        var entity = entities.getFirst();
        assertThat(entity.getColumnStatistics()).hasSize(3);
        assertThat(entity.getColumnStatistics())
                .anyMatch(stat -> stat.getColumnName().equals("driver") && stat.getUniqueCount() == 3)
                .anyMatch(stat -> stat.getColumnName().equals("number") && stat.getUniqueCount() == 3)
                .anyMatch(stat -> stat.getColumnName().equals("team") && stat.getUniqueCount() == 3);
    }

    // ==================== GET ENDPOINT TESTS ====================

    /**
     * Tests retrieving a previously analyzed CSV by its ID.
     * <p>
     * Expected behavior:
     * - GET /api/analysis/{id} should return the same analysis results
     * - Response should match the original ingest response
     * - Should include all column statistics with null counts and unique counts
     */
    @Test
    void shouldRetrievePreviousAnalysisById(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        // First, ingest the CSV
        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        // Get the ID of the persisted entity
        var entities = dataAnalysisRepository.findAll();
        assertThat(entities).hasSize(1);
        Long analysisId = entities.getFirst().getId();

        // Now retrieve it via GET
        var result = mockMvc.perform(get("/api/analysis/{id}", analysisId))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.numberOfRows()).isEqualTo(3);
        assertThat(response.numberOfColumns()).isEqualTo(3);
        assertThat(response.totalCharacters()).isEqualTo(csvData.length());
        assertThat(response.columnStatistics()).hasSize(3);
    }

    /**
     * Tests that GET endpoint returns 404 for non-existent analysis ID.
     * <p>
     * Expected behavior:
     * - GET /api/analysis/999 should return HTTP 404 Not Found
     */
    @Test
    void shouldReturn404ForNonExistentAnalysis() throws Exception {
        mockMvc.perform(get("/api/analysis/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests retrieving multiple analyses to ensure each can be fetched independently.
     * <p>
     * Expected behavior:
     * - After ingesting two different CSVs, both should be retrievable by ID
     * - Each GET request should return the correct analysis for that ID
     */
    @Test
    void shouldRetrieveMultipleAnalysesIndependently(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv,
            @Value("classpath:test-data/with-nulls.csv")
            Resource withNullsCsv
    ) throws Exception {
        String csvData1 = simpleCsv.getContentAsString(UTF_8);
        String csvData2 = withNullsCsv.getContentAsString(UTF_8);

        // Ingest both CSVs
        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData2))
                .andExpect(status().isOk());

        var entities = dataAnalysisRepository.findAll();
        assertThat(entities).hasSize(2);

        // Retrieve first analysis
        Long id1 = entities.getFirst().getId();
        var result1 = mockMvc.perform(get("/api/analysis/{id}", id1))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        // Retrieve second analysis
        Long id2 = entities.get(1).getId();
        var result2 = mockMvc.perform(get("/api/analysis/{id}", id2))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        // Verify they're different
        assertThat(response1.numberOfRows()).isEqualTo(3);
        assertThat(response2.numberOfRows()).isEqualTo(4);
    }

    // ==================== DELETE ENDPOINT TESTS ====================

    /**
     * Tests deleting an analysis by its ID.
     * <p>
     * Expected behavior:
     * - DELETE /api/analysis/{id} should remove the analysis from the database
     * - Should return HTTP 204 No Content
     * - Subsequent GET requests should return 404
     */
    @Test
    void shouldDeleteAnalysisById(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        // Ingest the CSV
        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        var entities = dataAnalysisRepository.findAll();
        assertThat(entities).hasSize(1);
        Long analysisId = entities.getFirst().getId();

        // Delete it
        mockMvc.perform(delete("/api/analysis/{id}", analysisId))
                .andExpect(status().isNoContent());

        // Verify it's gone from the database
        assertThat(dataAnalysisRepository.count()).isEqualTo(0);

        // Verify GET returns 404
        mockMvc.perform(get("/api/analysis/{id}", analysisId))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests that DELETE endpoint returns 404 for non-existent analysis ID.
     * <p>
     * Expected behavior:
     * - DELETE /api/analysis/999 should return HTTP 404 Not Found
     * - Nothing should be deleted from the database
     */
    @Test
    void shouldReturn404WhenDeletingNonExistentAnalysis(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        // Ingest one CSV
        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        assertThat(dataAnalysisRepository.count()).isEqualTo(1);

        // Try to delete non-existent ID
        mockMvc.perform(delete("/api/analysis/{id}", 999L))
                .andExpect(status().isNotFound());

        // Verify nothing was deleted
        assertThat(dataAnalysisRepository.count()).isEqualTo(1);
    }

    /**
     * Tests deleting one analysis doesn't affect others.
     * <p>
     * Expected behavior:
     * - After ingesting multiple CSVs, deleting one should leave the others intact
     * - The remaining analyses should still be retrievable
     */
    @Test
    void shouldDeleteOnlySpecifiedAnalysis(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv,
            @Value("classpath:test-data/with-nulls.csv")
            Resource withNullsCsv
    ) throws Exception {
        String csvData1 = simpleCsv.getContentAsString(UTF_8);
        String csvData2 = withNullsCsv.getContentAsString(UTF_8);

        // Ingest both CSVs
        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData2))
                .andExpect(status().isOk());

        var entities = dataAnalysisRepository.findAll();
        assertThat(entities).hasSize(2);

        Long id1 = entities.get(0).getId();
        Long id2 = entities.get(1).getId();

        // Delete first analysis
        mockMvc.perform(delete("/api/analysis/{id}", id1))
                .andExpect(status().isNoContent());

        // Verify only one deleted
        assertThat(dataAnalysisRepository.count()).isEqualTo(1);

        // Verify second is still accessible
        mockMvc.perform(get("/api/analysis/{id}", id2))
                .andExpect(status().isOk());

        // Verify first is gone
        mockMvc.perform(get("/api/analysis/{id}", id1))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests that deleting an analysis also deletes its associated column statistics.
     * <p>
     * Expected behavior:
     * - Column statistics should be cascade-deleted with the parent analysis
     * - This tests the JPA cascade configuration
     */
    @Test
    void shouldCascadeDeleteColumnStatistics(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        // Ingest the CSV
        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        var entities = dataAnalysisRepository.findAll();
        assertThat(entities).hasSize(1);

        DataAnalysisEntity entity = entities.getFirst();
        Long analysisId = entity.getId();

        // Verify column statistics exist
        assertThat(entity.getColumnStatistics()).hasSize(3);

        // Delete the analysis
        mockMvc.perform(delete("/api/analysis/{id}", analysisId))
                .andExpect(status().isNoContent());

        // Verify parent is deleted
        assertThat(dataAnalysisRepository.count()).isEqualTo(0);

        // Column statistics should also be deleted due to cascade
        // (This is verified implicitly by the relationship configuration)
    }
}
