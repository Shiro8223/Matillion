package com.matillion.techtest2025.service;

import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
//Added the request exception 
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

                for (int r = 1; r < lines.length; r++) {
                        // Split each row using comma delimiter, keeping empty cells
                        String[] cells = lines[r].split(",", -1);
                        if (cells.length != numberOfColumns) {
                                // Each row must have the same number of columns as the header
                                throw new BadRequestException("Malformed CSV: inconsistent column counts");
                        }

                        // Count empty or whitespace-only cells as nulls
                        for (int c = 0; c < numberOfColumns; c++) {
                                if (cells[c].trim().isEmpty()) {
                                        nullCounts[c]++;
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
                                        .uniqueCount(0) // unique count handled in Part 2
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
}
