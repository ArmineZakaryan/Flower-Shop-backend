package org.example.flowershop.endpoint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.FavoriteDto;
import org.example.flowershop.dto.SaveFavoriteRequest;
import org.example.flowershop.mapper.FavoriteMapper;
import org.example.flowershop.model.entity.Favorite;
import org.example.flowershop.security.CurrentUser;
import org.example.flowershop.service.impl.FavoriteServiceImpl;
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
@RequestMapping("/favorites")
@Slf4j
public class FavoriteEndpoint {
    private final FavoriteServiceImpl favoriteServiceImpl;
    private final FavoriteMapper favoriteMapper;


    @GetMapping("/{id}")
    public List<FavoriteDto> getFavoritesByUser(
            @PathVariable long id,
            @RequestParam(required = false) String sortBy) {

        log.info("Fetching favorites for user with id: {} and sorting by: {}", id, sortBy);
        List<Favorite> favorites = favoriteServiceImpl.getFavorites(id, sortBy);

        log.info("Found {} favorites for userId: {}", favorites.size(), id);

        List<FavoriteDto> favoriteDto = favorites.stream()
                .map(favorite -> favoriteMapper.toDto(favorite))
                .collect(Collectors.toList());

        log.info("Returning {} FavoriteDto for userId: {}", favoriteDto.size(), id);

        return favoriteDto;
    }

    @PostMapping
    public ResponseEntity<FavoriteDto> createFavorite(
            @RequestBody(required = false) SaveFavoriteRequest request,
            @AuthenticationPrincipal CurrentUser currentUser,
            UriComponentsBuilder uriBuilder) {

        if (currentUser == null) {
            log.warn("User is not authenticated for creating a favorite.");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User is not authenticated"
            );
        }

        if (request == null || request.getProductId() <= 0) {
            log.warn("Invalid request body or productId. Request: {}", request);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request body is missing or invalid"
            );
        }

        log.info("Adding product with id: {} to the favorites for userId: {}", request.getProductId(), currentUser.getUser().getId());
        FavoriteDto favoriteDto =
                favoriteServiceImpl.addToFavorites(
                        currentUser.getUser().getId(),
                        request
                );

        log.info("Favorite successfully created for userId: {} with favoriteId: {}", currentUser.getUser().getId(), favoriteDto.getId());

        var uri = uriBuilder.path("/favorites/{id}")
                .buildAndExpand(favoriteDto.getId())
                .toUri();

        return ResponseEntity.created(uri).body(favoriteDto);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {

        if (currentUser == null) {
            log.warn("User is not authenticated to delete favorite with id: {}", id);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User is not authenticated"
            );
        }
        log.info("User with id: {} is attempting to delete favorite with id: {}", currentUser.getUser().getId(), id);

        favoriteServiceImpl.remove(currentUser.getUser().getId(), id);
        log.info("Successfully deleted favorite with id: {} for userId: {}", id, currentUser.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}