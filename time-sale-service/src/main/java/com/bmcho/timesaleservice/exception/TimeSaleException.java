package com.bmcho.timesaleservice.exception;

import com.bmcho.timesaleservice.exception.common.ErrorCode;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

public class TimeSaleException extends TimeSaleBasicException {
    protected TimeSaleException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected TimeSaleException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }

    public static TimeSaleException notFound(Long timeSaleId) {
        return new TimeSaleException(
                ErrorCode.TIME_SALE_NOT_FOUND,
                null,
                Map.of("timeSaleId", timeSaleId)
        );
    }

    public static TimeSaleException invalidPeriod(LocalDateTime startAt, LocalDateTime endAt) {
        return new TimeSaleException(
                ErrorCode.INVALID_SALE_PERIOD,
                null,
                Map.of("startAt", startAt, "endAt", endAt)
        );
    }

    public static TimeSaleException invalidQuantity(Long quantity) {
        return new TimeSaleException(
                ErrorCode.INVALID_QUANTITY,
                null,
                Map.of("quantity", quantity)
        );
    }

    public static TimeSaleException invalidDiscountPrice(Long discountPrice) {
        return new TimeSaleException(
                ErrorCode.INVALID_SALE_PERIOD,
                null,
                Map.of("discountPrice", discountPrice)
        );
    }
}
