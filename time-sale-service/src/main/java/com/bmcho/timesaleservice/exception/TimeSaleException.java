package com.bmcho.timesaleservice.exception;

import com.bmcho.timesaleservice.exception.common.ErrorCode;

import java.time.LocalDateTime;
import java.util.Map;

public class TimeSaleException extends TimeSaleBasicException {
    public TimeSaleException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TimeSaleException(ErrorCode errorCode, String message, Map<String, Object> details) {
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

    public static TimeSaleException notActive(Long timeSaleId) {
        return new TimeSaleException(
                ErrorCode.TIME_SALE_NOT_ACTIVE,
                null,
                Map.of("timeSaleId", timeSaleId)
        );
    }

    public static TimeSaleException notEnoughQuantity(Long remainingQuantity, Long requiredQuantity) {
        return new TimeSaleException(
                ErrorCode.NOT_ENOUGH_QUANTITY,
                null,
                Map.of("remainingQuantity", remainingQuantity, "requiredQuantity", requiredQuantity)
        );
    }

    public static TimeSaleException notInValidPeriod(LocalDateTime startAt, LocalDateTime endAt) {
        return new TimeSaleException(
                ErrorCode.NOT_IN_PERIOD,
                null,
                Map.of("startAt", startAt, "endAt", endAt)
        );
    }

    public static TimeSaleException failedToCreateRedisLock(Long timeSaleId) {
        return new TimeSaleException(
                ErrorCode.FAILED_TO_CREATE_REDIS_LOCK,
                null,
                Map.of("timeSaleId", timeSaleId)
        );
    }

    public static TimeSaleException failedToAcquireRedisLock() {
        return new TimeSaleException(
                ErrorCode.FAILED_TO_ACQUIRE_REDIS_LOCK,
                null,
                null
        );
    }
}
