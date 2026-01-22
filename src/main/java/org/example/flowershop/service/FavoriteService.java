package org.example.flowershop.service;

import org.example.flowershop.dto.FavoriteDto;
import org.example.flowershop.dto.SaveFavoriteRequest;

import java.util.List;

public interface FavoriteService {

    List<FavoriteDto> getFavorites(long userId, String sortBy);

    FavoriteDto addToFavorites(long userId, SaveFavoriteRequest request);

    void remove(long userId, Long id);

}