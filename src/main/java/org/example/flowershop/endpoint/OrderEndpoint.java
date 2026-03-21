package org.example.flowershop.endpoint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.OrderDto;
import org.example.flowershop.dto.SaveOrderRequest;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
@Slf4j

public class OrderEndpoint {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderDto>> getMyOrders(
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("GET /orders called by userId={} sortBy={}", currentUser.getId(), sortBy);

        List<OrderDto> myOrders = orderService
                .getOrdersByUser(currentUser.getId(), sortBy);

        log.info("GET /orders returned {} orders for userId={}", myOrders.size(), currentUser.getId());
        return ResponseEntity.ok(myOrders);
    }

    @GetMapping("/all")
    public ResponseEntity<Page<OrderDto>> getAllOrders(
            Pageable pageable,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("GET /orders/all called by userId={}", currentUser.getId());

        if (currentUser.getUserType() != UserType.ADMIN) {
            throw new AccessDeniedException("Only admins can see all orders");
        }

        return ResponseEntity.ok(orderService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(
            @PathVariable long id,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("GET /orders/{} called by userId={}", id, currentUser.getId());

        OrderDto order = orderService.findByIdForUser(id, currentUser);

        log.info("GET /orders/{} success for userId={}", id, currentUser.getId());
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<OrderDto> create(
            @Valid @RequestBody SaveOrderRequest request,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("POST /orders called by userId={}", currentUser.getId());

        OrderDto created = orderService.save(request, currentUser.getId());

        log.info("Order successfully created for userId: {} with orderId: {}", currentUser.getId(), created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> update(
            @PathVariable long id,
            @RequestBody @Valid SaveOrderRequest request,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("PUT /orders/{} called by userId={}", id, currentUser.getId());

        OrderDto updated = orderService.update(id, request, currentUser);

        log.info("PUT /orders/{} update successfully", id);

        return ResponseEntity.ok(updated);
    }
}