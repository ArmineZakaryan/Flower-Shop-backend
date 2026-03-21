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
    public OrderDto findByIdForUser(long id, User currentUser) {
        log.info("Finding order id={} for userId={}", id, currentUser.getId());

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        boolean isAdmin = currentUser.getUserType() == UserType.ADMIN;
        boolean isOwner = order.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            log.warn("Access denied to orderId={} for userId={}", id, currentUser.getId());
            throw new AccessDeniedException("You are not allowed to access this order");
        }
        return orderMapper.toDto(order);
    }

    @Override
    public OrderDto save(SaveOrderRequest orderRequest, long userId) {

        log.info("Creating order for userId={} with productId={} and quantity={}",
                userId,
                orderRequest.getProductId(),
                orderRequest.getQuantity());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id={}", userId);
                    return new UserNotFoundException("User not found");
                });

        log.info("Found user id={}", user.getId());

        Product product = productRepository.findById(orderRequest.getProductId())
                .orElseThrow(() -> {
                    log.warn("Product not found with id={}", orderRequest.getProductId());
                    return new ProductNotFoundException("Product not found");
                });

        log.info("Found product id={}, name={}, price={}",
                product.getId(),
                product.getName(),
                product.getPrice());

        Order order = orderMapper.toEntity(orderRequest);
        order.setUser(user);
        order.setProduct(product);

        double totalPrice = product.getPrice() * orderRequest.getQuantity();
        order.setPrice(totalPrice);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Status.NEW);

        log.info("Order prepared: userId={}, productId={}, quantity={}, totalPrice={}, status={}",
                user.getId(),
                product.getId(),
                orderRequest.getQuantity(),
                totalPrice,
                Status.NEW);

        Order savedOrder = orderRepository.save(order);

        log.info("Order saved successfully with id={} for userId={}",
                savedOrder.getId(),
                user.getId());

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByUser(long userId, String sortBy) {
        log.info("Finding orders for userId={}  sortBy={}", userId, sortBy);

        String sort = switch (sortBy) {
            case "price", "status", "orderDate" -> sortBy;
            default -> "orderDate";
        };


        List<Order> orders = switch (sort) {
            case "price" -> orderRepository.findAllByUserIdOrderByPriceAsc(userId);
            case "status" -> orderRepository.findAllByUserIdOrderByStatusAsc(userId);
            default -> orderRepository.findAllByUserIdOrderByOrderDateDesc(userId);
        };

        log.info("Found {} orders for userId={}", orders.size(), userId);
        return orderMapper.toDtoList(orders);
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

        // Only the owner of the order can update it
        if (!currentUser.getId().equals(order.getUser().getId())) {
            throw new AccessDeniedException("You cannot update this order");
        }

        // Only NEW orders can be updated
        if (order.getStatus() != Status.NEW) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot update an order that is already " + order.getStatus()
            );
        }

        // Only within 10 minutes
        if (minutesSinceOrder > 10) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot update order after 10 minutes"
            );
        }

        // Update address
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            order.setAddress(request.getAddress());
        }

        // Update product if changed
        if (request.getProductId() != 0) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));

            order.setProduct(product);
        }

        // Validate quantity
        if (request.getQuantity() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be greater than 0"
            );
        }

        order.setQuantity(request.getQuantity());

        // Recalculate price
        order.setPrice(
                order.getProduct().getPrice() * request.getQuantity()
        );

        Order updatedOrder = orderRepository.save(order);

        log.info("Successfully updated order with id: {}", id);

        return orderMapper.toDto(updatedOrder);
    }
//
//    @Override
//    public OrderDto update(long id, SaveOrderRequest request, User currentUser) {
//        log.info("Attempting to update order with id: {}", id);
//
//        Order order = orderRepository.findById(id)
//                .orElseThrow(() -> {
//                    log.error("Order not found with id: {}", id);
//                    return new OrderNotFoundException("Order not found with id " + id);
//                });
//
//        long minutesSinceOrder =
//                Duration.between(order.getOrderDate(), LocalDateTime.now()).toMinutes();
//
//        // Only the owner of the order can update it
//        if (!currentUser.getId().equals(order.getUser().getId())) {
//            throw new AccessDeniedException("You cannot update this order");
//        }
//
//        if (order.getStatus() != Status.NEW) {
//            throw new ResponseStatusException(
//                    HttpStatus.FORBIDDEN,
//                    "You cannot update an order that is already " + order.getStatus()
//            );
//        }
//
//        if (minutesSinceOrder > 10) {
//            throw new ResponseStatusException(
//                    HttpStatus.FORBIDDEN,
//                    "You cannot update order after 10 minutes"
//            );
//        }
//
//        if (request.getAddress() != null) {
//            order.setAddress(request.getAddress());
//        }
//
//        if (request.getQuantity() <= 0) {
//            throw new ResponseStatusException(
//                    HttpStatus.BAD_REQUEST,
//                    "Quantity must be greater than 0"
//            );
//        }
//
//        order.setQuantity(request.getQuantity());
//        order.setPrice(
//                order.getProduct().getPrice() * request.getQuantity()
//        );
//
//        Order updatedOrder = orderRepository.save(order);
//        log.info("Successfully updated order with id: {}", id);
//
//        return orderMapper.toDto(updatedOrder);
//    }
}