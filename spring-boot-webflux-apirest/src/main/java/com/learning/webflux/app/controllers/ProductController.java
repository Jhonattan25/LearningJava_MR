package com.learning.webflux.app.controllers;

import com.learning.webflux.app.models.documents.Product;
import com.learning.webflux.app.models.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Value("${config.uploads.path}")
    private String pathImages;

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/v2")
    public Mono<ResponseEntity<Product>> createWithImage(Product product, @RequestPart FilePart file) {
        if (product.getCreateAt() == null) {
            product.setCreateAt(LocalDateTime.now());
        }

        String routeImage = file.filename()
                .replace(" ", "")
                .replace(":", "")
                .replace("\\", "");

        product.setImage(UUID.randomUUID().toString() + "-" + routeImage);

        return file.transferTo(new File(pathImages + product.getImage()))
                .then(productService.save(product))
                .map(p -> ResponseEntity
                        .created(URI.create("/api/products/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p));
    }

    @PostMapping("/upload/{id}")
    public Mono<ResponseEntity<Product>> uploadImage(@PathVariable String id, @RequestPart FilePart file) {
        return productService.findById(id)
                .flatMap(p -> {

                    String routeImage = file.filename()
                            .replace(" ", "")
                            .replace(":", "")
                            .replace("\\", "");
                    p.setImage(UUID.randomUUID().toString() + "-" + routeImage);

                    return file.transferTo(new File(pathImages + p.getImage()))
                            .then(productService.save(p));
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
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

    @PostMapping("/old")
    public Mono<ResponseEntity<Product>> createOld(@RequestBody Product product) {
        if (product.getCreateAt() == null) {
            product.setCreateAt(LocalDateTime.now());
        }
        return productService.save(product)
                .map(p -> ResponseEntity
                        .created(URI.create("/api/products/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p));
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> createMono(@Valid @RequestBody Mono<Product> monoProduct) {

        Map<String, Object> response = new HashMap<>();

        return monoProduct
                .flatMap(product -> {
                    if (product.getCreateAt() == null) {
                        product.setCreateAt(LocalDateTime.now());
                    }

                    return productService.save(product)
                            .map(p -> {
                                response.put("product", p);
                                response.put("status", HttpStatus.CREATED.value());
                                response.put("message", "Producto creado con exito");
                                response.put("timestamp", LocalDateTime.now());
                                return p;
                            })
                            .map(p -> ResponseEntity
                                    .created(URI.create("/api/products/".concat(p.getId())))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(response));
                })
                .onErrorResume(t -> Mono.just(t).cast(WebExchangeBindException.class)
                        .flatMap(e -> Mono.just(e.getFieldErrors()))
                        .flatMapMany(Flux::fromIterable)
                        .map(fieldError -> "El campo " + fieldError.getField() + " " +
                                fieldError.getDefaultMessage())
                        .collectList()
                        .flatMap(list -> {
                            response.put("errors", list);
                            response.put("status", HttpStatus.BAD_REQUEST.value());
                            response.put("message", "Se genero un error");
                            response.put("timestamp", LocalDateTime.now());
                            return Mono.just(ResponseEntity.badRequest().body(response));
                        })
                );
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
