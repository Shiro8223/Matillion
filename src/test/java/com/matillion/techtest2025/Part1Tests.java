package com.matillion.techtest2025;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
import com.matillion.techtest2025.repository.DataAnalysisRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Part1Tests {

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

    /**
     * Verifies that the Spring application context loads successfully.
     * This is a basic smoke test to ensure all beans are configured correctly.
     */
    @Test
    void contextLoads() {
    }

    /**
     * Tests basic CSV parsing with a simple 3x3 dataset containing no null values.
     * <p>
     * The CSV contains Formula 1 driver data:
     * - 3 rows of F1 drivers (Max Verstappen, Lewis Hamilton, Charles Leclerc)
     * - 3 columns: driver, number, team
     * - No null/empty values
     * <p>
     * Expected behavior:
     * - Parse the CSV correctly
     * - Count the number of rows and columns
     * - Calculate total character count
     * - Generate column statistics showing 0 null values for each column
     */
    @Test
    void shouldAnalyzeSimpleCsv(
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

        assertThat(response.numberOfRows()).isEqualTo(3);
        assertThat(response.numberOfColumns()).isEqualTo(3);
        assertThat(response.totalCharacters()).isEqualTo(csvData.length());
        assertThat(response.columnStatistics()).hasSize(3);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.nullCount() == 0);
    }

    /**
     * Tests CSV parsing with null/empty values scattered across different columns.
     * <p>
     * The CSV contains Formula 1 driver data:
     * - 4 rows of F1 drivers (Lando Norris, Fernando Alonso, George Russell, Oscar Piastri)
     * - 4 columns: driver, number, team, nationality
     * - Various null/empty values in different positions
     * <p>
     * Expected behavior:
     * - Correctly identify and count null/empty values per column
     * - 'driver' column: 0 nulls (all values present)
     * - 'number' column: 2 nulls (Alonso and Piastri missing)
     * - 'team' column: 2 nulls (Russell and Piastri missing)
     * - 'nationality' column: 2 nulls (Alonso and Piastri missing)
     * <p>
     * Note: An empty string between commas (e.g., "Oscar Piastri,,,") should be treated as null.
     */
    @Test
    void shouldCountNullValuesCorrectly(
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

        assertThat(response.numberOfRows()).isEqualTo(4);
        assertThat(response.numberOfColumns()).isEqualTo(4);
        assertThat(response.columnStatistics()).hasSize(4);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.nullCount() == 2)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.nullCount() == 2)
                .anyMatch(stat -> stat.columnName().equals("nationality") && stat.nullCount() == 0);
    }

    /**
     * Tests edge case: CSV with header row but no data rows.
     * <p>
     * The CSV contains:
     * - Only a header row: driver,number,team
     * - 0 data rows (no F1 drivers listed)
     * <p>
     * Expected behavior:
     * - numberOfRows should be 0
     * - numberOfColumns should be 3 (from the header)
     * - columnStatistics should still contain 3 entries with 0 null counts
     * - Should not throw an error (empty CSVs are valid)
     */
    @Test
    void shouldHandleEmptyCsvWithHeaderOnly(
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

        assertThat(response.numberOfRows()).isEqualTo(0);
        assertThat(response.numberOfColumns()).isEqualTo(3);
        assertThat(response.columnStatistics()).hasSize(3);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.nullCount() == 0);
    }

    /**
     * Tests edge case: CSV with only a single data row.
     * <p>
     * The CSV contains:
     * - 1 data row: Sergio Perez (plus header)
     * - 3 columns: driver, number, team
     * <p>
     * Expected behavior:
     * - numberOfRows should be 1
     * - Should correctly identify columns and generate statistics
     * - Verifies the parser works with minimal data
     */
    @Test
    void shouldHandleSingleRowCsv(
            @Value("classpath:test-data/single-row.csv")
            Resource singleRowCsv
    ) throws Exception {
        String csvData = singleRowCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.numberOfRows()).isEqualTo(1);
        assertThat(response.numberOfColumns()).isEqualTo(3);
        assertThat(response.columnStatistics()).hasSize(3);
    }

    /**
     * Tests parsing a larger CSV dataset with multiple rows and columns.
     * <p>
     * The CSV contains Formula 1 2024 season driver data:
     * - 10 rows of top F1 drivers (Max, Lewis, Charles, Lando, Carlos, George, Sergio, Fernando, Oscar, Pierre)
     * - 6 columns: driver, number, team, nationality, podiums, championships
     * - No null values
     * <p>
     * Expected behavior:
     * - Correctly count all 10 rows and 6 columns
     * - Calculate accurate character count
     * - Generate statistics for all 6 columns
     * - Verifies the parser scales beyond trivial datasets
     */
    @Test
    void shouldHandleLargeCsv(
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

        assertThat(response.numberOfRows()).isEqualTo(10);
        assertThat(response.numberOfColumns()).isEqualTo(6);
        assertThat(response.totalCharacters()).isEqualTo(csvData.length());
        assertThat(response.columnStatistics()).hasSize(6);
    }

    /**
     * Tests CSV parsing with a realistic dataset containing scattered null values.
     * <p>
     * The CSV contains F1 driver data with missing information:
     * - 6 rows of F1 drivers (Yuki, Lance, Valtteri, Zhou, Kevin, Nico)
     * - 5 columns: driver, team, number, nationality, podiums
     * - Null values distributed across different columns
     * <p>
     * Expected null counts per column:
     * - driver: 0 nulls
     * - team: 1 null (Zhou missing)
     * - number: 2 nulls (Lance and Nico missing)
     * - nationality: 1 null (Valtteri missing)
     * - podiums: 2 nulls (Lance and Kevin missing)
     * <p>
     * This tests the parser's ability to handle null values in various positions
     * within a more complex, real-world-like dataset.
     */
    @Test
    void shouldHandleMixedNullValues(
            @Value("classpath:test-data/mixed-nulls.csv")
            Resource mixedNullsCsv
    ) throws Exception {
        String csvData = mixedNullsCsv.getContentAsString(UTF_8);

        var result = mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk())
                .andReturn();

        DataAnalysisResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DataAnalysisResponse.class
        );

        assertThat(response.numberOfRows()).isEqualTo(6);
        assertThat(response.numberOfColumns()).isEqualTo(5);
        assertThat(response.columnStatistics()).hasSize(5);
        assertThat(response.columnStatistics())
                .anyMatch(stat -> stat.columnName().equals("driver") && stat.nullCount() == 0)
                .anyMatch(stat -> stat.columnName().equals("team") && stat.nullCount() == 1)
                .anyMatch(stat -> stat.columnName().equals("number") && stat.nullCount() == 3)
                .anyMatch(stat -> stat.columnName().equals("nationality") && stat.nullCount() == 1)
                .anyMatch(stat -> stat.columnName().equals("podiums") && stat.nullCount() == 0);
    }

    /**
     * Verifies that the analysis results are persisted to the H2 database.
     * <p>
     * Expected behavior:
     * - After a successful analysis, one record should exist in the database
     * - The DataAnalysisRepository should be used to save the entity
     * <p>
     * This test checks basic persistence functionality without examining
     * the details of what was saved.
     */
    @Test
    void shouldPersistDataToDatabase(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv
    ) throws Exception {
        String csvData = simpleCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isOk());

        assertThat(dataAnalysisRepository.count()).isEqualTo(1);
    }

    /**
     * Verifies that the persisted database entity contains correct analysis values.
     * <p>
     * Expected behavior:
     * - The DataAnalysisEntity should contain:
     * - Correct numberOfRows (3)
     * - Correct numberOfColumns (3)
     * - Correct totalCharacters (matching input CSV length)
     * - The original CSV data (originalData field)
     * - A non-null createdAt timestamp
     * <p>
     * This test validates that all analysis results are correctly stored
     * in the database for future retrieval and auditing.
     */
    @Test
    void shouldPersistCorrectAnalysisData(
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
        assertThat(entity.getNumberOfRows()).isEqualTo(3);
        assertThat(entity.getNumberOfColumns()).isEqualTo(3);
        assertThat(entity.getTotalCharacters()).isEqualTo(csvData.length());
        assertThat(entity.getOriginalData()).isEqualTo(csvData);
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    /**
     * Verifies that column statistics child entities are persisted correctly.
     * <p>
     * Expected behavior:
     * - The DataAnalysisEntity should have associated ColumnStatisticsEntity records
     * - There should be one ColumnStatisticsEntity per column (3 for simple.csv)
     * - Each ColumnStatisticsEntity should have:
     * - Correct columnName
     * - Correct nullCount
     * - A reference back to the parent DataAnalysisEntity
     * <p>
     * This test validates the parent-child relationship and ensures that
     * cascade persistence is working correctly.
     */
    @Test
    void shouldPersistColumnStatisticsEntities(
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
                .anyMatch(stat -> stat.getColumnName().equals("driver") && stat.getNullCount() == 0)
                .anyMatch(stat -> stat.getColumnName().equals("number") && stat.getNullCount() == 0)
                .anyMatch(stat -> stat.getColumnName().equals("team") && stat.getNullCount() == 0);

        // Verify bidirectional relationship
        entity.getColumnStatistics().forEach(stat ->
                assertThat(stat.getDataAnalysis()).isEqualTo(entity)
        );
    }

    /**
     * Tests that multiple CSV files can be ingested sequentially.
     * <p>
     * Expected behavior:
     * - Each ingestion should be independent
     * - Both datasets should be persisted to the database
     * - After two ingestions, the database should contain 2 records
     * <p>
     * This verifies that the service can handle multiple requests
     * and correctly maintains separate records for each analysis.
     */
    @Test
    void shouldHandleMultipleIngestRequests(
            @Value("classpath:test-data/simple.csv")
            Resource simpleCsv,
            @Value("classpath:test-data/with-nulls.csv")
            Resource withNullsCsv
    ) throws Exception {
        String csvData1 = simpleCsv.getContentAsString(UTF_8);
        String csvData2 = withNullsCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData2))
                .andExpect(status().isOk());

        assertThat(dataAnalysisRepository.count()).isEqualTo(2);
    }

    /**
     * Tests error handling for malformed CSV input.
     * <p>
     * The test CSV has F1 driver data with inconsistent column counts:
     * - Header: driver,number,team (3 columns)
     * - Row 1: Only 2 values (missing team)
     * - Row 2: 4 values (extra column)
     * <p>
     * Expected behavior:
     * - Return HTTP 400 Bad Request
     * - Should not persist anything to the database
     * - Should not throw an unhandled exception
     * <p>
     * Consider implementing proper error handling and validation
     * to catch malformed CSV data before processing.
     */
    @Test
    void shouldReturnBadRequestForInvalidCsv(
            @Value("classpath:test-data/invalid.csv")
            Resource invalidCsv
    ) throws Exception {
        String csvData = invalidCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests content validation: rejecting CSV data containing "Sonny Hayes".
     * <p>
     * Background: Sonny Hayes is a fictional Formula 1 driver from the recent F1 movie.
     * For this exercise, any CSV containing "Sonny Hayes" should be rejected.
     * <p>
     * Expected behavior:
     * - Return HTTP 400 Bad Request
     * - Should not persist anything to the database
     * - Should throw a BadRequestException with an appropriate message
     * <p>
     * This test demonstrates how to use the custom BadRequestException
     * to handle business logic validation failures. The exception is caught
     * by the GlobalExceptionHandler and converted to a proper HTTP 400 response
     * following the RFC 7807 Problem Details standard.
     * <p>
     * Implementation hint: Add validation logic in the DataAnalysisController
     * to check if the input data contains "Sonny Hayes" and throw a
     * BadRequestException if found.
     */
    @Test
    void shouldRejectCsvContainingSonnyHayes(
            @Value("classpath:test-data/sonny-hayes.csv")
            Resource sonnyHayesCsv
    ) throws Exception {
        String csvData = sonnyHayesCsv.getContentAsString(UTF_8);

        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(csvData))
                .andExpect(status().isBadRequest());

        // Verify nothing was persisted
        assertThat(dataAnalysisRepository.count()).isEqualTo(0);
    }

    /**
     * Tests error handling for completely empty input.
     * <p>
     * Expected behavior:
     * - Return HTTP 400 Bad Request
     * - Empty strings should be rejected (no header, no data)
     * - Should not attempt to parse or persist anything
     * <p>
     * This is different from a CSV with only a header (which is valid).
     * Empty input means literally nothing - not even a header row.
     */
    @Test
    void shouldReturnBadRequestForEmptyInput() throws Exception {
        mockMvc.perform(post("/api/analysis/ingestCsv")
                        .contentType(TEXT_PLAIN)
                        .content(""))
                .andExpect(status().isBadRequest());
    }
}
