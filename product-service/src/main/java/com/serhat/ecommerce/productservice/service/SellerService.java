package com.serhat.ecommerce.productservice.service;

import com.serhat.ecommerce.productservice.dto.CatalogDtos.SellerRequest;
import com.serhat.ecommerce.productservice.dto.CatalogDtos.SellerResponse;
import com.serhat.ecommerce.productservice.entity.Seller;
import com.serhat.ecommerce.productservice.exception.CatalogExceptions.SellerNotFoundException;
import com.serhat.ecommerce.productservice.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;

    @Transactional(readOnly = true)
    public SellerResponse getBySlug(String slug) {
        return sellerRepository.findBySlug(slug)
                .map(SellerResponse::from)
                .orElseThrow(() -> new SellerNotFoundException(slug));
    }

    @Transactional(readOnly = true)
    public Optional<Seller> findByUserId(String userId) {
        return sellerRepository.findByUserId(userId);
    }

    @Transactional
    public SellerResponse create(SellerRequest request) {
        Seller seller = Seller.builder()
                .name(request.name())
                .slug(SlugGenerator.uniqueSlug(
                        StringUtils.hasText(request.slug()) ? request.slug() : request.name(),
                        sellerRepository::existsBySlug))
                .userId(request.userId())
                .active(request.active() == null || request.active())
                .build();
        return SellerResponse.from(sellerRepository.save(seller));
    }

    @Transactional
    public SellerResponse update(Long id, SellerRequest request) {
        Seller seller = sellerRepository.findById(id).orElseThrow(() -> new SellerNotFoundException(id));
        seller.setName(request.name());
        if (StringUtils.hasText(request.slug())) {
            seller.setSlug(SlugGenerator.slugify(request.slug()));
        }
        if (request.active() != null) {
            seller.setActive(request.active());
        }
        return SellerResponse.from(sellerRepository.save(seller));
    }
}
