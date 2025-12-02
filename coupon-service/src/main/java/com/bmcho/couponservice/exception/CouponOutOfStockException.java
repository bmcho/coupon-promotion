package com.bmcho.couponservice.exception;

import org.springframework.http.HttpStatus;

public class CouponOutOfStockException extends CouponBasicException{

    public CouponOutOfStockException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public CouponOutOfStockException() {
        super("쿠폰이 모두 소진 되었습니다.", HttpStatus.CONFLICT);
    }
}
