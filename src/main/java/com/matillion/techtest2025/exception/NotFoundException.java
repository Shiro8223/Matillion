package com.matillion.techtest2025.exception;

/**
 * Custom exception class representing a not found (HTTP 404) error.
 * <p>
 * This exception extends {@link RuntimeException}, which means:
 * <ul>
 *   <li>It's an unchecked exception (doesn't need to be declared in method signatures)</li>
 *   <li>It can propagate up the call stack automatically until caught</li>
 *   <li>Spring will handle it through the {@link GlobalExceptionHandler}</li>
 * </ul>
 * <p>
 * When this exception is thrown anywhere in the application, Spring's exception handling
 * mechanism will catch it and the {@link GlobalExceptionHandler} will convert it into
 * an HTTP 404 Not Found response.
 * <p>
 * <b>Example usage (Part 2):</b>
 * <pre>
 * DataAnalysisEntity entity = repository.findById(id)
 *     .orElseThrow(() -> new NotFoundException("Analysis not found with id: " + id));
 * </pre>
 *
 * @see GlobalExceptionHandler
 * @see RuntimeException
 */
public class NotFoundException extends RuntimeException {

    /**
     * Creates a new NotFoundException with the specified error message.
     * <p>
     * The message should clearly describe what resource was not found,
     * as it will be included in the HTTP response sent back to the client.
     *
     * @param message a descriptive error message explaining what was not found
     */
    public NotFoundException(String message) {
        super(message);
    }
}
