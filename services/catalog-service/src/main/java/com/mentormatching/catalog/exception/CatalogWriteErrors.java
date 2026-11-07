package com.mentormatching.catalog.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;

import static org.springframework.http.HttpStatus.CONFLICT;

public final class CatalogWriteErrors {

    private CatalogWriteErrors() {
    }

    public static RuntimeException duplicateName(DataIntegrityViolationException error, String constraint,
                                                 String code, String message) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                if (name != null && (name.equals(constraint) || name.endsWith("." + constraint))) {
                    return new ApiException(CONFLICT, code, message);
                }
            }
        }
        return error;
    }
}
