package org.example.flowershop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.FavoriteDto;
import org.example.flowershop.dto.SaveFavoriteRequest;
import org.example.flowershop.exception.FavoriteNotFoundException;
import org.example.flowershop.exception.ProductNotFoundException;
import org.example.flowershop.exception.UserNotFoundException;
import org.example.flowershop.mapper.FavoriteMapper;
import org.example.flowershop.model.entity.Favorite;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.repository.FavoriteRepository;
import org.example.flowershop.repository.ProductRepository;
import org.example.flowershop.repository.UserRepository;
import org.example.flowershop.service.FavoriteService;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final FavoriteMapper favoriteMapper;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;


    @Override
    public List<Favorite> getFavorites(long userId, String sortBy) {
        log.info("Find favorites by userId: {} and sortBy: {}", userId, sortBy);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id " + userId);
        }

        Sort sort = (sortBy != null) ? Sort.by(sortBy).ascending() : Sort.by("productName").ascending();

        List<Favorite> favorites = favoriteRepository.findAllByUserId(userId, sort);

        log.info("Successfully found {} favorites for userId: {} sorted by {}", favorites.size(), userId, sortBy);
        return favorites;
    }


    @Override
    public FavoriteDto addToFavorites(long id, SaveFavoriteRequest favoriteRequest) {
        log.info("Attempting to add product to favorites for userId: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        log.info("Attempting to find product with id: {}", favoriteRequest.getProductId());
        Product product = productRepository.findById(favoriteRequest.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        log.info("Mapping SaveFavoriteRequest to Favorite entity for userId: {} and productId: {}", id, favoriteRequest.getProductId());
        Favorite favorite = favoriteMapper.toEntity(favoriteRequest);
        favorite.setUser(user);
        favorite.setProduct(product);

        log.info("Saving favorite for userId: {} and productId: {}", id, favoriteRequest.getProductId());
        Favorite savedFavorite = favoriteRepository.save(favorite);

        log.info("Successfully added favorite for userId: {} and productId: {}", id, favoriteRequest.getProductId());
        return favoriteMapper.toDto(savedFavorite);

    }


    @Override
    public void remove(long userId, Long id) {
        log.info("User {} removed favorite with id {}", userId, id);

        Favorite favorite = favoriteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Favorite item not found with id {}", id);
                    return new FavoriteNotFoundException("Favorite item not found with " + id + " id");
                });

        if (favorite.getUser().getId() != userId) {
            log.warn("User {} cannot delete another user's favorite item with id {}", userId, id);
            throw new AccessDeniedException("You cannot delete another user's favorite item");
        }
        favoriteRepository.delete(favorite);
        log.info("Successfully deleted favorite with id {} for user {}", id, userId);
    }
}