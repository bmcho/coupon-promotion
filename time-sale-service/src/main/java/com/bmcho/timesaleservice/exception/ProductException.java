package com.bmcho.timesaleservice.exception;

import com.bmcho.timesaleservice.exception.common.ErrorCode;

import java.util.Map;

public class ProductException extends TimeSaleBasicException {

    protected ProductException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected ProductException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }

    public static ProductException notFound(Long productId) {
        return new ProductException(
                ErrorCode.PRODUCT_NOT_FOUND,
                null,
                Map.of("productId", productId)) {};
    }

}
