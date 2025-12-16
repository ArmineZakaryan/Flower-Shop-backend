package org.example.flowershop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.model.entity.Order;
import org.example.flowershop.model.enums.Status;
import org.example.flowershop.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusScheduler {

    private final OrderRepository orderRepository;

    @Scheduled(fixedRate = 60000)
    public void updateDeliveredOrders() {
        LocalDateTime now = LocalDateTime.now();

        List<Order> ordersToDeliver = orderRepository.findAll()
                .stream()
                .filter(order -> order.getStatus() == Status.NEW)
                .filter(order -> Duration.between(order.getOrderDate(), now).toMinutes() >= 30)
                .toList();

        ordersToDeliver.forEach(order -> {
            order.setStatus(Status.DELIVERED);
            orderRepository.save(order);
            log.info("Order id {} automatically updated to DELIVERED", order.getId());
        });
    }
}