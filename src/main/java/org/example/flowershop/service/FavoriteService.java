package org.example.flowershop.service;

import org.example.flowershop.dto.FavoriteDto;
import org.example.flowershop.dto.SaveFavoriteRequest;
import org.example.flowershop.model.entity.Favorite;

import java.util.List;

public interface FavoriteService {


    List<Favorite> getFavorites(long userId, String sortBy);


    FavoriteDto addToFavorites(long id, SaveFavoriteRequest request);

    void remove(long userId, Long id);

}