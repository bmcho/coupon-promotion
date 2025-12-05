package com.bmcho.pointservice.exception;

import com.bmcho.pointservice.domain.PointBalance;

public class PointNotFound extends PointBasicException {
    public PointNotFound(Long pointId) {
        super(String.format("Point Not Found. pointId : %d", pointId));
    }

}
