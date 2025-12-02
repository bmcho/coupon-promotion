package com.bmcho.couponservice.exception;

import org.springframework.http.HttpStatus;

public class CouponIssueException extends CouponBasicException {

    public CouponIssueException(String message) {
        super(message);
    }

    public CouponIssueException() {
        super("쿠폰 발급 중 오류가 발생했습니다");
    }

    public CouponIssueException(String message, HttpStatus status) {
        super(message, status);
    }

}