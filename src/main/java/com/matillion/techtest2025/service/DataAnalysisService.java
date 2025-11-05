package com.matillion.techtest2025.service;

import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
// Added the request exception 
import com.matillion.techtest2025.exception.BadRequestException;

import com.matillion.techtest2025.model.ColumnStatistics;
import com.matillion.techtest2025.repository.ColumnStatisticsRepository;
import com.matillion.techtest2025.repository.DataAnalysisRepository;
import com.matillion.techtest2025.repository.entity.ColumnStatisticsEntity;
import com.matillion.techtest2025.repository.entity.DataAnalysisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
// NEW (Part 2): imports for unique counting
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Service layer containing business logic for data analysis.
 * <p>
 * Responsible for parsing data, calculating statistics, and persisting results.
 */
@Service
@RequiredArgsConstructor
public class DataAnalysisService {

        private final DataAnalysisRepository dataAnalysisRepository;
        private final ColumnStatisticsRepository columnStatisticsRepository;

        /**
         * Analyzes CSV data and returns statistics.
         * <p>
         * Parses the CSV, calculates statistics (row count, column count, character
         * count,
         * null counts per column), persists the results to the database, and returns
         * the analysis.
         * <p>
         * <b>Part 1 Implementation:</b>
         * <ul>
         * <li>Validates input (rejects empty, malformed, or inconsistent CSV)</li>
         * <li>Parses header and data rows</li>
         * <li>Counts rows, columns, total characters</li>
         * <li>Computes per-column null counts</li>
         * <li>Persists parent and child entities to the H2 database</li>
         * <li>Returns a response DTO matching test expectations</li>
         * </ul>
         * <p>
         * <b>Part 2 Additions:</b>
         * <ul>
         * <li>Compute per-column <code>uniqueCount</code> of non-null, trimmed
         * values</li>
         * <li>Persist <code>uniqueCount</code> into ColumnStatisticsEntity and expose
         * in DTO</li>
         * </ul>
         *
         * @param data raw CSV data (rows separated by newlines, columns by commas)
         * @return analysis results
         * @throws BadRequestException if the CSV is invalid or empty
         */
        public DataAnalysisResponse analyzeCsvData(String data) {

                // ---------------------------------------------------------------------
                // 1) Validate input
                // ---------------------------------------------------------------------
                if (data == null || data.trim().isEmpty()) {
                        throw new BadRequestException("Empty input");
                }

                // Split into lines (support both Windows and Unix newlines)
                String[] lines = data.split("\\r?\\n");
                if (lines.length == 0) {
                        throw new BadRequestException("No CSV content found");
                }

                // ---------------------------------------------------------------------
                // 2) Parse and validate the header row
                // ---------------------------------------------------------------------
                String[] headers = lines[0].split(",");
                for (int i = 0; i < headers.length; i++) {
                        headers[i] = headers[i].trim();
                        if (headers[i].isEmpty()) {
                                throw new BadRequestException("Invalid header: column name missing");
                        }
                }
                int numberOfColumns = headers.length;

                // ---------------------------------------------------------------------
                // 3) Parse data rows and check shape
                // ---------------------------------------------------------------------
                int numberOfRows = lines.length - 1;
                int[] nullCounts = new int[numberOfColumns]; // track null/empty cells per column

                // NEW (Part 2): track distinct non-null values per column
                // One HashSet per column; duplicates won't increase size
                List<Set<String>> uniqueSets = new ArrayList<>(numberOfColumns);
                for (int c = 0; c < numberOfColumns; c++) {
                        uniqueSets.add(new HashSet<>());
                }

                for (int r = 1; r < lines.length; r++) {
                        // Split each row using comma delimiter, keeping empty cells
                        String[] cells = lines[r].split(",", -1);
                        if (cells.length != numberOfColumns) {
                                // Each row must have the same number of columns as the header
                                throw new BadRequestException("Malformed CSV: inconsistent column counts");
                        }

                        // Count empty or whitespace-only cells as nulls
                        // NEW (Part 2): add non-null trimmed values to that column's unique set
                        for (int c = 0; c < numberOfColumns; c++) {
                                String trimmed = cells[c].trim();
                                if (trimmed.isEmpty()) {
                                        nullCounts[c]++;
                                } else {
                                        uniqueSets.get(c).add(trimmed); // Part 2: collect uniques
                                }
                        }
                }

                // ---------------------------------------------------------------------
                // 4) Calculate totals
                // ---------------------------------------------------------------------
                long totalCharacters = data.length(); // total character count
                OffsetDateTime createdAt = OffsetDateTime.now(); // timestamp for analysis

                // ---------------------------------------------------------------------
                // 5) Persist parent entity (DataAnalysisEntity)
                // ---------------------------------------------------------------------
                DataAnalysisEntity dataAnalysisEntity = DataAnalysisEntity.builder()
                                .originalData(data)
                                .numberOfRows(numberOfRows)
                                .numberOfColumns(numberOfColumns)
                                .totalCharacters(totalCharacters)
                                .createdAt(createdAt)
                                .build();

                dataAnalysisRepository.save(dataAnalysisEntity);

                // ---------------------------------------------------------------------
                // 6) Persist child entities (ColumnStatisticsEntity)
                // ---------------------------------------------------------------------
                List<ColumnStatisticsEntity> columnStatsEntities = new java.util.ArrayList<>();

                for (int i = 0; i < numberOfColumns; i++) {
                        ColumnStatisticsEntity stat = ColumnStatisticsEntity.builder()
                                        .dataAnalysis(dataAnalysisEntity)
                                        .columnName(headers[i])
                                        .nullCount(nullCounts[i])
                                        // NEW (Part 2): persist number of distinct, non-null values
                                        .uniqueCount(uniqueSets.get(i).size())
                                        .build();
                        columnStatsEntities.add(stat);
                }

                // Save all column statistics records
                columnStatisticsRepository.saveAll(columnStatsEntities);

                // Maintain bidirectional link (so parent knows its children)
                dataAnalysisEntity.setColumnStatistics(columnStatsEntities);

                // ---------------------------------------------------------------------
                // 7) Build and return response DTO
                // ---------------------------------------------------------------------
                // NOTE (Part 2): DTO now reflects the computed uniqueCount per column
                List<ColumnStatistics> statsDto = columnStatsEntities.stream()
                                .map(e -> new ColumnStatistics(
                                                e.getColumnName(),
                                                e.getNullCount(),
                                                e.getUniqueCount()))
                                .toList();

                return new DataAnalysisResponse(
                                numberOfRows,
                                numberOfColumns,
                                totalCharacters,
                                statsDto,
                                createdAt);
        }

        // ---------------------------------------------------------------------
        // NEW (Part 2): central mapper to keep response consistent
        // ---------------------------------------------------------------------
        /**
         * Maps a persisted entity (with children) to the response DTO used by the API.
         */
        private DataAnalysisResponse mapToResponse(DataAnalysisEntity e) {
                List<ColumnStatistics> statsDto = e.getColumnStatistics().stream()
                                .map(cs -> new ColumnStatistics(
                                                cs.getColumnName(),
                                                cs.getNullCount(),
                                                cs.getUniqueCount()))
                                .toList();

                return new DataAnalysisResponse(
                                e.getNumberOfRows(),
                                e.getNumberOfColumns(),
                                e.getTotalCharacters(),
                                statsDto,
                                e.getCreatedAt());
        }

        // ---------------------------------------------------------------------
        // NEW (Part 2): fetch previously-saved analysis by id
        // ---------------------------------------------------------------------
        /**
         * Retrieves a previously saved analysis by its id.
         * <p>
         * Loads the parent and its column statistics and maps them to the response DTO.
         *
         * @param id the analysis id
         * @return the mapped response DTO
         * @throws org.springframework.web.server.ResponseStatusException if not found
         *                                                                (404)
         */
        public DataAnalysisResponse getAnalysisById(long id) {
                var entityOpt = dataAnalysisRepository.findById(id);
                if (entityOpt.isEmpty()) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.NOT_FOUND, "Analysis not found");
                }
                DataAnalysisEntity e = entityOpt.get();
                // If columnStatistics is LAZY, accessing it here keeps things safe in service
                // e.getColumnStatistics().size();
                return mapToResponse(e);
        }

        // ---------------------------------------------------------------------
        // NEW (Part 2): delete an analysis by id
        // ---------------------------------------------------------------------
        /**
         * Deletes an analysis and its column statistics.
         * <p>
         * Assumes orphanRemoval/cascade is configured on the
         * DataAnalysisEntity->ColumnStatisticsEntity relationship.
         *
         * @param id the analysis id
         * @throws org.springframework.web.server.ResponseStatusException if not found
         *                                                                (404)
         */
        public void deleteAnalysis(long id) {
                // If the id doesn't exist, return 404 to match test expectations
                if (!dataAnalysisRepository.existsById(id)) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.NOT_FOUND, "Analysis not found");
                }
                // Deleting the parent should cascade to children due to orphanRemoval=true
                dataAnalysisRepository.deleteById(id);
        }

}
