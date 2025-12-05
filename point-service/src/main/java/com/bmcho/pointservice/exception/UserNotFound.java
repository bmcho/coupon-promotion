package com.bmcho.pointservice.exception;

import org.springframework.http.HttpStatus;

public class UserNotFound extends PointBasicException {

    public UserNotFound(Long userId) {
        super(String.format("User Not Found. userId : %d", userId), HttpStatus.NOT_FOUND);
    }
}
