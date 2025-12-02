package com.bmcho.couponservice.exception;

import org.springframework.http.HttpStatus;

public class CouponPolicyNotFoundException extends CouponBasicException {
    public CouponPolicyNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public CouponPolicyNotFoundException(Long couponPolicyId) {
        super("쿠폰 정책을 찾을 수 없습니다: " + couponPolicyId, HttpStatus.NOT_FOUND);
    }
}