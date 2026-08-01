package com.serhat.ecommerce.productservice.service;

import com.serhat.ecommerce.productservice.dto.ProductDtos.ProductRequest;
import com.serhat.ecommerce.productservice.dto.ProductDtos.ProductResponse;
import com.serhat.ecommerce.productservice.entity.Product;
import com.serhat.ecommerce.productservice.event.ProductEventPublisher;
import com.serhat.ecommerce.productservice.exception.ProductNotFoundException;
import com.serhat.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductEventPublisher eventPublisher;

    @Override
    @Transactional
    public ProductResponse addProduct(ProductRequest request) {
        Product product = new Product(null, request.name(), request.price());
        Product saved = productRepository.save(product);
        eventPublisher.publishCreated(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setName(request.name());
        product.setPrice(request.price());
        Product saved = productRepository.save(product);
        eventPublisher.publishUpdated(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
        eventPublisher.publishDeleted(id);
    }

    @Override
    public ProductResponse getProduct(Long id) {
        return productRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice());
    }
}
