package com.matillion.techtest2025.exception;

/**
 * Custom exception class representing a bad request (HTTP 400) error.
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
 * an HTTP 400 Bad Request response.
 * <p>
 * <b>Example usage:</b>
 * <pre>
 * if (data.contains("invalid")) {
 *     throw new BadRequestException("Invalid data format");
 * }
 * </pre>
 *
 * @see GlobalExceptionHandler
 * @see RuntimeException
 */
public class BadRequestException extends RuntimeException {

    /**
     * Creates a new BadRequestException with the specified error message.
     * <p>
     * The message should clearly describe what was wrong with the request,
     * as it will be included in the HTTP response sent back to the client.
     *
     * @param message a descriptive error message explaining why the request was invalid
     */
    public BadRequestException(String message) {
        super(message);
    }
}
