package com.inditex.similarproducts.adapter.in.rest.controller;

import com.inditex.similarproducts.application.port.in.SimilarProductsUseCase;
import com.inditex.similarproducts.domain.model.ProductDetail;
import com.inditex.similarproducts.adapter.in.rest.error.ExceptionHandlerController;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SimilarProductsController.class)
@Import(ExceptionHandlerController.class)
class SimilarProductsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean SimilarProductsUseCase useCase;
    @MockitoBean MetricsRecorder metrics;

    @Test
    void getSimilarProducts_ok() throws Exception {
        List<ProductDetail> payload = List.of(
                new ProductDetail("1", "Name 1", 10.0, true),
                new ProductDetail("2", "Name 2", 20.0, false)
        );
        given(useCase.getSimilarProducts("10")).willReturn(payload);

        mvc.perform(get("/product/{productId}/similar", "10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"))
                .andExpect(jsonPath("$[0].availability").value(true))
                .andExpect(jsonPath("$[1].availability").value(false));
    }

}
