package com.bmcho.timesaleservice.repository;

import com.bmcho.timesaleservice.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}