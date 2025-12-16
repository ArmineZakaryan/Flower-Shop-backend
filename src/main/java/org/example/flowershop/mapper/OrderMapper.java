package org.example.flowershop.mapper;

import org.example.flowershop.dto.OrderDto;
import org.example.flowershop.dto.ProductDto;
import org.example.flowershop.dto.SaveOrderRequest;
import org.example.flowershop.model.entity.Order;
import org.example.flowershop.model.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "user.id", target = "userId")
    OrderDto toDto(Order order);

    List<OrderDto> toDtoList(List<Order> orders);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "orderDate", ignore = true)
    Order toEntity(SaveOrderRequest orderRequest);

    ProductDto toProductDto(Product product);
}
