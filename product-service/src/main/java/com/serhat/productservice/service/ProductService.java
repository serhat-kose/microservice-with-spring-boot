package com.serhat.productservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.serhat.productservice.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    Product addProduct(Product product);
    List<Product> addProductList(List<Product> products);
    List<Product> getAllProducts();
    Optional<Product> sendToCart(Long id) throws JsonProcessingException;
}

