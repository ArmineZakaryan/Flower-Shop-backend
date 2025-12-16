package org.example.flowershop.service.impl;

import org.example.flowershop.dto.CartDto;
import org.example.flowershop.dto.SaveCartItemRequest;
import org.example.flowershop.exception.ProductNotFoundException;
import org.example.flowershop.exception.UserNotFoundException;
import org.example.flowershop.mapper.CartItemMapper;
import org.example.flowershop.model.entity.CartItem;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.repository.CartItemRepository;
import org.example.flowershop.repository.ProductRepository;
import org.example.flowershop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class CartItemServiceImplTest {

    @InjectMocks
    private CartItemServiceImpl cartItemServiceImpl;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getCartByUser_shouldReturnCartByUser() {
        long userId = 1L;
        CartItem cartItem1 = new CartItem();
        cartItem1.setId(10L);

        CartItem cartItem2 = new CartItem();
        cartItem2.setId(20L);

        List<CartItem> mockList = List.of(cartItem1, cartItem2);
        when(cartItemRepository.findAllByUserId(eq(userId), any(Sort.class))).thenReturn(mockList);
        List<CartItem> result = cartItemServiceImpl.getCartByUser(userId, null);

        assertEquals(2, result.size());
        assertEquals(cartItem1.getId(), result.get(0).getId());
        assertEquals(cartItem2.getId(), result.get(1).getId());

        verify(cartItemRepository).findAllByUserId(eq(userId), any(Sort.class));
    }

    @Test
    void addToCart_throwExceptionIfUserNotFound() {
        long userId = 1L;
        SaveCartItemRequest request = new SaveCartItemRequest();
        request.setProductId(5L);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> cartItemServiceImpl.addToCart(userId, request));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void addToCart_throwExceptionIfProductNotFound() {
        long userId = 1L;
        SaveCartItemRequest request = new SaveCartItemRequest();
        request.setProductId(5);
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(new User()));

        when(productRepository.findById(5L))
                .thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> cartItemServiceImpl.addToCart(userId, request));

        assertEquals("Product not found", exception.getMessage());
    }

    @Test
    void addToCart_newCartItem() {
        long userId = 1L;

        User user = new User();
        user.setId(userId);

        Product product = new Product();
        product.setId(5L);

        SaveCartItemRequest request = new SaveCartItemRequest();
        request.setProductId(5L);

        CartItem savedCartItem = CartItem.builder()
                .id(10L)
                .user(user)
                .product(product)
                .build();

        CartDto cartDto = new CartDto();
        cartDto.setId(10L);
        cartDto.setProductId(5L);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(productRepository.findById(5L))
                .thenReturn(Optional.of(product));
        when(cartItemRepository.save(any(CartItem.class)))
                .thenReturn(savedCartItem);
        when(cartItemMapper.toDto(savedCartItem))
                .thenReturn(cartDto);

        CartDto result = cartItemServiceImpl.addToCart(userId, request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(5L, result.getProductId());

        verify(cartItemRepository, times(1))
                .save(any(CartItem.class));
    }

    @Test
    void remove_shouldRemoveCartItem() {
        long userId = 1L;

        User user = new User();
        user.setId(userId);


        CartItem existing = new CartItem();
        existing.setId(10);
        existing.setUser(user);


        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(existing));

        cartItemServiceImpl.remove(userId, 10L);

        verify(cartItemRepository).delete(existing);
    }
}
