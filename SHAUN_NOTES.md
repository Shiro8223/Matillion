# Matillion Java Tech Test Summary

## Part 1 Completion Summary

### **Implemented**

- **CSV Parsing**
  - Supports both `\n` and `\r\n` line endings.
  - Splits columns by comma and trims whitespace.
- **Validation**
  - Reject empty input (400).
  - Reject invalid header (empty column names) (400).
  - Reject inconsistent row lengths (400).
  - Controller already rejects `"Sonny Hayes"` (400).
- **Metrics**
  - `numberOfRows = lines - 1` (exclude header).
  - `numberOfColumns = header columns`.
  - `totalCharacters = data.length()`.
  - `nullCount` = `trim(cell).isEmpty()`.
- **Persistence**
  - `DataAnalysisEntity` saved with `originalData`, `numberOfRows`, `numberOfColumns`, `totalCharacters`, and `createdAt`.
  - `ColumnStatisticsEntity` created per column with `columnName`, `nullCount`, and `uniqueCount = 0`.
  - Bidirectional link maintained between parent and child entities.

### **Tests Passing (Part1Tests)**

- shouldAnalyzeSimpleCsv
- shouldCountNullValuesCorrectly
- shouldHandleEmptyCsvWithHeaderOnly
- shouldHandleSingleRowCsv
- shouldHandleLargeCsv
- shouldHandleMixedNullValues
- shouldPersistDataToDatabase
- shouldPersistCorrectAnalysisData
- shouldPersistColumnStatisticsEntities
- shouldHandleMultipleIngestRequests
- shouldReturnBadRequestForInvalidCsv
- shouldReturnBadRequestForEmptyInput
- shouldRejectCsvContainingSonnyHayes

### **Commands I Used**

```bash
# Run Part 1 only
./gradlew test --tests Part1Tests

# Run single Part 1 test
./gradlew test --tests Part1Tests.shouldAnalyzeSimpleCsv

# Run all tests
./gradlew test

# Run app
./gradlew bootRun
```

## Part 2 Completion Summary

### **Implemented**

- **Unique Value Tracking**
  - Added `List<Set<String>>` in `analyzeCsvData()` to track distinct, non-null, trimmed cell values for each column.
  - `uniqueCount = set.size()` persisted in `ColumnStatisticsEntity`.
  - Empty or whitespace-only cells excluded from uniques (counted as nulls only).
- **GET Endpoint (`/api/analysis/{id}`)**
  - Added `getAnalysisById(long id)` in `DataAnalysisService`.
  - Added `@GetMapping("/{id}")` in `DataAnalysisController`.
  - Returns 404 (`ResponseStatusException`) if ID not found.
  - Maps entity + child statistics to `DataAnalysisResponse` DTO.
- **DELETE Endpoint (`/api/analysis/{id}`)**
  - Added `deleteAnalysis(long id)` in service.
  - Added `@DeleteMapping("/{id}")` in controller.
  - Returns 204 No Content on success; 404 if not found.
  - Leverages `cascade = ALL` + `orphanRemoval = true` for automatic child deletion.
- **DTO Mapping Refactor**
  - Introduced `mapToResponse(DataAnalysisEntity e)` for consistent entity → DTO conversion.
  - Ensures deterministic column order and complete statistics returned.
- **Validation & Behaviour**
  - All Part 1 validations remain intact.
  - Header-only CSV → valid (uniques = 0).
  - Case-sensitive unique tracking.
  - 400 for malformed CSV or empty input; 404 for missing IDs.

---

### **Tests Passing (Part2Tests)**

- shouldCalculateUniqueCountsForSimpleCsv
- shouldCalculateUniqueCountsWithDuplicates
- shouldExcludeNullsFromUniqueCount
- shouldPersistUniqueCountsToDatabase
- shouldRetrievePreviousAnalysisById
- shouldRetrieveMultipleAnalysesIndependently
- shouldReturn404ForNonExistentAnalysis
- shouldDeleteAnalysisById
- shouldReturn404WhenDeletingNonExistentAnalysis
- shouldDeleteOnlySpecifiedAnalysis
- shouldCascadeDeleteColumnStatistics

---

### **Commands I Used**

```bash
# Run Part 2 only
./gradlew test --tests Part2Tests

# Run single Part 2 test
./gradlew test --tests Part2Tests.shouldCalculateUniqueCountsForSimpleCsv

# Run all tests (Part 1 + 2)
./gradlew test

# Run app for manual API tests
./gradlew bootRun
```

## Part 3 Completion Roadmap

### **Ideas to create**

- **Upgrade the analyzer**
  1.  Detect each column's data type `INT`, `DEC`, `BOOL`, `DATE`, `STRING`
  2.  Compute stats based on types `MIN/MAX/MEAN` for numbers `Earliest/Latest` for dates
  3.  Add a new Endpoint to fetch stats quickly: `GET/api/analysis/{id}/stats`
  4.  Data Quality Score (0-100) per column
  5.  Outlier detection for numeric columns (IQR method)
