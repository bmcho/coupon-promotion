package com.bmcho.couponservice.exception;

import org.springframework.http.HttpStatus;

public class CouponNotFoundException extends CouponBasicException {
    public CouponNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public CouponNotFoundException(Long couponId) {
        super("쿠폰을 찾을 수 없습니다: " + couponId, HttpStatus.NOT_FOUND);
    }
}