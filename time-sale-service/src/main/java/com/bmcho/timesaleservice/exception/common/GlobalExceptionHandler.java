package com.bmcho.timesaleservice.exception.common;

import com.bmcho.timesaleservice.controller.response.TimeSaleApiResponse;
import com.bmcho.timesaleservice.exception.TimeSaleBasicException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TimeSaleBasicException.class)
    public ResponseEntity<TimeSaleApiResponse<Void>> handleBusiness(TimeSaleBasicException e) {
        ErrorCode ec = e.getErrorCode();
        ApiError error = ApiError.from(ec, e.getDetails());

        return ResponseEntity
                .status(ec.getStatus())
                .body(TimeSaleApiResponse.fail(ec.getCode(), ec.getMessage(), error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<TimeSaleApiResponse<Void>> handleUnhandled(Exception e) {
        log.error("Unhandled error", e);

        ErrorCode ec = ErrorCode.UNHANDLED_SERVER_ERROR;
        ApiError error = ApiError.from(ec, Map.of(
                // 운영에서는 보통 cause를 그대로 노출 안 함
                "traceId", org.slf4j.MDC.get("traceId")
        ));

        return ResponseEntity
                .status(ec.getStatus())
                .body(TimeSaleApiResponse.fail(ec.getCode(), ec.getMessage(), error));
    }

}
