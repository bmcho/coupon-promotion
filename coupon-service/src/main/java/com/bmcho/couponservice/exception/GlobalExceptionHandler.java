package com.bmcho.couponservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // or OK (200) if you want hard idempotency
                .build();
    }

    @ExceptionHandler(CouponAlreadyIssuedException.class)
    public ResponseEntity<Map<String, Object>> handleCouponAlreadyIssued(CouponAlreadyIssuedException e) {
        return ResponseEntity
                .status(HttpStatus.OK) // or OK (200) if you want hard idempotency
                .body(Map.of(
                        "message", e.getMessage()
                ));
    }
}