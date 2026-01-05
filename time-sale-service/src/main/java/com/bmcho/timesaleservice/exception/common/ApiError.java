package com.bmcho.timesaleservice.exception.common;

import java.util.HashMap;
import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, Object> details
) {
    public static ApiError from(ErrorCode errorCode) {
        Map<String, Object> details = new HashMap<>();
        return new ApiError(errorCode.getCode(), errorCode.getMessage(), details);
    }

    public static ApiError from(ErrorCode errorCode, Map<String, Object> details) {
        return new ApiError(errorCode.getCode(), errorCode.getMessage(), details);
    }
}
