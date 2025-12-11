package com.bmcho.pointservice.exception;

public class PointAlreadyCanceled extends PointBasicException {
    public PointAlreadyCanceled(Long pointId) {
        super("Point Already Canceled. pointId : %d".formatted(pointId));
    }
}
