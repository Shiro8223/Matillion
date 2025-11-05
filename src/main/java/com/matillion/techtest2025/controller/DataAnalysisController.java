package com.matillion.techtest2025.controller;

import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
import com.matillion.techtest2025.exception.BadRequestException;
import com.matillion.techtest2025.service.DataAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * REST controller for data analysis endpoints.
 * <p>
 * Handles HTTP requests and delegates business logic to {@link DataAnalysisService}.
 * All endpoints are prefixed with {@code /api/analysis}.
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class DataAnalysisController {

    private final DataAnalysisService dataAnalysisService;

    // Part 1 endpoints

    /**
     * Ingests and analyzes CSV data.
     * <p>
     * Validates the input data (rejects data containing "Sonny Hayes"), performs analysis,
     * persists the results to the database, and returns statistics about the CSV.
     *
     * @param data the raw CSV data as a string
     * @return analysis results including row count, column count, total characters, and column statistics
     * @throws BadRequestException if validation fails
     */
    @PostMapping("/ingestCsv")
    public DataAnalysisResponse ingestAndAnalyzeCsv(@RequestBody String data) {
        // Simple validation: reject data containing "Sonny Hayes"
        // (fictional F1 driver from the recent F1 movie)
        if (data.contains("Sonny Hayes")) {
            throw new BadRequestException("CSV data containing 'Sonny Hayes' is not allowed");
        }

        return dataAnalysisService.analyzeCsvData(data);
    }

    // Part 2 endpoints

    /**
     * Retrieves a previously analyzed CSV by its ID.
     * <p>
     * <b>Part 2:</b> This endpoint allows retrieving analysis results that were
     * previously persisted to the database via the POST /api/analysis/ingestCsv endpoint.
     *
     * @param id the ID of the analysis to retrieve
     * @return analysis results including row count, column count, total characters, and column statistics
     * @throws com.matillion.techtest2025.exception.NotFoundException if no analysis exists with the given ID (returns HTTP 404)
     */
    @GetMapping("/{id}")
    public DataAnalysisResponse getAnalysisById(@PathVariable Long id) {
        // To be implemented in part 2
        return null;
    }

    /**
     * Deletes an analysis by its ID.
     * <p>
     * <b>Part 2:</b> This endpoint removes an analysis and all its associated
     * column statistics from the database. The cascade configuration ensures that
     * all related data is properly cleaned up.
     *
     * @param id the ID of the analysis to delete
     * @throws com.matillion.techtest2025.exception.NotFoundException if no analysis exists with the given ID (returns HTTP 404)
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteAnalysisById(@PathVariable Long id) {
        // To be implemented in part 2
    }
}
