package com.serhat.ecommerce.productservice.service;

import com.serhat.ecommerce.productservice.dto.ProductDtos.ImageRequest;
import com.serhat.ecommerce.productservice.dto.ProductDtos.ProductDetail;
import com.serhat.ecommerce.productservice.dto.ProductDtos.ProductRequest;
import com.serhat.ecommerce.productservice.dto.ProductDtos.ProductSummary;
import com.serhat.ecommerce.productservice.dto.ProductDtos.VariantRequest;
import com.serhat.ecommerce.productservice.entity.*;
import com.serhat.ecommerce.productservice.event.ProductEventPublisher;
import com.serhat.ecommerce.productservice.exception.CatalogExceptions.BrandNotFoundException;
import com.serhat.ecommerce.productservice.exception.CatalogExceptions.CategoryNotFoundException;
import com.serhat.ecommerce.productservice.exception.CatalogExceptions.DuplicateSkuException;
import com.serhat.ecommerce.productservice.exception.CatalogExceptions.SellerNotFoundException;
import com.serhat.ecommerce.productservice.exception.ProductNotFoundException;
import com.serhat.ecommerce.productservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SellerRepository sellerRepository;
    private final CategoryService categoryService;
    private final ProductEventPublisher eventPublisher;

    /**
     * Filtered, paged listing. Replaces an unbounded {@code findAll()} that serialised the
     * entire catalog into a single response.
     */
    @Transactional(readOnly = true)
    public Page<ProductSummary> search(String keyword, Long categoryId, Long brandId, Long sellerId,
                                       BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minRating,
                                       Pageable pageable) {

        // Filtering by a parent category must include everything beneath it, otherwise
        // "Electronics" would appear empty while all products sit in its child categories.
        Set<Long> categoryIds = categoryId == null ? null : categoryService.selfAndDescendantIds(categoryId);

        Specification<Product> spec = Specification.where(ProductSpecifications.visibleToShoppers())
                .and(ProductSpecifications.keyword(keyword))
                .and(ProductSpecifications.inCategories(categoryIds))
                .and(ProductSpecifications.hasBrand(brandId))
                .and(ProductSpecifications.hasSeller(sellerId))
                .and(ProductSpecifications.priceAtLeast(minPrice))
                .and(ProductSpecifications.priceAtMost(maxPrice))
                .and(ProductSpecifications.minimumRating(minRating));

        return productRepository.findAll(spec, pageable).map(ProductSummary::from);
    }

    /** Product pages are the hottest read in the system, so the assembled detail is cached. */
    @Cacheable(value = "productDetail", key = "'slug:' + #slug")
    @Transactional(readOnly = true)
    public ProductDetail getBySlug(String slug) {
        return productRepository.findWithDetailsBySlug(slug)
                .map(ProductDetail::from)
                .orElseThrow(() -> new ProductNotFoundException(slug));
    }

    @Cacheable(value = "productDetail", key = "'id:' + #id")
    @Transactional(readOnly = true)
    public ProductDetail getById(Long id) {
        return productRepository.findWithDetailsById(id)
                .map(ProductDetail::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    @CacheEvict(value = "productDetail", allEntries = true)
    public ProductDetail create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .slug(SlugGenerator.uniqueSlug(
                        StringUtils.hasText(request.slug()) ? request.slug() : request.name(),
                        productRepository::existsBySlug))
                .description(request.description())
                .basePrice(request.basePrice())
                .status(request.status() == null ? ProductStatus.DRAFT : request.status())
                .build();

        applyAssociations(product, request);
        applyImages(product, request.images());
        applyVariants(product, request.variants());

        Product saved = productRepository.save(product);
        eventPublisher.publishCreated(saved);
        return ProductDetail.from(saved);
    }

    @Transactional
    @CacheEvict(value = "productDetail", allEntries = true)
    public ProductDetail update(Long id, ProductRequest request) {
        Product product = productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(request.name());
        if (StringUtils.hasText(request.slug())) {
            product.setSlug(SlugGenerator.slugify(request.slug()));
        }
        product.setDescription(request.description());
        product.setBasePrice(request.basePrice());
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        applyAssociations(product, request);

        if (request.images() != null) {
            product.getImages().clear();
            applyImages(product, request.images());
        }
        if (request.variants() != null) {
            product.getVariants().clear();
            applyVariants(product, request.variants());
        }

        Product saved = productRepository.save(product);
        eventPublisher.publishUpdated(saved);
        return ProductDetail.from(saved);
    }

    @Transactional
    @CacheEvict(value = "productDetail", allEntries = true)
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        // Withdrawn rather than removed: past orders and carts still need to resolve the
        // product, and a hard delete would leave them pointing at nothing.
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
        eventPublisher.publishDeleted(id);
    }

    /** True when the given account owns the seller this product is listed under. */
    @Transactional(readOnly = true)
    public boolean isOwnedBySellerAccount(Long productId, String userId) {
        return productRepository.findById(productId)
                .map(Product::getSeller)
                .filter(seller -> seller.getUserId().equals(userId))
                .isPresent();
    }

    private void applyAssociations(Product product, ProductRequest request) {
        if (request.categoryId() != null) {
            product.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.categoryId())));
        }
        if (request.brandId() != null) {
            product.setBrand(brandRepository.findById(request.brandId())
                    .orElseThrow(() -> new BrandNotFoundException(request.brandId())));
        }
        if (request.sellerId() != null) {
            product.setSeller(sellerRepository.findById(request.sellerId())
                    .orElseThrow(() -> new SellerNotFoundException(request.sellerId())));
        }
    }

    private void applyImages(Product product, List<ImageRequest> images) {
        if (images == null) {
            return;
        }
        int order = 0;
        for (ImageRequest image : images) {
            product.addImage(ProductImage.builder()
                    .url(image.url())
                    .altText(image.altText())
                    .displayOrder(image.displayOrder() == null ? order : image.displayOrder())
                    .build());
            order++;
        }
    }

    private void applyVariants(Product product, List<VariantRequest> variants) {
        if (variants == null) {
            return;
        }
        for (VariantRequest variant : variants) {
            // Checked explicitly so a clashing SKU surfaces as a 409 rather than a raw
            // constraint-violation 500 from the database.
            variantRepository.findBySku(variant.sku())
                    .filter(existing -> !existing.getProduct().getId().equals(product.getId()))
                    .ifPresent(existing -> {
                        throw new DuplicateSkuException(variant.sku());
                    });

            ProductVariant entity = ProductVariant.builder()
                    .sku(variant.sku())
                    .price(variant.price())
                    .listPrice(variant.listPrice())
                    .build();
            if (variant.attributes() != null) {
                entity.getAttributes().putAll(variant.attributes());
            }
            product.addVariant(entity);
        }
    }
}
