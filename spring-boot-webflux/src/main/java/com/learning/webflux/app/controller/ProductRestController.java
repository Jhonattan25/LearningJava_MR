package com.learning.webflux.app.controller;

import com.learning.webflux.app.models.dao.ProductDao;
import com.learning.webflux.app.models.documents.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
public class ProductRestController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductDao productDao;

    public ProductRestController(ProductDao productDao) {
        this.productDao = productDao;
    }

    @GetMapping()
    public Flux<Product> index() {
        Flux<Product> products = productDao.findAll()
                .map(product -> {
                    product.setName(product.getName().toUpperCase());

                    return product;
                })
                .doOnNext(product -> log.info(product.getName()));

        return products;
    }

    @GetMapping("/{id}")
    public Mono<Product> show(@PathVariable String id) {
        //Mono<Product> product = productDao.findById(id); Mas clara y eficiente

        Flux<Product> products = productDao.findAll();

        Mono<Product> product = products
                .filter(productElement -> productElement.getId().equals(id))
                .next()
                .doOnNext(productElement -> log.info(productElement.getName()));

        return product;
    }
}
