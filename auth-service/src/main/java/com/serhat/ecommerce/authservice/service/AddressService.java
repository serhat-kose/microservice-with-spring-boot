package com.serhat.ecommerce.authservice.service;

import com.serhat.ecommerce.authservice.dto.AddressDtos.AddressRequest;
import com.serhat.ecommerce.authservice.dto.AddressDtos.AddressResponse;
import com.serhat.ecommerce.authservice.model.UserAddress;
import com.serhat.ecommerce.authservice.repository.UserAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AddressService {

    private final UserAddressRepository repository;

    public AddressService(UserAddressRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return repository.findByUserIdOrderByDefaultAddressDescIdAsc(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        UserAddress address = UserAddress.builder()
                .userId(userId)
                .title(request.title())
                .recipientName(request.recipientName())
                .phone(request.phone())
                .line1(request.line1())
                .line2(request.line2())
                .city(request.city())
                .district(request.district())
                .postalCode(request.postalCode())
                .country(request.country())
                .defaultAddress(request.defaultAddress())
                .build();

        // Only one address per customer may be the default, so clear the previous one first.
        if (request.defaultAddress()) {
            repository.clearDefaultFor(userId);
        }
        return AddressResponse.from(repository.save(address));
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = requireOwned(userId, addressId);

        address.setTitle(request.title());
        address.setRecipientName(request.recipientName());
        address.setPhone(request.phone());
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setDistrict(request.district());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());

        if (request.defaultAddress() && !address.isDefaultAddress()) {
            repository.clearDefaultFor(userId);
        }
        address.setDefaultAddress(request.defaultAddress());

        return AddressResponse.from(repository.save(address));
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        repository.delete(requireOwned(userId, addressId));
    }

    /**
     * Looks the address up by id <em>and</em> owner, so a customer passing someone else's
     * address id gets a 404 rather than access to it.
     */
    private UserAddress requireOwned(Long userId, Long addressId) {
        return repository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NoSuchElementException("Address not found: " + addressId));
    }
}
