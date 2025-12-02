package com.bmcho.couponservice.exception;

import org.springframework.http.HttpStatus;

public class CouponIssueNotAvailableException extends CouponBasicException {
    public CouponIssueNotAvailableException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public CouponIssueNotAvailableException() {
        super("쿠폰 발급기간이 아닙니다.", HttpStatus.FORBIDDEN);
    }

}
