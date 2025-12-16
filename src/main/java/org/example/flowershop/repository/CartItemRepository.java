package org.example.flowershop.repository;

import org.example.flowershop.model.entity.CartItem;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByUserId(long userId, Sort sort);
}

