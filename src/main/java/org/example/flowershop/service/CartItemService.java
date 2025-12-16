package org.example.flowershop.service;

import org.example.flowershop.dto.CartDto;
import org.example.flowershop.dto.SaveCartItemRequest;
import org.example.flowershop.model.entity.CartItem;

import java.util.List;

public interface CartItemService {
    List<CartItem> getCartByUser(long userId, String sort);

    CartDto addToCart(long userId, SaveCartItemRequest request);

    void remove(long userId, long cartItemId);
}
