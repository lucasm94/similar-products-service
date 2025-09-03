package com.inditex.similarproducts;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class SimilarProductsApplicationTest {

	@Test
	void main_shouldRunSpringApplication() {
		try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
			mocked.when(() -> SpringApplication.run(SimilarProductsApplication.class, new String[]{}))
					.thenReturn(null);

			SimilarProductsApplication.main(new String[]{});

			mocked.verify(() -> SpringApplication.run(SimilarProductsApplication.class, new String[]{}));
		}
	}
}
