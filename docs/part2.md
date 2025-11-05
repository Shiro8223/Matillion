# Part 2: Extend API Functionality 🚀

The test class "Part2Tests" contains a test suite for functionality which doesn't exist yet. This includes:
- Reporting the number of unique values in a given column and the inferred type of the data
- A GET endpoint for retrieving previous results
- A DELETE endpoint for deleting previous results

Add the necessary code to allow these tests to pass.

## Getting Started

Run the Part 2 tests:
```bash
./gradlew test --tests Part2Tests
```

## Requirements

### 1. Enhanced Analysis Results
Extend the analysis to include:
- Count of unique values per column
- Inferred data type for each column (e.g., STRING, INTEGER, DECIMAL, BOOLEAN)

### 2. GET Endpoint
Implement `GET /api/analysis/{id}` to:
- Retrieve a previously analyzed CSV by its ID
- Return the full analysis results
- Handle cases where the ID doesn't exist

### 3. DELETE Endpoint
Implement `DELETE /api/analysis/{id}` to:
- Remove an analysis record by its ID
- Return appropriate status codes
- Handle cases where the ID doesn't exist

## Tips

- Review the test assertions carefully to understand the expected response format
- Think about proper HTTP status codes for different scenarios
