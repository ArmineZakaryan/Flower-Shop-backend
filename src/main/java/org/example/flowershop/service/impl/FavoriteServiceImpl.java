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
    @Transactional(readOnly = true)
    public List<FavoriteDto> getFavorites(long userId, String sortBy) {
        log.info("Find favorites by userId: {} and sortBy: {}", userId, sortBy);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found");
        }

        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "productName" : sortBy;

        String sortField = switch (safeSortBy) {
            case "productName", "product.name" -> "product.name";
            case "price", "product.price" -> "product.price";
            default -> "product.name";
        };

        Sort sort = Sort.by(sortField).ascending();

        return favoriteRepository.findAllByUserId(userId, sort)
                .stream()
                .map(favoriteMapper::toDto)
                .toList();
    }


    @Override
    public FavoriteDto addToFavorites(long userId, SaveFavoriteRequest request) {
        log.info("Attempting to add product to favorites for userId: {}", userId);

        if (request == null || request.getProductId() <= 0) {
            log.warn("Invalid favorite request for userId {}: {}", userId, request);
            throw new IllegalArgumentException("Request body invalid or missing");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        log.info("Attempting to find product with id: {}", request.getProductId());
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        log.info("Mapping SaveFavoriteRequest to Favorite entity for userId: {} and productId: {}", userId, request.getProductId());
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProduct(product);

        log.info("Saving favorite for userId: {} and productId: {}", userId, request.getProductId());
        Favorite saved = favoriteRepository.save(favorite);

        log.info("Successfully added favorite for userId: {} and productId: {}", userId, request.getProductId());
        return favoriteMapper.toDto(saved);

    }

    @Override
    public void remove(long userId, Long id) {
        log.info("Attempting to remove product from favorites for userId: {}, favoriteId: {}", userId, id);

        if (id == null) {
            log.warn("Favorite id is null for userId {}", userId);
            throw new IllegalArgumentException("Favorite id must not be null");
        }

        Favorite favorite = favoriteRepository.findById(id)
                .orElseThrow(() ->
                        new FavoriteNotFoundException("Favorite item not found with id " + id));

        if (!favorite.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to delete favorite {} owned by user {}",
                    userId, id, favorite.getUser().getId());
            throw new AccessDeniedException("You cannot delete another user's favorite item");
        }

        favoriteRepository.delete(favorite);
        log.info("Successfully deleted favorite with id {} for user {}", id, userId);
    }
}