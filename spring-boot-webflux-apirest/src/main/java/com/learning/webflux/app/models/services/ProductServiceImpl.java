package com.learning.webflux.app.models.services;

import com.learning.webflux.app.models.dao.CategoryDao;
import com.learning.webflux.app.models.dao.ProductDao;
import com.learning.webflux.app.models.documents.Category;
import com.learning.webflux.app.models.documents.Product;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductDao productDao;
    private final CategoryDao categoryDao;

    public ProductServiceImpl(ProductDao productDao, CategoryDao categoryDao) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
    }

    @Override
    public Flux<Product> findAll() {
        return productDao.findAll();
    }

    @Override
    public Flux<Product> findAllWithNameUpperCase() {
        return productDao.findAll()
                .map(product -> {
                    product.setName(product.getName().toUpperCase());

                    return product;
                });
    }

    @Override
    public Flux<Product> findAllWithNameUpperCaseRepeat() {
        return findAllWithNameUpperCase().repeat(5000);
    }

    @Override
    public Mono<Product> findById(String id) {
        return productDao.findById(id);
    }

    @Override
    public Mono<Product> save(Product product) {
        return productDao.save(product);
    }

    @Override
    public Mono<Void> delete(Product product) {
        return productDao.delete(product);
    }

    @Override
    public Flux<Category> findAllCategory() {
        return categoryDao.findAll();
    }

    @Override
    public Mono<Category> findCategoryById(String id) {
        return categoryDao.findById(id);
    }

    @Override
    public Mono<Category> saveCategory(Category category) {
        return categoryDao.save(category);
    }

    @Override
    public Mono<Product> findByName(String name) {
        return productDao.findByName(name);
    }

    @Override
    public Mono<Product> getName(String name) {
        return productDao.getByName(name);
    }
}
