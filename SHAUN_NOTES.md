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

## Part 3.1 Completion Roadmap

### **What was implemented**

- **New intelligent analyzer features**
  1. **Automatic Type Inference**  
     Detects each column's data type from CSV input:  
     `INTEGER`, `DECIMAL`, `BOOLEAN`, `DATE`, or `STRING`.
  2. **Descriptive Statistics by Type**
     - **Numeric:** `min`, `max`, `mean`, `median`, `stddev`
     - **Boolean:** `trueCount`, `falseCount`
     - **Date:** `minDate`, `maxDate`
     - **String:** `minLength`, `maxLength`
  3. **New Stats Endpoint**  
     Added `GET /api/analysis/{id}/stats` to return enhanced per-column analytics.
  4. **Data Quality Score (DQS)**  
     Each column receives a score from **0–100** and a letter grade **(A–F)**  
     based on null rate, invalid type rate, and cardinality balance.
  5. **Outlier Detection (IQR Method)**  
     Numeric columns are evaluated for statistical outliers using the Interquartile Range (IQR):
     ```
     Q1 = 25th percentile
     Q3 = 75th percentile
     IQR = Q3 - Q1
     lowerFence = Q1 - 1.5 * IQR
     upperFence = Q3 + 1.5 * IQR
     ```
     Values outside this range are flagged as outliers and summarized in
     an `outlierSummary` block.

---

### ⚙️ **API Summary**

#### **1. Ingest CSV (existing)**

- POST /api/analysis/ingestCsv
- Content-Type: text/plain
- Body: raw CSV data

#### **2. Fetch Enhanced Stats (new)**

- GET /api/analysis/{id}/stats

##### Example Response

```json
{
  "id": 1,
  "columns": [
    {
      "columnName": "age",
      "dataType": "INTEGER",
      "nullCount": 0,
      "uniqueCount": 3,
      "min": 18.0,
      "max": 40.0,
      "mean": 26.67,
      "median": 22.0,
      "stddev": 9.57,
      "qualityScore": 90,
      "qualityGrade": "A",
      "outlierSummary": {
        "lowerFence": 3.5,
        "upperFence": 47.5,
        "outlierCount": 0
      }
    },
    {
      "columnName": "is_active",
      "dataType": "BOOLEAN",
      "trueCount": 2,
      "falseCount": 1,
      "qualityScore": 100,
      "qualityGrade": "A"
    },
    {
      "columnName": "signup_date",
      "dataType": "DATE",
      "minDate": "2024-01-01",
      "maxDate": "2024-12-31",
      "qualityScore": 90,
      "qualityGrade": "A"
    },
    {
      "columnName": "notes",
      "dataType": "STRING",
      "minLength": 2,
      "maxLength": 5,
      "nullCount": 1,
      "uniqueCount": 2,
      "qualityScore": 77,
      "qualityGrade": "C"
    }
  ]
}
```

### **How Data Quality Score is Calculated**

| Factor                  | Description                           | Max Penalty |
| ----------------------- | ------------------------------------- | ----------- |
| **Null Rate**           | Percentage of empty cells             | −40         |
| **Invalid Rate**        | Values that don't match inferred type | −40         |
| **Cardinality Balance** | Too few or too many unique values     | −10         |

Final score = `100 - (nullPenalty + invalidPenalty + cardinalityPenalty)`  
Grades: **A ≥ 90**, **B ≥ 80**, **C ≥ 70**, **D ≥ 60**, **F < 60**

## Testing Commands

- Run only Part 3 tests:
  ```bash
  ./gradlew test --tests "*Part3*"
  ```
- Run all tests:

  ```bash
  ./gradlew test
  ```

- Start application:
  ```bash
  ./gradlew bootRun
  ```
- Manual smoke test:
  ```bash
  # Ingest CSV
  curl -s -X POST "http://localhost:8080/api/analysis/ingestCsv" \
  	-H "Content-Type: text/plain" \
  	--data-binary $'age,is_active,signup_date,notes\n18,true,2024-01-01,ok\n22,false,2024-03-05,hello\n40,true,2024-12-31,'
  ```
  ```bash
  # Fetch stats
  curl -s "http://localhost:8080/api/analysis/1/stats"
  ```

## Outcome

- All Part 3 JUnit tests pass
- Endpoint /api/analysis/{id}/stats returns full analytics payload
- Analyzer now provides intelligent, production-grade insight per column

# Part 3.2 — HTML & PDF Visualizer

## Summary

> “In Part 3.2, I implemented a complete HTML + PDF visualization layer on top of our data-profiling backend.  
> The system dynamically generates a dataset report using existing analysis results, then exports it as both web and PDF formats.  
> It demonstrates clean routing, server-side rendering, and full-stack polish turning raw analytics into a clear, shareable insight report.”

---

## 1) What we implemented

- **HTML visualizer endpoint:**  
  `GET /api/analysis/{id}/report`  
  Generates a fully formatted, responsive HTML dataset report using:

  - `DataAnalysisService.getAnalysisById(id)` for metadata (rows, columns, timestamps)
  - `DataAnalysisService.getStats(id)` for per-column analytics

- **PDF export (stretch goal achieved):**  
  `GET /api/analysis/{id}/report?format=pdf`  
  Converts the same HTML into a downloadable PDF using **OpenHTMLtoPDF** (`PdfRendererBuilder`), with XHTML output for XML compliance.

- **Raw JSON download:**  
  `GET /api/analysis/{id}/stats?download=true`  
  Returns a pretty-printed JSON download with correct `Content-Disposition` headers, avoiding overlap with `/stats` via a `params="download=true"` constraint.

---

## 2) Key features in the HTML report

| Section             | Details                                                                                                                          |
| ------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| **Header**          | Displays “Dataset Report”, analysis ID, creation date, and summary metrics (row/column counts).                                  |
| **Column Table**    | Lists each column with: name, inferred type, nullCount, uniqueCount, data-quality score (grade A–F + numeric), and outlier info. |
| **DQ Badges**       | Color-coded pills — A=blue, B=teal, C=amber, D=orange, F=red.                                                                    |
| **Outlier Display** | Shows ▲ icon (lower/upper fences + outlier count).                                                                               |
| **Actions Section** | “Download JSON” and “Download PDF” buttons.                                                                                      |
| **Footer**          | Mentions raw JSON endpoint and provides direct link for transparency.                                                            |

---

## 3) Architecture & Integration

- **Data flow:**

  - The `/stats` API already provided full per-column stats, DQ grades, and outlier summaries.
  - The new controller **reuses** these endpoints to compose the HTML dynamically — no new persistence or services were added.

- **Routing fixes:**

  - `params="download=true"` was introduced for the JSON download route to prevent Spring’s _ambiguous mapping_ conflict with `DataAnalysisController#stats(Long)`.
  - Both controllers now boot without error.

- **Rendering stack:**
  - HTML is generated server-side via `StringBuilder` with embedded CSS.
  - XHTML (strict XML) is used for PDF generation compatibility with `openhtmltopdf`.
  - Minimal, dependency-free CSS ensures consistent rendering in browsers and PDFs.

---

## 4) Why this matters

- **Professional presentation:** Converts JSON stats into a stakeholder-friendly visual summary.
- **End-to-end thinking:** Demonstrates understanding from data ingestion to human-readable reporting.
- **Reusability:** HTML and PDF both pull from the same analysis + stats data model.

---

## 5) Validation Checklist

| Feature                                                    | Status |
| ---------------------------------------------------------- | ------ |
| `/api/analysis/{id}/report` renders HTML                   | ✅     |
| `/api/analysis/{id}/report?format=pdf` downloads valid PDF | ✅     |
| `/api/analysis/{id}/stats?download=true` downloads JSON    | ✅     |
| Mobile & desktop responsive                                | ✅     |
| No controller conflicts                                    | ✅     |
