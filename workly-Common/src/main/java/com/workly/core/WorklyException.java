package com.workly.core;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WorklyException extends RuntimeException {
    private final HttpStatus status;

    public WorklyException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static WorklyException badRequest(String message) {
        return new WorklyException(message, HttpStatus.BAD_REQUEST);
    }

    public static WorklyException unauthorized(String message) {
        return new WorklyException(message, HttpStatus.UNAUTHORIZED);
    }

    public static WorklyException notFound(String message) {
        return new WorklyException(message, HttpStatus.NOT_FOUND);
    }

    public static WorklyException forbidden(String message) {
        return new WorklyException(message, HttpStatus.FORBIDDEN);
    }

    public static WorklyException internal(String message) {
        return new WorklyException(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
