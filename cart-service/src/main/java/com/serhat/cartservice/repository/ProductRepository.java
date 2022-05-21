package com.serhat.cartservice.repository;

import com.serhat.cartservice.entity.*;
import org.springframework.data.jpa.repository.*;

public interface ProductRepository  extends JpaRepository<Product,Long> {
}
