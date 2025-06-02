package com.learning.webflux.app.handler;

import com.learning.webflux.app.models.documents.Category;
import com.learning.webflux.app.models.documents.Product;
import com.learning.webflux.app.models.services.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ProductHandler {

    private final ProductService productService;
    private final String pathImages;
    private final Validator validator;

    ProductHandler(ProductService productService, @Value("${config.uploads.path}") String pathImages,
                   Validator validator) {
        this.productService = productService;
        this.pathImages = pathImages;
        this.validator = validator;
    }

    public Mono<ServerResponse> createWithImage(ServerRequest request) {
        Mono<Product> product = request.multipartData()
                .map(multipart -> {
                    FormFieldPart name = (FormFieldPart) multipart.toSingleValueMap().get("name");
                    FormFieldPart price = (FormFieldPart) multipart.toSingleValueMap().get("price");
                    FormFieldPart categoryId = (FormFieldPart) multipart.toSingleValueMap().get("category.id");
                    FormFieldPart categoryName = (FormFieldPart) multipart.toSingleValueMap().get("category.name");

                    Category category = new Category(categoryName.value());
                    category.setId(categoryId.value());

                    return new Product(name.value(), Double.parseDouble(price.value()), category);
                });

        return product.flatMap(p -> {
                    if (p.getCreateAt() == null) {
                        p.setCreateAt(LocalDateTime.now());
                    }

                    return Mono.just(p);
                })
                .flatMap(p -> request.multipartData()
                        .map(multipart -> multipart.toSingleValueMap().get("file"))
                        .cast(FilePart.class)
                        .flatMap(filePart -> {
                            String imageName = filePart.filename()
                                    .replace(" ", "")
                                    .replace(":", "")
                                    .replace("\\", "");

                            p.setImage(UUID.randomUUID() + "-" + imageName);

                            return filePart.transferTo(new File(pathImages + p.getImage()))
                                    .then(productService.save(p));
                        })
                )
                .flatMap(p -> ServerResponse.created(URI
                                .create("/api/v2/products/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p));
    }

    public Mono<ServerResponse> upload(ServerRequest request) {
        String id = request.pathVariable("id");

        return request.multipartData()
                .map(multipart -> multipart.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(filePart -> productService.findById(id)
                        .flatMap(p -> {

                            String imageName = filePart.filename()
                                    .replace(" ", "")
                                    .replace(":", "")
                                    .replace("\\", "");

                            p.setImage(UUID.randomUUID() + "-" + imageName);

                            return filePart.transferTo(new File(pathImages + p.getImage()))
                                    .then(productService.save(p));
                        }))
                .flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse
                        .notFound()
                        .build());
    }

    public Mono<ServerResponse> list(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(productService.findAll(), Product.class);
    }

    public Mono<ServerResponse> show(ServerRequest request) {

        String id = request.pathVariable("id");

        return productService.findById(id)
                .flatMap(p -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse
                        .notFound()
                        .build());
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        Mono<Product> product = request.bodyToMono(Product.class);

        return product.flatMap(p -> {

            Errors errors = new BeanPropertyBindingResult(p, Product.class.getName());
            validator.validate(p, errors);

            if (errors.hasErrors()) {
                return Flux.fromIterable(errors.getFieldErrors())
                        .map(fieldError -> "The field " + fieldError.getField() + " " + fieldError.getDefaultMessage())
                        .collectList()
                        .flatMap(list -> ServerResponse
                                .badRequest()
                                .bodyValue(list));
            } else {
                if (p.getCreateAt() == null) {
                    p.setCreateAt(LocalDateTime.now());
                }

                return productService.save(p)
                        .flatMap(pdb -> ServerResponse.created(URI
                                        .create("/api/v2/products/".concat(pdb.getId())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(pdb));
            }
        });
    }

    public Mono<ServerResponse> update(ServerRequest request) {

        String id = request.pathVariable("id");
        Mono<Product> newProduct = request.bodyToMono(Product.class);
        Mono<Product> productDb = productService.findById(id);

        return productDb.zipWith(newProduct, (db, req) -> {
                    db.setName(req.getName());
                    db.setPrice(req.getPrice());
                    db.setCategory(req.getCategory());
                    return db;
                }).flatMap(p -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(productService.save(p), Product.class))
                .switchIfEmpty(ServerResponse
                        .notFound()
                        .build());
    }

    public Mono<ServerResponse> delete(ServerRequest request) {

        String id = request.pathVariable("id");

        Mono<Product> productDb = productService.findById(id);

        return productDb
                .flatMap(productService::delete)
                .then(ServerResponse
                        .noContent()
                        .build())
                .switchIfEmpty(ServerResponse
                        .notFound()
                        .build());
    }
}
