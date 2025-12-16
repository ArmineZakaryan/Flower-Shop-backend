package org.example.flowershop.endpoint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.OrderDto;
import org.example.flowershop.dto.SaveOrderRequest;
import org.example.flowershop.exception.OrderNotFoundException;
import org.example.flowershop.mapper.OrderMapper;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
@Slf4j
public class OrderEndpoint {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @GetMapping
    public ResponseEntity<List<OrderDto>> getMyOrders(
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        log.info("Fetching orders for userId: {} sorted by: {}", currentUser.getId(), sortBy);

        List<String> allowedSort = List.of("orderDate", "price", "status");
        if (!allowedSort.contains(sortBy)) {
            sortBy = "orderDate";
        }

        List<OrderDto> myOrders = orderService
                .getOrdersByUser(currentUser.getId(), sortBy)
                .stream()
                .map(orderMapper::toDto)
                .toList();

        log.info("Successfully fetched {} orders for userId: {} sorted by: {}", myOrders.size(), currentUser.getId(), sortBy);
        return ResponseEntity.ok(myOrders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(
            @PathVariable long id,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        OrderDto order;
        try {
            order = orderService.findById(id);
        } catch (OrderNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }

        boolean isAdmin = currentUser.getUserType() == UserType.ADMIN;
        boolean isOwner = order.getUserId() == currentUser.getId();

        if (!isAdmin && !isOwner) {
            log.warn("UserId: {} is trying to access an order that does not belong to them", currentUser.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this order");
        }

        log.info("Successfully fetched order with id: {} for userId: {}", id, currentUser.getId());
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<OrderDto> create(
            @RequestBody SaveOrderRequest request,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("Received request to create order for userId: {}", currentUser != null ? currentUser.getId() : "null");

        if (currentUser == null) {
            log.error("User is not authenticated. Cannot create order.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        log.info("Creating order for userId: {}", currentUser.getId());

        OrderDto created = orderService.save(request, currentUser.getId());

        log.info("Order successfully created for userId: {} with orderId: {}", currentUser.getId(), created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> update(
            @PathVariable long id,
            @RequestBody @Valid SaveOrderRequest request,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        log.info("User with id: {} is attempting to update order with id: {}", currentUser.getId(), id);

        OrderDto updated = orderService.update(id, request, currentUser);

        log.info("Successfully updated order with id: {}", id);

        return ResponseEntity.ok(updated);
    }
}