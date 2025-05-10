package com.learning.webflux.app.controllers;

import com.learning.webflux.app.models.documents.Product;
import com.learning.webflux.app.models.services.ProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.print.attribute.standard.Media;
import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Flux<Product> list() {
        return productService.findAll();
    }

    public Mono<ResponseEntity<Flux<Product>>> list2() {
        return Mono.just(ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(productService.findAll())
        );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> show(@PathVariable String id) {
        return productService.findById(id)
                .map(p -> ResponseEntity
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Product>> create(@RequestBody Product product) {
        if (product.getCreateAt() == null) {
            product.setCreateAt(LocalDateTime.now());
        }
        return productService.save(product)
                .map(p -> ResponseEntity
                        .created(URI.create("/api/products/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Product>> update(@PathVariable String id, @RequestBody Product product) {

        return productService.findById(id)
                .flatMap(p -> {
                    product.setId(id);
                    product.setCreateAt(p.getCreateAt());
                    return productService.save(product);
                })
                .map(p -> ResponseEntity
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable String id) {
        return productService.findById(id)
                .flatMap(p -> productService.delete(p)
                        .then(Mono.just(ResponseEntity
                                .noContent()
                                .build())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
