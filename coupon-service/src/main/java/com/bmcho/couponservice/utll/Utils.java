package com.bmcho.couponservice.utll;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;

public class Utils {

    public static boolean isDuplicateKey(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException) {
            return ((ConstraintViolationException) e.getCause()).getKind().equals(ConstraintViolationException.ConstraintKind.UNIQUE);
        }

        return e.getCause() instanceof SQLIntegrityConstraintViolationException;
    }
}
