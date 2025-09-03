package com.inditex.similarproducts.infrastructure.monitoring;

import lombok.Getter;

@Getter
public enum MetricsType {
    PARTIAL_SUCCESS("partial_success"),
    SUCCESS("success"),
    EMPTY("empty"),
    ERROR("error"),
    BAD_REQUEST("bad_request"),
    NOT_FOUND("not_found"),
    FALLBACK("fallback"),
    SKIPPED_NOT_FOUND("skipped_not_found"),
    SKIPPED_ERROR("skipped_error");

    private final String value;

    MetricsType(String value) {
        this.value = value;
    }

}
