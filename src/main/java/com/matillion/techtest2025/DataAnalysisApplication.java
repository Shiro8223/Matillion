package com.matillion.techtest2025;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Data Analysis Service.
 * <p>
 * This class serves as the entry point for the Spring Boot application. The {@code @SpringBootApplication}
 * annotation is a convenience annotation that combines three important Spring annotations:
 * <ul>
 *   <li>{@code @Configuration} - Marks this class as a source of bean definitions</li>
 *   <li>{@code @EnableAutoConfiguration} - Enables Spring Boot's auto-configuration mechanism</li>
 *   <li>{@code @ComponentScan} - Enables component scanning to find and register Spring beans</li>
 * </ul>
 * <p>
 * When you run this application, Spring Boot will:
 * <ol>
 *   <li>Start an embedded web server (Tomcat by default) on port 8080</li>
 *   <li>Automatically configure the H2 database and JPA repositories</li>
 *   <li>Scan for and register all REST controllers and services</li>
 *   <li>Set up the API endpoints defined in the controllers</li>
 * </ol>
 *
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 */
@SpringBootApplication
public class DataAnalysisApplication {

    /**
     * Main method that launches the Spring Boot application.
     * <p>
     * This method delegates to Spring Boot's {@link SpringApplication#run} method, which:
     * <ul>
     *   <li>Creates the application context</li>
     *   <li>Initializes all Spring beans</li>
     *   <li>Starts the embedded web server</li>
     *   <li>Makes the API endpoints available for requests</li>
     * </ul>
     *
     * @param args command-line arguments passed to the application (not used in this application)
     */
    public static void main(String[] args) {
        SpringApplication.run(DataAnalysisApplication.class, args);
    }

}
