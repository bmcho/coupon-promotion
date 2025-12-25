package com.bmcho.timesaleservice.exception.common;

import com.bmcho.timesaleservice.controller.TimeSaleApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

import static com.bmcho.timesaleservice.exception.common.ErrorCode.UNHANDLED_SERVER_ERROR;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    protected TimeSaleApiResponse<?> handleRuntimeException(RuntimeException e) {
        log.error("error={}", e.getMessage(), e);
        Map<String, Object> detail = new HashMap<>();
        detail.put("cause", e.getCause());
        ApiError error = ApiError.of(UNHANDLED_SERVER_ERROR, detail);
        return TimeSaleApiResponse.fail(error);
    }


}
