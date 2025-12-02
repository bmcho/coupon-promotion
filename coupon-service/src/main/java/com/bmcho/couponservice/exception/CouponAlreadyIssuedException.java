package com.bmcho.couponservice.exception;

import com.bmcho.couponservice.domain.Coupon;
import org.springframework.http.HttpStatus;

public class CouponAlreadyIssuedException extends CouponBasicException {

    public CouponAlreadyIssuedException(String message) {
        super(message);
    }

    public CouponAlreadyIssuedException(Long policyId, Long userId) {
        super("Coupon already issued for policyId=" + policyId + ", userId=" + userId,
                HttpStatus.CONFLICT);
    }
}