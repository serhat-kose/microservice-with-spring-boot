package com.serhat.ecommerce.authservice.controller;

import com.serhat.ecommerce.authservice.dto.AddressDtos.AddressRequest;
import com.serhat.ecommerce.authservice.dto.AddressDtos.AddressResponse;
import com.serhat.ecommerce.authservice.service.AddressService;
import com.serhat.ecommerce.commons.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The address book of the <em>authenticated</em> caller. The owner is taken from the
 * gateway-supplied identity, never from a path variable, so one customer cannot reach
 * another's addresses.
 */
@RestController
@RequestMapping("/api/users/me/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list() {
        return ResponseEntity.ok(addressService.list(currentUserId()));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody AddressRequest request) {
        return new ResponseEntity<>(addressService.create(currentUserId(), request), HttpStatus.CREATED);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> update(@PathVariable Long addressId,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(currentUserId(), addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable Long addressId) {
        addressService.delete(currentUserId(), addressId);
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId() {
        return Long.valueOf(CurrentUser.requireUserId());
    }
}
