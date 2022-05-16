package com.serhat.productservice.repository;

import com.serhat.productservice.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {

}
