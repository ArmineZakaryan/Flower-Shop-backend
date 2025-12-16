package org.example.flowershop.service;

import jakarta.validation.Valid;
import org.example.flowershop.dto.OrderDto;
import org.example.flowershop.dto.SaveOrderRequest;
import org.example.flowershop.model.entity.Order;
import org.example.flowershop.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    Page<OrderDto> findAll(Pageable pageable);

    List<OrderDto> findAll();

    OrderDto findById(long id);

    OrderDto save(SaveOrderRequest orderRequest, long userId);


    List<Order> getOrdersByUser(long id, String sortBy);

    OrderDto update(long id, @Valid SaveOrderRequest request, User currentUser);

}