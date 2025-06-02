package com.learning.webflux.app.models.dao;

import com.learning.webflux.app.models.documents.Product;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface ProductDao extends ReactiveMongoRepository<Product, String> {

    public Mono<Product> findByName(String name);

    @Query("{ 'name': ?0 }")
    public Mono<Product> getByName(String name);
}
