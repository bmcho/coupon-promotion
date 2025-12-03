package com.bmcho.pointservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class PointBasicException extends RuntimeException {

    private final HttpStatus status;

    public PointBasicException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public PointBasicException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public PointBasicException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

}
