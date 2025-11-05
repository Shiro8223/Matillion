# Matillion Tech Test 2025

Welcome to the Matillion placement student technical test! This repository contains a Java-based Spring Boot service designed to test your skills in backend development, testing, and problem-solving.

## Overview

This is a data analysis service built with:
- **Java 21**
- **Spring Boot 3** (Web, JPA, Actuator)
- **Gradle** for dependency management
- **H2 Database** for lightweight in-memory persistence
- **Lombok** for reducing boilerplate code
- **JUnit 5** for testing

The service provides REST API endpoints for ingesting and analyzing data, with results persisted to an H2 database.

### Real-World Use Cases

This type of service is commonly used in data engineering and analytics platforms where users need to:
- **Explore unknown datasets** - Quickly understand the structure, data types, and basic statistics of CSV files without manual inspection
- **Data quality assessment** - Automatically detect data types, identify null values, and calculate statistical summaries to assess data completeness
- **ETL pipeline validation** - Verify data format and content before loading into data warehouses or lakes
- **Self-service analytics** - Enable business users to upload and analyze their own datasets through a simple API
- **Data profiling** - Generate metadata and summary statistics for data catalogs and governance tools

In production environments, similar services often integrate with cloud storage (S3, Azure Blob), handle larger file formats (Parquet, Avro), and scale horizontally to process multiple files concurrently.

## Prerequisites

- Java 21 (JDK 21)
- A Java-compatible IDE, such as IntelliJ IDEA

## Getting Started

### Build the Project
```bash
./gradlew build
```

### Run the Application
```bash
./gradlew bootRun
```

The service will start on `http://localhost:8080`

### Run Tests
```bash
./gradlew test
```

### Test the API Manually

Once the application is running, you can interact with the API using Swagger UI:

**Open in your browser:** `http://localhost:8080/swagger-ui/index.html`

This provides an interactive interface to test API endpoints without needing additional tools like Postman or curl.

## Test Structure

This technical test is divided into three Parts, designed to progressively challenge your skills:

### [Part 1: Fix the Failing Tests ⚠️](docs/part1.md)

The test class "Part1Tests" contains a test suite with multiple failing tests. Your first task is to identify and fix all failing tests.

[Read the full Part 1 requirements →](docs/part1.md)

### [Part 2: Extend API Functionality 🚀](docs/part2.md)

The test class "Part2Tests" contains a test suite for new functionality including unique value counts, data type inference, and additional REST endpoints.

[Read the full Part 2 requirements →](docs/part2.md)

### [Part 3: Design and Implement a New Feature 💡](docs/part3.md)

Think creatively and propose a new piece of functionality that would enhance this service - it's totally up to you!

[Read the full Part 3 requirements →](docs/part3.md)

## API Endpoints

### Data Analysis
- `POST /api/analysis/ingestCsv` - Ingest and analyze CSV data
- `GET /api/analysis/{id}` - Retrieve a previously analyzed CSV by ID (Part 2)
- `DELETE /api/analysis/{id}` - Delete an analysis by ID (Part 2)

## Tips

- Read the existing code carefully to understand the intended architecture
- Pay attention to test names and assertions - they tell you what's expected
- Don't hesitate to refactor if you see opportunities for improvement
- Write clear, self-documenting code
- Commit your changes regularly with meaningful commit messages
- **Use AI tools!** We actively encourage the use of AI coding assistants (GitHub Copilot, Claude, ChatGPT, etc.) to help you complete this test. Modern software development involves leveraging these tools effectively, and we want to see how you work with them

## Questions?

If you have any questions about the test requirements or encounter any setup issues, please reach out to your Matillion contact.

Good luck! 🎉
