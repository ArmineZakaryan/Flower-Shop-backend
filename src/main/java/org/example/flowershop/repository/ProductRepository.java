package org.example.flowershop.repository;

import org.example.flowershop.model.entity.Category;
import org.example.flowershop.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);

    Page<Product> findAllByCategory(Category category, Pageable pageable);
}
