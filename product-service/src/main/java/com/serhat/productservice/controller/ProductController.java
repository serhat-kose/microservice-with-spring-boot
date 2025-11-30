package com.serhat.productservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.serhat.productservice.entity.Product;
import com.serhat.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping("/addOne")
    public ResponseEntity<Product> addProduct(@RequestBody Product product) {
        try {
            Product saved = productService.addProduct(product);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/addList")
    public ResponseEntity<List<Product>> addProductList(@RequestBody List<Product> products) {
        try {
            List<Product> saved = productService.addProductList(products);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<Product>> getAllProduct() {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    //Send a product to the message queue
    @GetMapping("/sendToCart/{id}")
    public ResponseEntity<Product> sendToCart(@PathVariable Long id) {
        try {
            Optional<Product> productOpt = productService.sendToCart(id);
            if (productOpt.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(productOpt.get(), HttpStatus.OK);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
