package com.bmcho.couponservice.exception;

public class CouponAlreadyIssuedException extends RuntimeException {
    private final Long policyId;
    private final Long userId;

    public CouponAlreadyIssuedException(Long policyId, Long userId) {
        super("Coupon already issued for policyId=" + policyId + ", userId=" + userId);
        this.policyId = policyId;
        this.userId = userId;
    }
}