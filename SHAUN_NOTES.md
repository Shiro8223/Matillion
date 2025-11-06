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

# Part 3.2 — HTML Visualizer (Design, No Code)

## 1) What we already have

- **Ingest & persist**: Raw CSV stored + row/column/char counts + per-column nulls/uniques.
- **Stats API**: `/api/analysis/{id}/stats` returns:
  - Inferred types (`INTEGER/DECIMAL/BOOLEAN/DATE/STRING`)
  - Per-type stats (numeric min/max/mean/median/stddev; boolean true/false counts; date min/max; string length min/max)
  - Data Quality Score (0–100) + grade (A–F)
  - Outlier summary for numeric (IQR fences + count)

> Translation: We already have everything needed to render a compelling HTML report. The visualizer is just **presentation** over existing data.

---

## 2) What we will add

- A **human-readable HTML report** for any analysis ID:
  - Route: `GET /api/analysis/{id}/report`
  - Returns an HTML page with sections and simple charts (no heavy JS required).
- A **clean layout** with badges, tables, and lightweight charts built from the stats JSON.

---

## 3) Why this is valuable (interview-friendly impact)

- **Immediate clarity**: Stakeholders can skim a web page instead of JSON.
- **Professional polish**: Shows you think about the last mile — communicating results.
- **Reusability**: Same stats API powers multiple front-ends (HTML now, dashboard later).
- **Low risk**: No new persistence or complex algorithms — just UI over stable data.

---

## 4) Page structure (at a glance)

**Header**

- Title: “Dataset Report”
- Subheader: Analysis ID, created timestamp (from `/api/analysis/{id}` if you expose it), row/column counts.

**Quality Overview**

- **Overall quality** badge (e.g., average of column DQS).
- Top 3 best/worst columns by DQS.
- Outlier summary: count of numeric columns with outliers.

**Per-Column Cards** (repeat for each header, in original order)

- **Title**: `<columnName>` + type pill (e.g., `INTEGER`)
- **DQS badge**: e.g., `A (92)`
- **Key metrics** (type-aware):
  - Numeric: min / median / mean / max / stddev
  - Boolean: trueCount / falseCount (% split)
  - Date: minDate / maxDate (range)
  - String: minLength / maxLength
- **Nulls & Uniques**: `nullCount`, `uniqueCount`
- **Outliers (numeric only)**: `lowerFence`, `upperFence`, `outlierCount`
- **Micro-chart**:
  - Numeric → mini histogram (bucketed counts)
  - Boolean → mini bar (true vs false)
  - String → mini bar of length buckets (e.g., 0–10, 11–20, …)

**Footer**

- Disclaimer: “Exploratory profiling — not inferential statistics.”
- Link to raw JSON (`/api/analysis/{id}/stats`) for transparency.

---

## 5) UX details (simple, crisp, explainable)

- **Color system**:
  - DQS grade badge colors: A=green, B=teal, C=amber, D=orange, F=red.
  - Outlier flag: red dot if `outlierCount > 0`.
- **Layout**:
  - Two-column grid on desktop, single column on mobile.
  - Cards with subtle shadows and rounded corners for readability.
- **Accessibility**:
  - Semantic HTML (header, main, section, footer).
  - Sufficient color contrast; badge color + text label (not color alone).
  - Charts include aria-labels and numeric summaries.

---

## 6) Data mapping (what goes where)

- From `/stats`:
  - `columns[*].dataType` → type pill
  - `qualityScore`, `qualityGrade` → DQS badge + tooltip
  - Numeric stats → main metrics + histogram bins (computed client-side from values or summarized)
  - Boolean `trueCount/falseCount` → percentage bars
  - Date `minDate/maxDate` → displayed range
  - String `minLength/maxLength` → length range
  - `nullCount`, `uniqueCount` → info row
  - `outlierSummary` → red badge with count + fences in tooltip

---

## 7) API shape & endpoints (no code, just spec)

- **HTML report**: `GET /api/analysis/{id}/report` → `text/html`
  - Server composes HTML using the JSON from `/stats`.
- **Optional raw**: Link on the page to `/api/analysis/{id}/stats` (json) and `/api/analysis/{id}` (basic metadata).
- **Optional export**: `GET /api/analysis/{id}/report?format=pdf` (stretch goal — generate a PDF of the same HTML).

---

## 8) Acceptance criteria (what “done” means)

- Visiting `/api/analysis/{id}/report` renders:
  - Header with dataset counts (rows/cols).
  - Quality overview with average DQS and top/bottom columns.
  - One card per column with type-aware stats and DQS.
  - An outlier indicator where applicable.
  - A simple visual for each column (bar or histogram).
  - A footer with disclaimers and links to raw JSON.
- Works on mobile and desktop (responsive).
- Handles edge cases:
  - No numeric columns → no outlier section.
  - Header-only CSV → shows “no data rows” but still renders cards with zeroes.
  - Constant numeric column → outlier section hidden; stats show min=max and stddev≈0.

---

## 9) Demo script (interview walkthrough)

1. **Ingest** the provided F1 CSV (curl or UI).
2. **Open** `/api/analysis/{id}/report`.
3. **Narrate**:
   - “Type detection assigns each column a type.”
   - “Per-type stats appear here; DQS badges summarize cleanliness.”
   - “Outliers are flagged; numeric visuals show distribution.”
   - “You can click ‘View raw JSON’ to inspect the exact data.”
4. **Close** with why it matters: “Non-technical readers get value immediately; technical users still have the raw stats.”

---

## 10) Stretch ideas (if time allows, still no code here)

- **Filter bar** on the page (show only numeric/categorical/flagged columns).
- **Badge legend** explaining DQS grading.
- **Download buttons**: JSON, CSV, and (if implemented) PDF.
- **Shareable link** with a minimal query string (e.g., highlighting a specific column).
