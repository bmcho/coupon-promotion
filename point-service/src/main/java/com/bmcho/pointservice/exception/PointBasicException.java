package com.bmcho.pointservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PointBasicException extends RuntimeException {

    private final HttpStatus status;

    public PointBasicException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public PointBasicException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public PointBasicException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;;
    }

    public PointBasicException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

}
