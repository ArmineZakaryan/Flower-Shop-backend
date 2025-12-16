package org.example.flowershop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.OrderDto;
import org.example.flowershop.dto.SaveOrderRequest;
import org.example.flowershop.exception.OrderNotFoundException;
import org.example.flowershop.exception.ProductNotFoundException;
import org.example.flowershop.exception.UserNotFoundException;
import org.example.flowershop.mapper.OrderMapper;
import org.example.flowershop.model.entity.Order;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.Status;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.repository.OrderRepository;
import org.example.flowershop.repository.ProductRepository;
import org.example.flowershop.repository.UserRepository;
import org.example.flowershop.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> findAll(Pageable pageable) {
        log.info("Finding all orders with pagination and sorting by: {}", pageable.getSort());

        Page<OrderDto> ordersPage = orderRepository.findAll(pageable)
                .map(orderMapper::toDto);

        log.info("Successfully retrieved {} orders", ordersPage.getNumberOfElements());
        return ordersPage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> findAll() {
        log.info("Finding all orders without pagination");

        List<Order> orders = orderRepository.findAll();
        log.info("Successfully retrieved {} orders", orders.size());

        return orderMapper.toDtoList(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto findById(long id) {
        log.info("Finding order with id: {}", id);

        Order order = orderRepository
                .findById(id)
                .orElseThrow(() -> {
                    log.error("Order with id: {} not found", id);
                    return new OrderNotFoundException("Order not found with id " + id);
                });
        log.info("Successfully retrieved order with id: {}", id);
        return orderMapper.toDto(order);
    }


    @Override
    public OrderDto save(SaveOrderRequest orderRequest, long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Product product = productRepository.findById(orderRequest.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        Order order = orderMapper.toEntity(orderRequest);
        order.setUser(user);
        order.setProduct(product);
        order.setPrice(product.getPrice() * orderRequest.getQuantity());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Status.NEW);


        Order savedOrder = orderRepository.save(order);
        return orderMapper.toDto(savedOrder);
    }


    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(long userId, String sortBy) {
        log.info("Finding orders for userId: {} with sorting by: {}", userId, sortBy);

        List<Order> orders;
        switch (sortBy) {
            case "price":
                orders = orderRepository.findAllByUserIdOrderByPriceAsc(userId);
                break;
            case "status":
                orders = orderRepository.findAllByUserIdOrderByStatusAsc(userId);
                break;
            case "orderDate":
            default:
                orders = orderRepository.findAllByUserIdOrderByOrderDateDesc(userId);
        }
        log.info("Found {} orders for userId: {} sorted by: {}", orders.size(), userId, sortBy);
        return orders;
    }


    @Override
    public OrderDto update(long id, SaveOrderRequest request, User currentUser) {
        log.info("Attempting to update order with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Order not found with id: {}", id);
                    return new OrderNotFoundException("Order not found with id " + id);
                });

        long minutesSinceOrder =
                Duration.between(order.getOrderDate(), LocalDateTime.now()).toMinutes();

        if (currentUser.getUserType() == UserType.ADMIN) {

            if (order.getStatus() != Status.NEW) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Admin can cancel only NEW orders"
                );
            }

            if (minutesSinceOrder > 10) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Admin cannot cancel order after 10 minutes"
                );
            }

            order.setStatus(Status.CANCELLED);
        } else if (currentUser.getId().equals(order.getUser().getId())) {

            if (order.getStatus() != Status.NEW) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You cannot update an order that is already " + order.getStatus()
                );
            }

            if (minutesSinceOrder > 10) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You cannot update order after 10 minutes"
                );
            }

            if (request.getAddress() != null) {
                order.setAddress(request.getAddress());
            }

            if (request.getQuantity() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Quantity must be greater than 0"
                );
            }
            order.setQuantity(request.getQuantity());
            order.setPrice(
                    order.getProduct().getPrice() * request.getQuantity()
            );
        } else {
            throw new AccessDeniedException("You cannot update this order");
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Successfully updated order with id: {}", id);

        return orderMapper.toDto(updatedOrder);
    }
}