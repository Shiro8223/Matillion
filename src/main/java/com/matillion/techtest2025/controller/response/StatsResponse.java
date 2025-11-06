package com.matillion.techtest2025.controller.response;

import java.util.List;

public record StatsResponse(
        long id,
        List<ColumnStatsView> columns) {
    public record ColumnStatsView(
            String columnName,
            String dataType,
            long nullCount,
            long uniqueCount,

            // numeric
            Double min, Double max, Double mean, Double median, Double stddev,

            // boolean
            Long trueCount, Long falseCount,

            // date
            String minDate, String maxDate,

            // string
            Integer minLength, Integer maxLength,

            // quality + outliers
            Integer qualityScore, String qualityGrade,
            OutlierSummary outlierSummary) {
    }
}
