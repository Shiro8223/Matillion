package com.matillion.techtest2025.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Global exception handler for converting exceptions into HTTP error responses.
 * <p>
 * Uses {@code @RestControllerAdvice} to handle exceptions across all controllers.
 * Returns {@link ProblemDetail} objects following RFC 7807 standard for error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Converts {@link BadRequestException} to HTTP 400 Bad Request response.
     *
     * @param ex the exception
     * @return problem detail with error information
     */
    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequestException(BadRequestException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Bad Request");
        return problemDetail;
    }

    /**
     * Converts {@link NotFoundException} to HTTP 404 Not Found response.
     * <p>
     * <b>Part 2:</b> This handler is used by the GET and DELETE endpoints
     * when an analysis ID is not found in the database.
     *
     * @param ex the exception
     * @return problem detail with error information
     */
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundException(NotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Not Found");
        return problemDetail;
    }
}
