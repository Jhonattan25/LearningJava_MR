package com.learning.webflux.app.models.dao;

import com.learning.webflux.app.models.documents.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ProductDao extends ReactiveMongoRepository<Product, String> {
}
