package org.example.flowershop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.CartDto;
import org.example.flowershop.dto.SaveCartItemRequest;
import org.example.flowershop.exception.CartItemNotFoundException;
import org.example.flowershop.exception.ProductNotFoundException;
import org.example.flowershop.exception.UserNotFoundException;
import org.example.flowershop.mapper.CartItemMapper;
import org.example.flowershop.model.entity.CartItem;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.repository.CartItemRepository;
import org.example.flowershop.repository.ProductRepository;
import org.example.flowershop.repository.UserRepository;
import org.example.flowershop.service.CartItemService;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartItemServiceImpl implements CartItemService {
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemMapper cartItemMapper;


    @Override
    public List<CartItem> getCartByUser(long userId, String sortBy) {
        log.info("Finding cartItems for userId: {} with sorting by: {}", userId, sortBy);

        if (!"productName".equals(sortBy)) {
            log.warn("Invalid sortBy value '{}'. Using default sort by 'productName'.", sortBy);
        }

        Sort sorting = Sort.by("product.name").ascending();

        return cartItemRepository.findAllByUserId(userId, sorting);
    }


    @Override
    public CartDto addToCart(long userId, SaveCartItemRequest request) {

        log.info("Request to add product={} " +
                        "to cart for user={}",
                request.getProductId(),
                userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        CartItem cartItem = CartItem.builder()
                .user(user)
                .product(product)
                .build();

        CartItem saved = cartItemRepository.save(cartItem);
        log.info("New cartItem created id={} for user={}", saved.getId(), user.getId());
        return cartItemMapper.toDto(saved);
    }

    @Override
    public void remove(long userId, long cartItemId) {

        log.info("Request to remove cartItem={} for user={}", cartItemId, userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> {
                    log.error("CartItem {} not found for user={}", cartItemId, userId);
                    return new CartItemNotFoundException("Cart item not found");
                });

        if (cartItem.getUser().getId() != userId) {
            log.error("User {} attempted to delete cartItem {} belonging to another user", userId, cartItemId);
            throw new AccessDeniedException("You cannot delete another user's cart item");

        }
        cartItemRepository.delete(cartItem);
        log.info("Removed cartItem={} for user={}", cartItemId, userId);
    }
}