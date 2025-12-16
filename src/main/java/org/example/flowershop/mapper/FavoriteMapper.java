package org.example.flowershop.mapper;

import org.example.flowershop.dto.FavoriteDto;
import org.example.flowershop.dto.SaveFavoriteRequest;
import org.example.flowershop.model.entity.Favorite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FavoriteMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.price", target = "productPrice")
    @Mapping(source = "product.description", target = "productDescription")
    @Mapping(source = "product.image", target = "productImage")
    FavoriteDto toDto(Favorite favorite);


    Favorite toEntity(SaveFavoriteRequest favoriteRequest);

}