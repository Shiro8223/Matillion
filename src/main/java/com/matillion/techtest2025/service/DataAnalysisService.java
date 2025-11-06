package com.matillion.techtest2025.service;

import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
// Added the request exception for handling invalid CSVs
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

// Part 2 imports for unique counting
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

// Part 3 imports for stats endpoint
import com.matillion.techtest2025.controller.response.StatsResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import java.util.Comparator;

/**
 * Service layer containing the core business logic for CSV analysis.
 *
 * Responsibilities:
 * - Validates and parses CSV input
 * - Computes per-column statistics (nulls, uniques, etc.)
 * - Persists results to the H2 in-memory database
 * - Provides retrieval and deletion operations
 * - (Part 3) Exposes type inference and descriptive statistics for /stats
 */
@Service
@RequiredArgsConstructor
public class DataAnalysisService {

        private final DataAnalysisRepository dataAnalysisRepository;
        private final ColumnStatisticsRepository columnStatisticsRepository;

        // ---------------------------------------------------------------------
        // PART 1 + PART 2: CSV ingestion and persistence
        // ---------------------------------------------------------------------

        /**
         * Analyzes raw CSV text and saves the computed statistics.
         *
         * Part 1: Parse and validate the CSV, count rows/columns/nulls,
         * persist the data, and return a DataAnalysisResponse.
         *
         * Part 2: Extend logic to compute unique non-null values per column.
         */
        public DataAnalysisResponse analyzeCsvData(String data) {
                // -----------------------------------------------------------------
                // 1) Input validation
                // -----------------------------------------------------------------
                if (data == null || data.trim().isEmpty()) {
                        throw new BadRequestException("Empty input");
                }

                // Split into lines, supporting both \n and \r\n endings
                String[] lines = data.split("\\r?\\n");
                if (lines.length == 0) {
                        throw new BadRequestException("No CSV content found");
                }

                // -----------------------------------------------------------------
                // 2) Parse and validate header row
                // -----------------------------------------------------------------
                String[] headers = lines[0].split(",");
                for (int i = 0; i < headers.length; i++) {
                        headers[i] = headers[i].trim();
                        if (headers[i].isEmpty()) {
                                throw new BadRequestException("Invalid header: column name missing");
                        }
                }
                int numberOfColumns = headers.length;

                // -----------------------------------------------------------------
                // 3) Parse data rows and compute null + unique counts
                // -----------------------------------------------------------------
                int numberOfRows = lines.length - 1;
                int[] nullCounts = new int[numberOfColumns]; // per-column null tracking

                // One HashSet per column to store distinct non-null values
                List<Set<String>> uniqueSets = new ArrayList<>(numberOfColumns);
                for (int c = 0; c < numberOfColumns; c++) {
                        uniqueSets.add(new HashSet<>());
                }

                for (int r = 1; r < lines.length; r++) {
                        String[] cells = lines[r].split(",", -1);
                        if (cells.length != numberOfColumns) {
                                throw new BadRequestException("Malformed CSV: inconsistent column counts");
                        }

                        // Count nulls and collect unique non-nulls
                        for (int c = 0; c < numberOfColumns; c++) {
                                String trimmed = cells[c].trim();
                                if (trimmed.isEmpty()) {
                                        nullCounts[c]++;
                                } else {
                                        uniqueSets.get(c).add(trimmed);
                                }
                        }
                }

                // -----------------------------------------------------------------
                // 4) Totals and timestamps
                // -----------------------------------------------------------------
                long totalCharacters = data.length();
                OffsetDateTime createdAt = OffsetDateTime.now();

                // -----------------------------------------------------------------
                // 5) Persist parent entity (DataAnalysisEntity) — CAPTURE SAVED INSTANCE
                // -----------------------------------------------------------------
                DataAnalysisEntity parent = DataAnalysisEntity.builder()
                                .originalData(data)
                                .numberOfRows(numberOfRows)
                                .numberOfColumns(numberOfColumns)
                                .totalCharacters(totalCharacters)
                                .createdAt(createdAt)
                                .build();

                DataAnalysisEntity saved = dataAnalysisRepository.save(parent);

                // -----------------------------------------------------------------
                // 6) Build & persist child entities (ColumnStatisticsEntity)
                // -----------------------------------------------------------------
                List<ColumnStatisticsEntity> children = new ArrayList<>();

                for (int i = 0; i < numberOfColumns; i++) {
                        ColumnStatisticsEntity stat = ColumnStatisticsEntity.builder()
                                        .dataAnalysis(saved) // link to parent
                                        .columnName(headers[i])
                                        .nullCount(nullCounts[i])
                                        .uniqueCount(uniqueSets.get(i).size()) // Part 2
                                        .build();
                        children.add(stat);
                }

                columnStatisticsRepository.saveAll(children);
                saved.setColumnStatistics(children); // maintain in-memory graph

                // -----------------------------------------------------------------
                // 7) Return API response via mapper (id first, matches record order)
                // -----------------------------------------------------------------
                return mapToResponse(saved);
        }

        // ---------------------------------------------------------------------
        // Helper: entity -> DTO mapper (keeps mapping consistent across endpoints)
        // ---------------------------------------------------------------------
        private DataAnalysisResponse mapToResponse(DataAnalysisEntity e) {
                List<ColumnStatistics> statsDto = e.getColumnStatistics().stream()
                                .map(cs -> new ColumnStatistics(
                                                cs.getColumnName(),
                                                cs.getNullCount(),
                                                cs.getUniqueCount()))
                                .toList();

                return new DataAnalysisResponse(
                                e.getId(),
                                e.getNumberOfRows(),
                                e.getNumberOfColumns(),
                                e.getTotalCharacters(),
                                statsDto,
                                e.getCreatedAt());
        }

        // ---------------------------------------------------------------------
        // Part 2: retrieval and deletion endpoints
        // ---------------------------------------------------------------------

        /** Load an existing analysis by ID (used for GET /api/analysis/{id}). */
        public DataAnalysisResponse getAnalysisById(long id) {
                var entityOpt = dataAnalysisRepository.findById(id);
                if (entityOpt.isEmpty()) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.NOT_FOUND, "Analysis not found");
                }
                return mapToResponse(entityOpt.get());
        }

        /**
         * Delete an analysis and its column statistics (used for DELETE
         * /api/analysis/{id}).
         */
        public void deleteAnalysis(long id) {
                if (!dataAnalysisRepository.existsById(id)) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.NOT_FOUND, "Analysis not found");
                }
                dataAnalysisRepository.deleteById(id);
        }

        // ---------------------------------------------------------------------
        // PART 3: new stats endpoint
        // ---------------------------------------------------------------------

        /**
         * Retrieves enhanced statistics for an existing analysis (Part 3).
         *
         * Currently only verifies that the record exists and returns an empty
         * {@link StatsResponse}. The next steps will fill this with:
         * - Column type inference (INTEGER, DECIMAL, BOOLEAN, DATE, STRING)
         * - Descriptive statistics (min, max, mean, etc.)
         * - Data quality score and outlier detection
         *
         * @param id analysis identifier
         * @return empty placeholder response until Part 3 implementation is complete
         */
        public StatsResponse getStats(long id) {
                // 1) Load persisted analysis
                DataAnalysisEntity entity = dataAnalysisRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Analysis " + id + " not found"));

                String csv = entity.getOriginalData();
                if (csv == null || csv.isBlank()) {
                        return new StatsResponse(entity.getId(), List.of());
                }

                // 2) Parse CSV lines + header
                String[] lines = csv.split("\\r?\\n", -1);
                if (lines.length == 0) {
                        return new StatsResponse(entity.getId(), List.of());
                }
                String[] headers = splitCsvLine(lines[0]);
                int cols = headers.length;

                // 3) Build per-column cell arrays
                ArrayList<ArrayList<String>> columnCells = new ArrayList<>(cols);
                for (int c = 0; c < cols; c++)
                        columnCells.add(new ArrayList<>());

                for (int r = 1; r < lines.length; r++) {
                        if (lines[r].isEmpty())
                                continue;
                        String[] cells = splitCsvLine(lines[r]);
                        if (cells.length != cols)
                                continue; // malformed rows were rejected at ingest
                        for (int c = 0; c < cols; c++)
                                columnCells.get(c).add(cells[c]);
                }

                // 4) Index persisted per-column metrics by name (null/unique)
                Map<String, ColumnStatisticsEntity> byName = new HashMap<>();
                if (entity.getColumnStatistics() != null) {
                        for (ColumnStatisticsEntity e : entity.getColumnStatistics()) {
                                byName.put(e.getColumnName(), e);
                        }
                }

                // 5) For each column: infer type, compute stats, quality, outliers
                ArrayList<StatsResponse.ColumnStatsView> views = new ArrayList<>(cols);

                for (int c = 0; c < cols; c++) {
                        String colName = t(headers[c]);
                        ArrayList<String> raw = columnCells.get(c);

                        // Persisted counts (from Parts 1–2)
                        ColumnStatisticsEntity base = byName.get(colName);
                        long nullCount = base != null ? base.getNullCount() : 0L;
                        long uniqueCount = base != null ? base.getUniqueCount() : 0L;

                        // Split into null vs non-null (trimmed)
                        ArrayList<String> nonNull = new ArrayList<>(raw.size());
                        for (String v : raw) {
                                if (t(v).isEmpty())
                                        continue;
                                nonNull.add(t(v));
                        }

                        // ---- Type inference pass (deterministic) ----
                        boolean seenString = false, seenDecimal = false, seenInteger = false, seenBoolean = false,
                                        seenDate = false;
                        for (String v : nonNull) {
                                if (parseBoolean(v) != null) {
                                        seenBoolean = true;
                                        continue;
                                }
                                if (INT.matcher(v).matches()) {
                                        seenInteger = true;
                                        continue;
                                }
                                if (DEC.matcher(v).matches()) {
                                        seenDecimal = true;
                                        continue;
                                }
                                if (parseDate(v) != null) {
                                        seenDate = true;
                                        continue;
                                }
                                seenString = true;
                        }

                        String dataType;
                        if (seenString)
                                dataType = "STRING";
                        else if (seenDecimal || (seenInteger && seenDecimal))
                                dataType = "DECIMAL";
                        else if (seenInteger)
                                dataType = "INTEGER";
                        else if (seenBoolean)
                                dataType = "BOOLEAN";
                        else if (seenDate)
                                dataType = "DATE";
                        else
                                dataType = "STRING";

                        // ---- Type-specific stats + invalid counting ----
                        Long trueCount = null, falseCount = null;
                        Double min = null, max = null, mean = null, median = null, stddev = null;
                        String minDate = null, maxDate = null;
                        Integer minLen = null, maxLen = null;
                        Double lowerFence = null, upperFence = null;
                        Integer outlierCount = null;

                        long invalid = 0;

                        switch (dataType) {
                                case "BOOLEAN" -> {
                                        long tCount = 0, fCount = 0;
                                        for (String v : nonNull) {
                                                Boolean b = parseBoolean(v);
                                                if (b == null) {
                                                        invalid++;
                                                        continue;
                                                }
                                                if (b)
                                                        tCount++;
                                                else
                                                        fCount++;
                                        }
                                        trueCount = tCount;
                                        falseCount = fCount;
                                }
                                case "INTEGER", "DECIMAL" -> {
                                        ArrayList<Double> vals = new ArrayList<>(nonNull.size());
                                        for (String v : nonNull) {
                                                Double d = parseNumber(v);
                                                if (d == null) {
                                                        invalid++;
                                                        continue;
                                                }
                                                vals.add(d);
                                        }
                                        if (!vals.isEmpty()) {
                                                vals.sort(Comparator.naturalOrder());
                                                min = vals.getFirst();
                                                max = vals.getLast();
                                                mean = vals.stream().mapToDouble(Double::doubleValue).average()
                                                                .orElse(Double.NaN);
                                                median = medianOf(vals);
                                                stddev = stddevOf(vals, mean);

                                                // IQR outliers
                                                double[] fences = iqrFences(vals);
                                                lowerFence = fences[0];
                                                upperFence = fences[1];
                                                int oc = 0;
                                                for (double d : vals)
                                                        if (d < lowerFence || d > upperFence)
                                                                oc++;
                                                outlierCount = oc;
                                        } else {
                                                // No numeric values but inferred numeric = everything was invalid
                                                invalid = nonNull.size();
                                        }
                                }
                                case "DATE" -> {
                                        ArrayList<java.time.LocalDate> dates = new ArrayList<>(nonNull.size());
                                        for (String v : nonNull) {
                                                java.time.LocalDate d = parseDate(v);
                                                if (d == null) {
                                                        invalid++;
                                                        continue;
                                                }
                                                dates.add(d);
                                        }
                                        if (!dates.isEmpty()) {
                                                dates.sort(Comparator.naturalOrder());
                                                minDate = dates.getFirst().toString();
                                                maxDate = dates.getLast().toString();
                                        }
                                }
                                default -> { // STRING
                                        for (String v : nonNull) {
                                                int len = v.length();
                                                minLen = (minLen == null) ? len : Math.min(minLen, len);
                                                maxLen = (maxLen == null) ? len : Math.max(maxLen, len);
                                        }
                                }
                        }

                        // ---- Data Quality Score (0–100) and grade ----
                        int qScore = qualityScore(nonNull.size(), nullCount, invalid, uniqueCount(nonNull));
                        String qGrade = qualityGrade(qScore);

                        // ---- Assemble view ----
                        views.add(new StatsResponse.ColumnStatsView(
                                        colName,
                                        dataType,
                                        nullCount,
                                        uniqueCount,
                                        // numeric
                                        min, max, mean, median, stddev,
                                        // boolean
                                        trueCount, falseCount,
                                        // date
                                        minDate, maxDate,
                                        // string
                                        minLen, maxLen,
                                        // quality + outliers
                                        qScore, qGrade,
                                        (lowerFence == null && upperFence == null && outlierCount == null)
                                                        ? null
                                                        : new com.matillion.techtest2025.controller.response.OutlierSummary(
                                                                        lowerFence, upperFence, outlierCount)));
                }

                return new StatsResponse(entity.getId(), views);
        }

        /** CSV split that preserves empty cells. */
        private static String[] splitCsvLine(String line) {
                return line.split(",", -1);
        }

        // ---------- Part 3 helpers: parsing & stats ----------

        private static final java.util.regex.Pattern INT = java.util.regex.Pattern.compile("^[+-]?\\d+$");
        private static final java.util.regex.Pattern DEC = java.util.regex.Pattern.compile("^[+-]?\\d*\\.\\d+$");

        /** Trim null-safe. */
        private static String t(String s) {
                return s == null ? "" : s.trim();
        }

        /**
         * Parse boolean variants; return null if not a known boolean representation.
         */
        private static Boolean parseBoolean(String v) {
                String s = t(v).toLowerCase(java.util.Locale.ROOT);
                return switch (s) {
                        case "true", "1", "yes", "y" -> true;
                        case "false", "0", "no", "n" -> false;
                        default -> null;
                };
        }

        /**
         * Parse number (integer or decimal) using simple regexes; null if not numeric.
         */
        private static Double parseNumber(String v) {
                String s = t(v);
                if (INT.matcher(s).matches())
                        return Double.valueOf(s);
                if (DEC.matcher(s).matches())
                        return Double.valueOf(s);
                return null;
        }

        /** Parse ISO-8601 date (yyyy-MM-dd); null if not parsable. */
        private static java.time.LocalDate parseDate(String v) {
                try {
                        return java.time.LocalDate.parse(t(v));
                } catch (java.time.format.DateTimeParseException e) {
                        return null;
                }
        }

        /** Median from a sorted list of doubles. */
        private static double medianOf(java.util.List<Double> sorted) {
                int n = sorted.size();
                if (n == 0)
                        return Double.NaN;
                if (n % 2 == 1)
                        return sorted.get(n / 2);
                return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        }

        /** Population standard deviation given mean. */
        private static double stddevOf(java.util.List<Double> vals, double mean) {
                if (vals.isEmpty())
                        return Double.NaN;
                double sumSq = 0.0;
                for (double d : vals) {
                        double diff = d - mean;
                        sumSq += diff * diff;
                }
                return Math.sqrt(sumSq / vals.size());
        }

        /** Linear-interpolated percentile from a sorted list. */
        private static double percentile(java.util.List<Double> sorted, double p) {
                if (sorted.isEmpty())
                        return Double.NaN;
                double rank = (p / 100.0) * (sorted.size() - 1);
                int lo = (int) Math.floor(rank);
                int hi = (int) Math.ceil(rank);
                if (lo == hi)
                        return sorted.get(lo);
                double w = rank - lo;
                return sorted.get(lo) * (1 - w) + sorted.get(hi) * w;
        }

        /** IQR fences (lower, upper) from a sorted list. */
        private static double[] iqrFences(java.util.List<Double> sorted) {
                double q1 = percentile(sorted, 25.0);
                double q3 = percentile(sorted, 75.0);
                double iqr = q3 - q1;
                return new double[] { q1 - 1.5 * iqr, q3 + 1.5 * iqr };
        }

        /** Count unique non-null trimmed values. */
        private static int uniqueCount(java.util.List<String> vals) {
                java.util.HashSet<String> s = new java.util.HashSet<>();
                for (String v : vals) {
                        String tv = t(v);
                        if (!tv.isEmpty())
                                s.add(tv);
                }
                return s.size();
        }

        /**
         * Data quality score (0–100) based on null rate, invalid rate, and cardinality.
         */
        private static int qualityScore(int nonNull, long nulls, long invalid, int uniques) {
                int total = nonNull + (int) nulls;
                if (total == 0)
                        return 100;

                double nullRate = (total == 0) ? 0 : (double) nulls / total;
                double invalidRate = (nonNull == 0) ? 0 : (double) invalid / nonNull;
                double uniqueRatio = (nonNull == 0) ? 0 : (double) uniques / nonNull;

                int nullPenalty = (int) Math.round(nullRate * 40.0);
                int invalidPenalty = (int) Math.round(invalidRate * 40.0);

                int cardinalityPenalty = 0;
                if (uniqueRatio < 0.02)
                        cardinalityPenalty = 10; // nearly constant column
                else if (uniqueRatio > 0.98)
                        cardinalityPenalty = 10; // nearly ID-like

                int score = 100 - nullPenalty - invalidPenalty - cardinalityPenalty;
                return Math.max(0, Math.min(100, score));
        }

        /** Letter grade from score. */
        private static String qualityGrade(int score) {
                if (score >= 90)
                        return "A";
                if (score >= 80)
                        return "B";
                if (score >= 70)
                        return "C";
                if (score >= 60)
                        return "D";
                return "F";
        }

}
