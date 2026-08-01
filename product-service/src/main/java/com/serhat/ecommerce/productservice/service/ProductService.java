package com.serhat.ecommerce.productservice.service;

import com.serhat.ecommerce.productservice.dto.ProductDtos.ProductRequest;
import com.serhat.ecommerce.productservice.dto.ProductDtos.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse addProduct(ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request);
    void deleteProduct(Long id);
    ProductResponse getProduct(Long id);
    List<ProductResponse> getAllProducts();
}
