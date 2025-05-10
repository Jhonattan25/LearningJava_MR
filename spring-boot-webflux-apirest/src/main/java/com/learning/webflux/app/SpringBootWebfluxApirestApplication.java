package com.learning.webflux.app;

import com.learning.webflux.app.models.documents.Category;
import com.learning.webflux.app.models.documents.Product;
import com.learning.webflux.app.models.services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@SpringBootApplication
public class SpringBootWebfluxApirestApplication {

    private static final Logger log = LoggerFactory.getLogger(SpringBootWebfluxApirestApplication.class);

    private final ProductService productService;
    private final ReactiveMongoTemplate mongoTemplate;

    public SpringBootWebfluxApirestApplication(ProductService productService,
                                               ReactiveMongoTemplate reactiveMongoTemplate) {
        this.productService = productService;
        this.mongoTemplate = reactiveMongoTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringBootWebfluxApirestApplication.class, args);
    }

    @Bean
    CommandLineRunner init() {
        return args -> {
            mongoTemplate.dropCollection("product").subscribe();
            mongoTemplate.dropCollection("categories").subscribe();

            Category electronic = new Category("Electrónico");
            Category deport = new Category("Deporte");
            Category computing = new Category("Computación");
            Category furniture = new Category("Muebles");

            Flux.just(electronic, deport, computing, furniture)
                    .flatMap(productService::saveCategory)
                    .doOnNext(c -> log.info("Insert category: {}", c))
                    .thenMany(
                            Flux.just(new Product("TV Panasonic Pantalla LCD", 456.89, electronic),
                                            new Product("Sony Camara HG Digital", 177.89, electronic),
                                            new Product("Apple iPad", 46.89, electronic),
                                            new Product("Sony Notebook", 846.89, computing),
                                            new Product("Bicicleta", 1000.89, deport),
                                            new Product("Macbook pro m4", 8000.89, computing),
                                            new Product("Mica cómoda 5 cajones", 3000.89, furniture))
                                    .flatMap(product -> {
                                        product.setCreateAt(LocalDateTime.now());
                                        return productService.save(product);
                                    })
                    ).subscribe(product -> log.info("Insert: {}", product));
        };
    }
}
