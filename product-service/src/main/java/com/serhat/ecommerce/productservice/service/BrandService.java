package com.serhat.ecommerce.productservice.service;

import com.serhat.ecommerce.productservice.dto.CatalogDtos.BrandRequest;
import com.serhat.ecommerce.productservice.dto.CatalogDtos.BrandResponse;
import com.serhat.ecommerce.productservice.entity.Brand;
import com.serhat.ecommerce.productservice.exception.CatalogExceptions.BrandNotFoundException;
import com.serhat.ecommerce.productservice.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public Page<BrandResponse> list(Pageable pageable) {
        return brandRepository.findByActiveTrue(pageable).map(BrandResponse::from);
    }

    @Transactional(readOnly = true)
    public BrandResponse getBySlug(String slug) {
        return brandRepository.findBySlug(slug)
                .map(BrandResponse::from)
                .orElseThrow(() -> new BrandNotFoundException(slug));
    }

    @Transactional
    public BrandResponse create(BrandRequest request) {
        Brand brand = Brand.builder()
                .name(request.name())
                .slug(SlugGenerator.uniqueSlug(
                        StringUtils.hasText(request.slug()) ? request.slug() : request.name(),
                        brandRepository::existsBySlug))
                .logoUrl(request.logoUrl())
                .active(request.active() == null || request.active())
                .build();
        return BrandResponse.from(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = brandRepository.findById(id).orElseThrow(() -> new BrandNotFoundException(id));
        brand.setName(request.name());
        if (StringUtils.hasText(request.slug())) {
            brand.setSlug(SlugGenerator.slugify(request.slug()));
        }
        brand.setLogoUrl(request.logoUrl());
        if (request.active() != null) {
            brand.setActive(request.active());
        }
        return BrandResponse.from(brandRepository.save(brand));
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = brandRepository.findById(id).orElseThrow(() -> new BrandNotFoundException(id));
        // Soft-deleted: products still reference the brand, and hard-deleting would either
        // orphan them or cascade away catalog data.
        brand.setActive(false);
        brandRepository.save(brand);
    }
}
