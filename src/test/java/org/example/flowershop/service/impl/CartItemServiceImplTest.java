package org.example.flowershop.service.impl;

import org.example.flowershop.dto.CartDto;
import org.example.flowershop.dto.SaveCartItemRequest;
import org.example.flowershop.exception.CartItemNotFoundException;
import org.example.flowershop.exception.ProductNotFoundException;
import org.example.flowershop.mapper.CartItemMapper;
import org.example.flowershop.model.entity.CartItem;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.repository.CartItemRepository;
import org.example.flowershop.repository.ProductRepository;
import org.example.flowershop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    void getCartByUser_shouldReturnCartItems_sortedByProductName_default() {
        long userId = 1L;

        CartItem item1 = new CartItem();
        item1.setId(1L);
        CartItem item2 = new CartItem();
        item2.setId(2L);

        when(cartItemRepository.findAllByUserId(eq(userId), any(Sort.class)))
                .thenReturn(List.of(item1, item2));

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        List<CartItem> result = cartItemServiceImpl.getCartByUser(userId, "invalid");

        assertEquals(2, result.size());

        verify(cartItemRepository)
                .findAllByUserId(eq(userId), sortCaptor.capture());

        Sort usedSort = sortCaptor.getValue();
        assertTrue(usedSort.isSorted());
        assertEquals(Sort.by("product.name").ascending(), usedSort);
    }

    @Test
    void getCartByUser_shouldSortByPrice() {
        long userId = 1L;

        when(cartItemRepository.findAllByUserId(eq(userId), any(Sort.class)))
                .thenReturn(List.of());

        cartItemServiceImpl.getCartByUser(userId, "price");

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        verify(cartItemRepository).findAllByUserId(eq(userId), sortCaptor.capture());

        Sort usedSort = sortCaptor.getValue();

        assertTrue(usedSort.isSorted());
        assertEquals(
                Sort.by("product.price").ascending(),
                usedSort
        );
    }


    @Test
    void getCartByUser_shouldSortByCreatedAt() {
        long userId = 1L;

        when(cartItemRepository.findAllByUserId(eq(userId), any(Sort.class)))
                .thenReturn(List.of());

        cartItemServiceImpl.getCartByUser(userId, "createdAt");

        verify(cartItemRepository).findAllByUserId(
                eq(userId),
                eq(Sort.by("createdAt").descending())
        );
    }

    @Test
    void addToCart_shouldCreateNewCartItem() {
        long userId = 1L;
        long productId = 5L;

        SaveCartItemRequest request = new SaveCartItemRequest();
        request.setProductId(productId);

        User user = new User();
        user.setId(userId);

        Product product = new Product();
        product.setId(productId);

        CartItem savedCartItem = CartItem.builder()
                .id(10L)
                .user(user)
                .product(product)
                .build();

        CartDto cartDto = new CartDto();
        cartDto.setId(10L);
        cartDto.setProductId(productId);

        when(userRepository.getReferenceById(userId))
                .thenReturn(user);

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.save(any(CartItem.class)))
                .thenReturn(savedCartItem);

        when(cartItemMapper.toDto(savedCartItem))
                .thenReturn(cartDto);

        CartDto result = cartItemServiceImpl.addToCart(userId, request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(productId, result.getProductId());

        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartItemMapper).toDto(savedCartItem);
    }

    @Test
    void addToCart_shouldThrowException_whenProductNotFound() {
        long userId = 1L;

        SaveCartItemRequest request = new SaveCartItemRequest();
        request.setProductId(99L);

        when(userRepository.getReferenceById(anyLong()))
                .thenReturn(new User());

        when(productRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        ProductNotFoundException ex = assertThrows(
                ProductNotFoundException.class,
                () -> cartItemServiceImpl.addToCart(userId, request)
        );

        assertEquals("Product not found", ex.getMessage());

        verify(cartItemRepository, never()).save(any());
        verify(cartItemMapper, never()).toDto(any());
    }

    @Test
    void remove_shouldDeleteCartItem_whenExistsAndOwnedByUser() {
        long userId = 1L;
        long cartItemId = 10L;

        CartItem cartItem = new CartItem();
        cartItem.setId(cartItemId);

        when(cartItemRepository.findByIdAndUserId(cartItemId, userId))
                .thenReturn(Optional.of(cartItem));

        cartItemServiceImpl.remove(userId, cartItemId);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void remove_shouldThrowException_whenCartItemNotFound() {
        long userId = 1L;
        long cartItemId = 10L;

        when(cartItemRepository.findByIdAndUserId(cartItemId, userId))
                .thenReturn(Optional.empty());

        CartItemNotFoundException ex = assertThrows(
                CartItemNotFoundException.class,
                () -> cartItemServiceImpl.remove(userId, cartItemId)
        );

        assertEquals("Cart item not found", ex.getMessage());
        verify(cartItemRepository, never()).delete(any());
    }
}