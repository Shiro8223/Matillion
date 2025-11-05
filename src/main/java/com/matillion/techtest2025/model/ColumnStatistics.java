package com.matillion.techtest2025.model;

/**
 * Model class representing statistical information about a single column in a CSV dataset.
 * <p>
 * This record is part of the {@link com.matillion.techtest2025.controller.response.DataAnalysisResponse}
 * and provides per-column analysis results. Like {@code DataAnalysisResponse}, this is a Java record
 * that provides immutability and automatic generation of constructors, getters, equals, hashCode, and toString.
 * <p>
 * <b>Example usage in JSON response:</b>
 * <pre>
 * {
 *   "columnName": "age",
 *   "nullCount": 5,
 *   "uniqueCount": 42
 * }
 * </pre>
 *
 * @param columnName  the name of the column (from the CSV header)
 * @param nullCount   the number of null/empty values in this column
 * @param uniqueCount the number of unique non-null values in this column (Part 2 requirement)
 */
public record ColumnStatistics(
        String columnName,
        int nullCount,
        int uniqueCount
) {
}
