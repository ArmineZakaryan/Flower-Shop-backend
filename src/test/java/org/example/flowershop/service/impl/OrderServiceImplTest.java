package org.example.flowershop.service.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    void findAll_shouldReturnPage() {
        Order order = new Order();
        OrderDto dto = new OrderDto();

        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(orderMapper.toDto(order)).thenReturn(dto);

        Page<OrderDto> result =
                orderServiceImpl.findAll(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        verify(orderRepository).findAll(any(PageRequest.class));
    }


    @Test
    void findByIdForUser_adminAccess_shouldReturnOrder() {
        User admin = new User();
        admin.setId(1L);
        admin.setUserType(UserType.ADMIN);

        User orderOwner = new User();
        orderOwner.setId(2L);

        Order order = new Order();
        order.setId(1L);
        order.setUser(orderOwner);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(new OrderDto());

        OrderDto result = orderServiceImpl.findByIdForUser(1L, admin);

        assertNotNull(result);
        verify(orderRepository).findById(1L);
        verify(orderMapper).toDto(order);
    }


    @Test
    void findByIdForUser_notOwner_shouldThrowAccessDenied() {
        User user = new User();
        user.setId(1L);
        user.setUserType(UserType.USER);

        User owner = new User();
        owner.setId(2L);

        Order order = new Order();
        order.setUser(owner);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(
                AccessDeniedException.class,
                () -> orderServiceImpl.findByIdForUser(1L, user)
        );
    }


    @Test
    void save_shouldCreateOrder() {
        User user = new User();
        user.setId(1L);

        Product product = new Product();
        product.setId(2L);
        product.setPrice(10);

        SaveOrderRequest request = new SaveOrderRequest();
        request.setProductId(2L);
        request.setQuantity(2);

        Order order = new Order();
        Order saved = new Order();
        OrderDto dto = new OrderDto();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(orderMapper.toEntity(request)).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderMapper.toDto(saved)).thenReturn(dto);

        OrderDto result = orderServiceImpl.save(request, 1L);

        assertNotNull(result);
    }

    @Test
    void save_userNotFound_shouldThrowException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> orderServiceImpl.save(new SaveOrderRequest(), 1L)
        );
    }

    @Test
    void save_productNotFound_shouldThrowException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        SaveOrderRequest request = new SaveOrderRequest();
        request.setProductId(99L);

        assertThrows(
                ProductNotFoundException.class,
                () -> orderServiceImpl.save(request, 1L)
        );
    }


    @Test
    void getOrdersByUser_sortByPrice() {
        when(orderRepository.findAllByUserIdOrderByPriceAsc(1L))
                .thenReturn(List.of(new Order()));
        when(orderMapper.toDtoList(any()))
                .thenReturn(List.of(new OrderDto()));

        List<OrderDto> result =
                orderServiceImpl.getOrdersByUser(1L, "price");

        assertEquals(1, result.size());
    }


    @Test
    void update_adminCancelNewOrder_shouldCancel() {
        User admin = new User();
        admin.setUserType(UserType.ADMIN);

        Order order = new Order();
        order.setStatus(Status.NEW);
        order.setOrderDate(LocalDateTime.now());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toDto(order)).thenReturn(new OrderDto());

        OrderDto result =
                orderServiceImpl.update(1L, new SaveOrderRequest(), admin);

        assertNotNull(result);
        assertEquals(Status.CANCELLED, order.getStatus());
    }

    @Test
    void update_userWrongOwner_shouldThrowAccessDenied() {
        User user = new User();
        user.setId(1L);
        user.setUserType(UserType.USER);

        User owner = new User();
        owner.setId(2L);

        Order order = new Order();
        order.setId(1L);
        order.setUser(owner);
        order.setStatus(Status.NEW);
        order.setOrderDate(LocalDateTime.now().minusMinutes(1));

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThrows(
                AccessDeniedException.class,
                () -> orderServiceImpl.update(1L, new SaveOrderRequest(), user)
        );

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void update_userAfter10Minutes_shouldThrowForbidden() {
        User user = new User();
        user.setId(1L);
        user.setUserType(UserType.USER);

        Order order = new Order();
        order.setUser(user);
        order.setStatus(Status.NEW);
        order.setOrderDate(LocalDateTime.now().minusMinutes(20));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(
                ResponseStatusException.class,
                () -> orderServiceImpl.update(1L, new SaveOrderRequest(), user)
        );
    }

    @Test
    void update_notFound_shouldThrowException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderServiceImpl.update(1L, new SaveOrderRequest(), new User())
        );
    }
}