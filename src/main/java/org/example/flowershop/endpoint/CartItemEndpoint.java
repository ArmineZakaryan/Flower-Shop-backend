package org.example.flowershop.endpoint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.CartDto;
import org.example.flowershop.dto.SaveCartItemRequest;
import org.example.flowershop.mapper.CartItemMapper;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.service.CartItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cart-items")
@Slf4j
public class CartItemEndpoint {

    private final CartItemService cartItemService;
    private final CartItemMapper cartItemMapper;

    @GetMapping
    public ResponseEntity<List<CartDto>> getUserCartItems(
            @RequestParam(defaultValue = "productName") String sortBy,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("Fetching cart items for userId: {} ", currentUser.getId());

        List<CartDto> cartItems = cartItemService
                .getCartByUser(currentUser.getId(), sortBy)
                .stream()
                .map(cartItemMapper::toDto)
                .toList();
        return ResponseEntity.ok(cartItems);
    }

    @PostMapping
    public ResponseEntity<CartDto> addToCart(
            @Valid @RequestBody SaveCartItemRequest request,
            @AuthenticationPrincipal(expression = "user") User currentUser,
            UriComponentsBuilder uriBuilder
    ) {
        CartDto cartDto = cartItemService.addToCart(currentUser.getId(), request);

        var uri = uriBuilder
                .path("/cart-items/{id}")
                .buildAndExpand(cartDto.getId())
                .toUri();

        return ResponseEntity.created(uri).body(cartDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("User {} deleting cart-item {}", currentUser.getId(), id);

        cartItemService.remove(currentUser.getId(), id);

        log.info("cart item {} deleted for user={}", id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}