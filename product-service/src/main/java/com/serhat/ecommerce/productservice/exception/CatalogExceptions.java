package com.serhat.ecommerce.productservice.exception;

public class CatalogExceptions {

    public static class CategoryNotFoundException extends RuntimeException {
        public CategoryNotFoundException(Object key) {
            super("Category not found: " + key);
        }
    }

    public static class BrandNotFoundException extends RuntimeException {
        public BrandNotFoundException(Object key) {
            super("Brand not found: " + key);
        }
    }

    public static class SellerNotFoundException extends RuntimeException {
        public SellerNotFoundException(Object key) {
            super("Seller not found: " + key);
        }
    }

    public static class DuplicateSkuException extends RuntimeException {
        public DuplicateSkuException(String sku) {
            super("SKU already exists: " + sku);
        }
    }

    /** Raised when a category would become its own ancestor. */
    public static class CategoryCycleException extends RuntimeException {
        public CategoryCycleException(Long categoryId) {
            super("Category " + categoryId + " cannot be its own ancestor");
        }
    }
}
