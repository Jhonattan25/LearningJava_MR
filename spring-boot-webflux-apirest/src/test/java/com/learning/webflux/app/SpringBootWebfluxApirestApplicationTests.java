package com.learning.webflux.app;

import com.learning.webflux.app.models.documents.Product;
import com.learning.webflux.app.models.services.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpringBootWebfluxApirestApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ProductService productService;

	@Test
	void listTestSuccess() {

		webTestClient.get()
				.uri("/api/v2/products")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBodyList(Product.class)
				.hasSize(7);

	}

	@Test
	void listTestSuccess2() {

		webTestClient.get()
				.uri("/api/v2/products")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBodyList(Product.class)
				.consumeWith(response -> {
					List<Product> products = response.getResponseBody();
                    assert products != null;
                    products.forEach(p -> {
						System.out.println(p.getName());
					});

					assertEquals(7, products.size());
				});

	}

	@Test
	void showTestSuccess() {

		String productName = "TV Panasonic Pantalla LCD";

		Product product = productService.findByName(productName)
				.block();

		webTestClient.get()
				.uri("/api/v2/products/{id}", Collections.singletonMap("id", product.getId()))
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(Product.class)
				.consumeWith(response -> {
					Product productResponse = response.getResponseBody();

                    assert productResponse != null;
                    assertNotNull(productResponse.getId());
					assertEquals(productName, productResponse.getName());
				});
				//.jsonPath("$.id").isNotEmpty()
				//.jsonPath("$.name").isEqualTo(productName);
	}

}
