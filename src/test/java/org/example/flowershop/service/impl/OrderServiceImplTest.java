package org.example.flowershop.service.impl;

import org.example.flowershop.dto.OrderDto;
import org.example.flowershop.dto.SaveOrderRequest;
import org.example.flowershop.exception.OrderNotFoundException;
import org.example.flowershop.mapper.OrderMapper;
import org.example.flowershop.model.entity.Order;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.Status;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.repository.OrderRepository;
import org.example.flowershop.repository.ProductRepository;
import org.example.flowershop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private Product product;

    @Mock
    private UserRepository userRepository;

    private User user;
    private User admin;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setUserType(UserType.USER);

        admin = new User();
        admin.setId(2L);
        admin.setUserType(UserType.ADMIN);
    }

    @Test
    void findAllPageable_shouldReturnPageOfOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Order> order = List.of(
                new Order(1L, user, 10.0, Status.NEW, LocalDateTime.now(), "Gyumri", 2, product),
                new Order(2L, user, 12.0, Status.DELIVERED, LocalDateTime.now(), "Yerevan", 5, product));
        Page<Order> page = new PageImpl<>(order, pageable, 1);

        when(orderRepository.findAll(pageable)).thenReturn(page);
        when(orderMapper.toDto(any(Order.class)))
                .thenAnswer(inv -> {
                    Order orders = inv.getArgument(0);
                    return OrderDto.builder()
                            .id(orders.getId())
                            .userId(orders.getUser().getId())
                            .price(orders.getPrice())
                            .status(orders.getStatus())
                            .orderDate(orders.getOrderDate())
                            .address(orders.getAddress())
                            .build();
                });
        Page<OrderDto> result = orderServiceImpl.findAll(pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(1, result.getContent().get(0).getId());
    }


    @Test
    void findAll_shouldReturnListOfOrder() {
        List<Order> order = List.of(
                new Order(1L, user, 10.0, Status.NEW, LocalDateTime.now(), "Gyumri", 2, product),
                new Order(2L, user, 12.0, Status.DELIVERED, LocalDateTime.now(), "Yerevan", 5, product));


        List<OrderDto> dtoList = List.of(
                OrderDto.builder()
                        .id(1L)
                        .userId(user.getId())
                        .price(10.0)
                        .status(Status.NEW)
                        .orderDate(LocalDateTime.now())
                        .address("Gyumri")
                        .build(),

                OrderDto.builder()
                        .id(2L)
                        .userId(user.getId())
                        .price(12.0)
                        .status(Status.DELIVERED)
                        .orderDate(LocalDateTime.now())
                        .address("Yerevan")
                        .build()
        );

        when(orderRepository.findAll()).thenReturn(order);
        when(orderMapper.toDtoList(order)).thenReturn(dtoList);

        List<OrderDto> result = orderServiceImpl.findAll();

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals(2, result.get(1).getId());

        verify(orderRepository).findAll();
        verify(orderMapper).toDtoList(order);

    }

    @Test
    void findById_shouldReturnOrder() {
        Order order = new Order(1L, user, 10.0, Status.NEW, LocalDateTime.now(), "Gyumri", 2, product);

        OrderDto dto = OrderDto.builder()
                .id(1L)
                .userId(user.getId())
                .price(10.0)
                .status(Status.NEW)
                .orderDate(LocalDateTime.now())
                .address("Gyumri")
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(dto);

        OrderDto result = orderServiceImpl.findById(1L);

        assertEquals(1, result.getId());
    }

    @Test
    void findById_shouldThrowException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> orderServiceImpl.findById(1L));
    }


    @Test
    void save_shouldReturnOrder() {
        long userId = 1L;
        user.setId(userId);

        SaveOrderRequest request = new SaveOrderRequest();
        request.setProductId(product.getId());
        request.setAddress("Gyumri");
        request.setQuantity(3);

        Order order = new Order(1L, user, 10.0, Status.NEW, LocalDateTime.now(), "Gyumri", 2, product);

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderMapper.toEntity(any(SaveOrderRequest.class))).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(orderMapper.toDto(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);

            return OrderDto.builder()
                    .id(1L)
                    .userId(userId)
                    .price(o.getPrice())
                    .status(o.getStatus())
                    .orderDate(o.getOrderDate())
                    .address(o.getAddress())
                    .build();
        });

        OrderDto result = orderServiceImpl.save(request, userId);

        assertEquals(1, result.getId());
        verify(orderRepository).save(any(Order.class));
        verify(userRepository).findById(userId);

    }


    @Test
    void getOrdersByUser_shouldReturnUserOrder() {
        long userId = 1L;
        user.setId(userId);

        Order order1 = new Order();
        order1.setId(1L);
        order1.setUser(user);
        order1.setPrice(10.0);
        order1.setStatus(Status.NEW);
        order1.setOrderDate(LocalDateTime.now());
        order1.setAddress("Gyumri");

        Order order2 = new Order();
        order2.setId(2L);
        order2.setUser(user);
        order2.setPrice(12.0);
        order2.setStatus(Status.DELIVERED);
        order2.setOrderDate(LocalDateTime.now());
        order2.setAddress("Yerevan");

        List<Order> listOrders = List.of(order1, order2);

        when(orderRepository.findAllByUserIdOrderByOrderDateDesc(userId))
                .thenReturn(listOrders);

        List<Order> result = orderServiceImpl.getOrdersByUser(userId, "orderDate");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(order1.getId(), result.get(0).getId());
        assertEquals(order2.getId(), result.get(1).getId());

        verify(orderRepository).findAllByUserIdOrderByOrderDateDesc(userId);

    }


    @Test
    void getOrdersByUser_shouldReturnSortedOrdersByPrice() {
        long userId = 1L;
        user.setId(userId);

        Order order1 = new Order();
        order1.setId(1L);
        order1.setUser(user);
        order1.setPrice(10.0);
        order1.setStatus(Status.NEW);
        order1.setOrderDate(LocalDateTime.now());
        order1.setAddress("Gyumri");

        Order order2 = new Order();
        order2.setId(2L);
        order2.setUser(user);
        order2.setPrice(12.0);
        order2.setStatus(Status.DELIVERED);
        order2.setOrderDate(LocalDateTime.now());
        order2.setAddress("Yerevan");

        List<Order> listOrders = List.of(order1, order2);


        when(orderRepository.findAllByUserIdOrderByPriceAsc(userId)).thenReturn(listOrders);

        List<Order> result = orderServiceImpl.getOrdersByUser(userId, "price");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(order1.getId(), result.get(0).getId());
        assertEquals(order2.getId(), result.get(1).getId());


        verify(orderRepository).findAllByUserIdOrderByPriceAsc(userId);
    }

    @Test
    void getOrdersByUser_shouldReturnSortedOrdersByStatus() {
        long userId = 1L;
        user.setId(userId);

        Order order1 = new Order();
        order1.setId(1L);
        order1.setUser(user);
        order1.setPrice(10.0);
        order1.setStatus(Status.NEW);
        order1.setOrderDate(LocalDateTime.now());
        order1.setAddress("Gyumri");

        Order order2 = new Order();
        order2.setId(2L);
        order2.setUser(user);
        order2.setPrice(12.0);
        order2.setStatus(Status.DELIVERED);
        order2.setOrderDate(LocalDateTime.now());
        order2.setAddress("Yerevan");

        List<Order> listOrders = List.of(order1, order2);

        when(orderRepository.findAllByUserIdOrderByStatusAsc(userId)).thenReturn(listOrders);

        List<Order> result = orderServiceImpl.getOrdersByUser(userId, "status");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(order1.getId(), result.get(0).getId());
        assertEquals(order2.getId(), result.get(1).getId());

        verify(orderRepository).findAllByUserIdOrderByStatusAsc(userId);
    }

    @Test
    void getOrdersByUser_shouldReturnSortedOrdersByOrderDate() {
        long userId = 1L;
        user.setId(userId);

        Order order1 = new Order();
        order1.setId(1L);
        order1.setUser(user);
        order1.setPrice(10.0);
        order1.setStatus(Status.NEW);
        order1.setOrderDate(LocalDateTime.now().minusDays(1));
        order1.setAddress("Gyumri");

        Order order2 = new Order();
        order2.setId(2L);
        order2.setUser(user);
        order2.setPrice(12.0);
        order2.setStatus(Status.DELIVERED);
        order2.setOrderDate(LocalDateTime.now());
        order2.setAddress("Yerevan");

        List<Order> listOrders = List.of(order2, order1);

        when(orderRepository.findAllByUserIdOrderByOrderDateDesc(userId)).thenReturn(listOrders);

        List<Order> result = orderServiceImpl.getOrdersByUser(userId, "orderDate");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(order1.getId(), result.get(1).getId());
        assertEquals(order2.getId(), result.get(0).getId());

        verify(orderRepository).findAllByUserIdOrderByOrderDateDesc(userId);
    }

    @Test
    void updateOrder_shouldUpdateOrder_forUser() {
        long orderId = 1L;

        Product product = new Product();
        product.setId(10L);
        product.setPrice(100);

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setUser(user);
        existingOrder.setProduct(product);
        existingOrder.setStatus(Status.NEW);
        existingOrder.setAddress("Old Address");
        existingOrder.setQuantity(1);
        existingOrder.setPrice(100);
        existingOrder.setOrderDate(LocalDateTime.now().minusMinutes(5));

        SaveOrderRequest request = new SaveOrderRequest();
        request.setAddress("New Address");
        request.setQuantity(3);

        Order updatedOrder = new Order();
        updatedOrder.setId(orderId);
        updatedOrder.setUser(user);
        updatedOrder.setProduct(product);
        updatedOrder.setStatus(Status.NEW);
        updatedOrder.setAddress(request.getAddress());
        updatedOrder.setQuantity(request.getQuantity());
        updatedOrder.setPrice(product.getPrice() * request.getQuantity());
        updatedOrder.setOrderDate(existingOrder.getOrderDate());

        OrderDto dto = new OrderDto();
        dto.setId(orderId);
        dto.setUserId(user.getId());
        dto.setStatus(Status.NEW);
        dto.setAddress(request.getAddress());
        dto.setQuantity(request.getQuantity());

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(existingOrder));

        when(orderRepository.save(any(Order.class)))
                .thenReturn(updatedOrder);

        when(orderMapper.toDto(updatedOrder))
                .thenReturn(dto);

        OrderDto result = orderServiceImpl.update(orderId, request, user);

        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toDto(updatedOrder);

        assertEquals("New Address", result.getAddress());
        assertEquals(3, result.getQuantity());
        assertEquals(Status.NEW, result.getStatus());
    }

    @Test
    void updateOrder_shouldUpdateOrder_forAdminWithin10Minutes() {
        long orderId = 2L;

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setUser(user);
        existingOrder.setStatus(Status.NEW);
        existingOrder.setOrderDate(LocalDateTime.now().minusMinutes(5));

        SaveOrderRequest request = new SaveOrderRequest();

        Order updatedOrder = new Order();
        updatedOrder.setId(orderId);
        updatedOrder.setUser(user);
        updatedOrder.setStatus(Status.CANCELLED);
        updatedOrder.setOrderDate(existingOrder.getOrderDate());

        OrderDto dto = new OrderDto();
        dto.setId(orderId);
        dto.setUserId(user.getId());
        dto.setStatus(Status.CANCELLED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(orderMapper.toDto(updatedOrder)).thenReturn(dto);

        OrderDto result = orderServiceImpl.update(orderId, request, admin);

        assertEquals(Status.CANCELLED, result.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowException_whenOrderNotFound() {
        long orderId = 3L;

        SaveOrderRequest request = new SaveOrderRequest();
        request.setAddress("New Address");
        request.setQuantity(2);

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderServiceImpl.update(orderId, request, user)
        );

        assertEquals("Order not found with id " + orderId, exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowException_forUserIfStatusNotNew() {
        long orderId = 4L;

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setUser(user);
        existingOrder.setStatus(Status.DELIVERED);

        SaveOrderRequest request = new SaveOrderRequest();
        request.setAddress("New Address");
        request.setQuantity(2);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowException_forAdminIfMoreThan10Minutes() {
        long orderId = 5L;

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setUser(user);
        existingOrder.setStatus(Status.NEW);
        existingOrder.setOrderDate(LocalDateTime.now().minusMinutes(15));


        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        verify(orderRepository, never()).save(any(Order.class));
    }
}