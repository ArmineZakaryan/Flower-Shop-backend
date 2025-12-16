package org.example.flowershop.mapper;

import org.example.flowershop.dto.CartDto;
import org.example.flowershop.dto.SaveCartItemRequest;
import org.example.flowershop.model.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.price", target = "productPrice")
    @Mapping(source = "product.description", target = "productDescription")
    @Mapping(source = "product.image", target = "productImage")
    CartDto toDto(CartItem cartItem);

    CartItem toEntity(SaveCartItemRequest cartRequest);
}