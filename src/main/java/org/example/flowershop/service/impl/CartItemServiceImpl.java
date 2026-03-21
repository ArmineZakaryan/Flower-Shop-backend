package org.example.flowershop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.example.flowershop.service.CartItemService;
import org.springframework.data.domain.Sort;
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
        log.info("Finding cart items for userId: {} with sorting by: {}", userId, sortBy);

        Sort sort = resolveSort(sortBy);

        return cartItemRepository.findAllByUserId(userId, sort);
    }

    private Sort resolveSort(String sortBy) {
        return switch (sortBy) {
            case "productName" -> Sort.by("product.name").ascending();
            case "price" -> Sort.by("product.price").ascending();
            case "createdAt" -> Sort.by("createdAt").descending();
            default -> {
                log.warn("Invalid sortBy '{}', using default 'productName'", sortBy);
                yield Sort.by("product.name").ascending();
            }
        };
    }


    @Override
    public CartDto addToCart(long userId, SaveCartItemRequest request) {

        log.info("Request to add product={} " +
                        "to cart for user={}",
                request.getProductId(),
                userId);

        User user = userRepository.getReferenceById(userId);

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

        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> {
                    log.warn("Cart item {} not found or not owned by user={}", cartItemId, userId);
                    return new CartItemNotFoundException("Cart item not found");
                });

        cartItemRepository.delete(cartItem);
        log.info("Removed cart item = {} for user={}", cartItemId, userId);
    }
}