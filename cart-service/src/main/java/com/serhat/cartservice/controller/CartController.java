package com.serhat.cartservice.controller;

import com.serhat.cartservice.entity.*;
import com.serhat.cartservice.repository.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    ProductRepository productRepository;

    @GetMapping("/getProducts")
    List<Product> getCartProduct() {
        return productRepository.findAll();
    }

    @GetMapping("/deleteOne/{id}")
    void deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
    }

    @GetMapping("/deleteAll")
    void deleteProducts() {
        productRepository.deleteAll();
    }

    @GetMapping("/info")
    String getInfo() {
        return "cart microservice";
    }
}
