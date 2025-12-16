package org.example.flowershop.endpoint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.CartDto;
import org.example.flowershop.dto.SaveCartItemRequest;
import org.example.flowershop.mapper.CartItemMapper;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.service.impl.CartItemServiceImpl;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cartItem")
@Slf4j
public class CartItemEndpoint {

    private final CartItemServiceImpl cartItemServiceImpl;
    private final CartItemMapper cartItemMapper;

    @GetMapping
    public ResponseEntity<List<CartDto>> getUserCartItems(
            @RequestParam(defaultValue = "productName") String sortBy,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        if (currentUser == null) {
            log.warn("User is unauthorized");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        log.info("Fetching cart items for userId: {} sorted by: {}", currentUser.getId(), sortBy);

        if (!"productName".equals(sortBy)) {
            log.warn("Invalid sortBy value '{}'. Using default sort by 'productName'.", sortBy);
            sortBy = "productName";
        }

        List<CartDto> myCartItems = cartItemServiceImpl.getCartByUser(currentUser.getId(), sortBy)
                .stream()
                .map(cartItemMapper::toDto)
                .collect(Collectors.toList());

        log.info("Returning {} cart items for userId: {}", myCartItems.size(), currentUser.getId());

        return ResponseEntity.ok(myCartItems);
    }

    @PostMapping
    public ResponseEntity<CartDto> addToCart(
            @Valid @RequestBody SaveCartItemRequest request,
            @AuthenticationPrincipal(expression = "user") User currentUser,
            UriComponentsBuilder uriBuilder
    ) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        CartDto cartDto = cartItemServiceImpl.addToCart(currentUser.getId(), request);

        var uri = uriBuilder
                .path("/cartItem/{id}")
                .buildAndExpand(cartDto.getId())
                .toUri();

        return ResponseEntity.created(uri).body(cartDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        if (currentUser == null) {

            log.warn("User is unauthorized", id);

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        log.info("User {} deleting cartItem {}", id, currentUser.getId());

        cartItemServiceImpl.remove(currentUser.getId(), id);
        log.info("CartItem {} deleted for user={}", id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}