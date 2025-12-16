package org.example.flowershop.mapper;

import org.example.flowershop.dto.ProductDto;
import org.example.flowershop.dto.SaveProductRequest;
import org.example.flowershop.model.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface ProductMapper {

    @Mapping(target = "category", source = "category")
    ProductDto toDto(Product product);

    Product toEntity(SaveProductRequest productRequest);
}