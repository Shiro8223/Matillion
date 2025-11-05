package com.matillion.techtest2025.service;

import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
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
     * Parses the CSV, calculates statistics (row count, column count, character count,
     * null counts per column), persists the results to the database, and returns the analysis.
     * <p>
     * <b>Note:</b> Current implementation is incomplete. Part 1 of the tech test
     * requires implementing the CSV parsing and analysis logic.
     *
     * @param data raw CSV data (rows separated by newlines, columns by commas)
     * @return analysis results
     */
    public DataAnalysisResponse analyzeCsvData(String data) {
        // Analysis logic will be implemented here
        int numberOfRows = 0;
        int numberOfColumns = 0;
        long totalCharacters = 0L;

        // Create and persist the entity
        OffsetDateTime creationTimestamp = OffsetDateTime.now();

        DataAnalysisEntity dataAnalysisEntity = DataAnalysisEntity.builder()
                .originalData(data)
                .numberOfRows(numberOfRows)
                .numberOfColumns(numberOfColumns)
                .totalCharacters(totalCharacters)
                .createdAt(creationTimestamp)
                .build();

        dataAnalysisRepository.save(dataAnalysisEntity);

        // Create the per-column statistics
        List<ColumnStatisticsEntity> columnStatisticsEntities = List.of(ColumnStatisticsEntity.builder()
                .dataAnalysis(dataAnalysisEntity)
                .columnName("example")
                .nullCount(0)
                .uniqueCount(0)  // Part 2: Calculate unique non-null values per column
                .build());

        columnStatisticsRepository.saveAll(columnStatisticsEntities);

        // Return the analysis values
        return new DataAnalysisResponse(
                numberOfRows,
                numberOfColumns,
                totalCharacters,
                columnStatisticsEntities
                        .stream()
                        .map(e -> new ColumnStatistics(
                                e.getColumnName(),
                                e.getNullCount(),
                                e.getUniqueCount()
                        ))
                        .toList(),
                creationTimestamp
        );
    }

}
