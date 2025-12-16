package org.example.flowershop.repository;

import org.example.flowershop.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByUserIdOrderByPriceAsc(long userId);

    List<Order> findAllByUserIdOrderByStatusAsc(long userId);

    List<Order> findAllByUserIdOrderByOrderDateDesc(long userId);

}