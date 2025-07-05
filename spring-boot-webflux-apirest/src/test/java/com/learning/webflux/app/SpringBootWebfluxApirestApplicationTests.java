package com.learning.webflux.app;

import com.learning.webflux.app.models.documents.Category;
import com.learning.webflux.app.models.documents.Product;
import com.learning.webflux.app.models.services.ProductService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) Para levantar el servidor real
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // Para mockear el servidor
class SpringBootWebfluxApirestApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProductService productService;

    @Value("${config.base.endpoint}")
    private String url;

    @Test
    void listTestSuccess() {

        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Product.class)
                .hasSize(8);

    }

    @Test
    void listTestSuccess2() {

        webTestClient.get()
                .uri(url)
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

                    assertEquals(8, products.size());
                });

    }

    @Test
    void showTestSuccess() {

        String productName = "TV Panasonic Pantalla LCD";

        Product product = productService.findByName(productName)
                .block();

        webTestClient.get()
                .uri(url + "/{id}", Collections.singletonMap("id", product.getId()))
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

    @Test
    public void createTest() {

        Category electronic = productService.findCategoryByName("Electrónico").block();

        Product product = new Product("Apple watch", 999_000.00, electronic);

        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(product), Product.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.name").isEqualTo("Apple watch")
                .jsonPath("$.category.name").isEqualTo("Electrónico");
    }

    @Test
    public void create2Test() {

        Category electronic = productService.findCategoryByName("Electrónico").block();

        Product product = new Product("Apple watch", 999_000.00, electronic);

        webTestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(product), Product.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Product.class)
                .consumeWith(response -> {
                    Product productResponse = response.getResponseBody();
                    assert productResponse != null;
                    Assertions.assertThat(productResponse.getId()).isNotEmpty();
                    Assertions.assertThat(productResponse.getName()).isEqualTo("Apple watch");
                    Assertions.assertThat(productResponse.getCategory().getName()).isEqualTo("Electrónico");
                });
    }

    @Test
    public void updateTest() {

        Product product = productService.findByName("Sony Notebook").block();
        Category category = productService.findCategoryByName("Electrónico").block();

        Product productUpdate = new Product("Sony Notebook 2G", 999.99, category);

        assert product != null;

        webTestClient.put()
                .uri(url + "/{id}", Collections.singletonMap("id", product.getId()))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productUpdate)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Product.class)
                .consumeWith(response -> {
                    Product productResponse = response.getResponseBody();
                    assert productResponse != null;
                    Assertions.assertThat(productResponse.getId()).isNotEmpty();
                    Assertions.assertThat(productResponse.getName()).isEqualTo("Sony Notebook 2G");
                    Assertions.assertThat(productResponse.getPrice()).isEqualTo(999.99);
                    Assertions.assertThat(productResponse.getCategory().getName()).isEqualTo("Electrónico");
                });
    }

    @Test
    public void deleteTest() {
        Product product = productService.findByName("Bicicleta").block();

        assert product != null;

        webTestClient.delete()
                .uri(url + "/{id}", Collections.singletonMap("id", product.getId()))
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri(url + "/{id}", Collections.singletonMap("id", product.getId()))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().isEmpty();
    }
}
