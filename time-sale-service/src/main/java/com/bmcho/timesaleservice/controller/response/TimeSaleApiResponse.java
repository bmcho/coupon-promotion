package com.bmcho.timesaleservice.controller.response;

import com.bmcho.timesaleservice.exception.common.ApiError;

public record TimeSaleApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        ApiError error
) {

    public static final String CODE_SUCCEED = "0000";
    public static final String MESSAGE_SUCCEED = "Success";

    public static final String CODE_FAILED = "9999";
    public static final String MESSAGE_FAILED = "Fail";


    public static <T> TimeSaleApiResponse<T> ok(T data) {
        return new TimeSaleApiResponse<>(true, CODE_SUCCEED, MESSAGE_SUCCEED, data, null);
    }

    public static <T> TimeSaleApiResponse<T> fail(String code, String message, ApiError error) {
        return new TimeSaleApiResponse<>(false, code, message, null, error);
    }
}
