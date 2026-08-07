package com.serhat.ecommerce.authservice.repository;

import com.serhat.ecommerce.authservice.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserIdOrderByDefaultAddressDescIdAsc(Long userId);

    /** Scoped by userId so one customer can never load another's address by guessing an id. */
    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE UserAddress a SET a.defaultAddress = false WHERE a.userId = :userId")
    void clearDefaultFor(@Param("userId") Long userId);
}
