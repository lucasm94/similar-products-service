package com.inditex.similarproducts.domain.exception;

import lombok.Getter;

@Getter
public class ExternalServiceException extends RuntimeException {
    private final Integer status;

    public ExternalServiceException(String message, Throwable cause, Integer status) {
        super(message, cause);
        this.status = status;
    }

    public ExternalServiceException(String message, Integer status) {
        super(message);
        this.status = status;
    }
}
