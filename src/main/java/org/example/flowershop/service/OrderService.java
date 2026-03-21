package org.example.flowershop.service;

import jakarta.validation.Valid;
import org.example.flowershop.dto.OrderDto;
import org.example.flowershop.dto.SaveOrderRequest;
import org.example.flowershop.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    Page<OrderDto> findAll(Pageable pageable);

    OrderDto findByIdForUser(long id, User currentUser);

    OrderDto save(SaveOrderRequest orderRequest, long userId);

    List<OrderDto> getOrdersByUser(long id, String sortBy);

    OrderDto update(long id, @Valid SaveOrderRequest request, User currentUser);
}