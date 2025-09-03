package com.inditex.similarproducts.adapter.in.rest.error;

import com.inditex.similarproducts.domain.exception.ExternalServiceException;
import com.inditex.similarproducts.domain.exception.NotFoundException;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsEndpoint;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsRecorder;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsType;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.inditex.similarproducts.adapter.in.rest.error.ErrorMessage.*;
import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ExceptionHandlerController {
    private final MetricsRecorder metrics;

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<String> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        metrics.recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.BAD_REQUEST);
        return ResponseEntity.status(BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException ex) {
        log.info("Not found exception: {}", ex.getMessage());
        metrics.recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.NOT_FOUND);
        return ResponseEntity.status(NOT_FOUND).body(PRODUCT_NOT_FOUND.getMessage());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<String> handleExternal(ExternalServiceException ex) {
        log.error("External service error: {}", ex.getMessage());
        metrics.recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.ERROR);
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneric(Exception ex) {
        log.error("Unhandled error", ex);
        metrics.recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.ERROR);
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(INTERNAL_ERROR.getMessage());
    }
}
