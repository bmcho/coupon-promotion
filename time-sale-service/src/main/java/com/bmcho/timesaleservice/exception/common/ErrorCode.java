package com.bmcho.timesaleservice.exception.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    UNHANDLED_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "TSE0000", "에러가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("[%s(%s)] %s", this.code, this.status, this.message);
    }
}
