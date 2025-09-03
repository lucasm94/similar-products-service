package com.inditex.similarproducts.adapter.in.rest.error;

import com.inditex.similarproducts.domain.exception.ExternalServiceException;
import com.inditex.similarproducts.domain.exception.NotFoundException;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

class ExceptionHandlerControllerTest {
    @Mock MetricsRecorder metrics;
    ExceptionHandlerController advice;

    @BeforeEach
    void setup(){
        MockitoAnnotations.openMocks(this);
        advice = new ExceptionHandlerController(metrics);
    }

    @Test
    void handleBadRequest_returns400(){
        ResponseEntity<String> resp = advice.handleBadRequest(new IllegalArgumentException("bad request"));
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        assertThat(resp.getBody()).contains("bad request");
    }

    @Test
    void handleNotFound_returns404(){
        ResponseEntity<String> resp = advice.handleNotFound(new NotFoundException("not found"));
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        assertThat(resp.getBody()).contains("not found");
    }

    @Test
    void handleExternal_propagatesStatus(){
        ExternalServiceException ex = new ExternalServiceException("error", new RuntimeException("error"), 503);
        ResponseEntity<String> resp = advice.handleExternal(ex);
        assertThat(resp.getStatusCode().value()).isEqualTo(503);
        assertThat(resp.getBody()).contains("error");
        verify(metrics).recordRequest(anyString(), any());
    }

    @Test
    void handleGeneric_returns500(){
        ResponseEntity<String> resp = advice.handleGeneric(new RuntimeException("internal error"));
        assertThat(resp.getStatusCode().value()).isEqualTo(500);
        assertThat(resp.getBody()).contains("Internal error");
        verify(metrics).recordRequest(anyString(), any());
    }
}
