package com.bmcho.timesaleservice.exception.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    UNHANDLED_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "TSE0000", "에러가 발생했습니다."),

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "TSE1001", "해당 상품을 찾을 수 없습니다."),

    TIME_SALE_NOT_FOUND(HttpStatus.NOT_FOUND, "TSE2001", "해당 타임세일을   찾을 수 없습니다."),
    INVALID_SALE_PERIOD(HttpStatus.BAD_REQUEST, "TSE2010", "시작 시간은 종료 시간보다 이전이어야 합니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "TSE2011", "수량은 1개 이상이어야 합니다."),
    INVALID_DISCOUNT_PRICE(HttpStatus.BAD_REQUEST, "TSE2012", "할인 가격은 0보다 커야 합니다."),
    TIME_SALE_NOT_ACTIVE(HttpStatus.FORBIDDEN, "TSE2013", "타임세일 상태가 '활성화'가 아닙니다."),
    NOT_ENOUGH_QUANTITY(HttpStatus.FORBIDDEN, "TSE2014", "상품 수량이 충분하지 않습니다."),
    NOT_IN_PERIOD(HttpStatus.FORBIDDEN, "TSE2015", "타임세일 기간이 아닙니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("[%s(%s)] %s", this.code, this.status, this.message);
    }
}
