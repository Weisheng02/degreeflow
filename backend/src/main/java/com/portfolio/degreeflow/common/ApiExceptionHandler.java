package com.portfolio.degreeflow.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiError notFound(NotFoundException exception) {
        return error("NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError conflict(ConflictException exception) {
        return error("CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError concurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        return error("CONFLICT", "This goal changed in another request. Refresh and try again.");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError badRequest(RuntimeException exception) {
        return error("BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiError forbidden(AccessDeniedException exception) {
        return error("FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(fieldError -> fields.put(fieldError.getField(), fieldError.getDefaultMessage()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "VALIDATION_FAILED");
        response.put("message", "Request validation failed");
        response.put("timestamp", Instant.now());
        response.put("fields", fields);
        return response;
    }

    private ApiError error(String code, String message) {
        return new ApiError(code, message, Instant.now());
    }

    record ApiError(String code, String message, Instant timestamp) {
    }
}
