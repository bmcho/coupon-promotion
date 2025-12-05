package com.bmcho.pointservice.exception;

public class PointAlreadyCanceled extends PointBasicException {
    public PointAlreadyCanceled(Long pointId) {
        super(String.format("Point Already Canceled. pointId : %d", pointId));
    }
}
