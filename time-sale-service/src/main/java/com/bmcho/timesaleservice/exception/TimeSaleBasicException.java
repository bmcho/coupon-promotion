package com.bmcho.timesaleservice.exception;

import com.bmcho.timesaleservice.exception.common.ErrorCode;
import lombok.Getter;

import java.util.Map;

@Getter
public abstract class TimeSaleBasicException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    protected TimeSaleBasicException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    protected TimeSaleBasicException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public static TimeSaleBasicException invalidPeriod(ErrorCode errorCode, String message) {

    }
}
