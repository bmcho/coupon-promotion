package com.bmcho.pointservice.exception;

import org.springframework.http.HttpStatus;

public class LockAcquisitionFailedException extends PointBasicException {

    public LockAcquisitionFailedException(String key) {
        super("Failed to acquire lock for " + key, HttpStatus.CONFLICT);
    }
}