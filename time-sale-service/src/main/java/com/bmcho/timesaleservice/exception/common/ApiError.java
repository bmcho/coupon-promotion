package com.bmcho.timesaleservice.exception.common;

import java.util.Map;

public record ApiError(
        ErrorCode errorCode,
        Map<String, Object> details // 필드 에러, 원인 등 추가 정보
) {
    public static ApiError of(ErrorCode errorCode) {
        return new ApiError(errorCode, null);
    }

    public static ApiError of(ErrorCode errorCode, Map<String, Object> details) {
        return new ApiError(errorCode, details);
    }
}
