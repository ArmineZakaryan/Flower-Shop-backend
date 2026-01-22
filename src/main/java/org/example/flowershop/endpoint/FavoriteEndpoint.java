package org.example.flowershop.endpoint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.FavoriteDto;
import org.example.flowershop.dto.SaveFavoriteRequest;
import org.example.flowershop.mapper.FavoriteMapper;
import org.example.flowershop.security.CurrentUser;
import org.example.flowershop.service.FavoriteService;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/favorites")
@Slf4j
public class FavoriteEndpoint {
    private final FavoriteService favoriteService;
    private final FavoriteMapper favoriteMapper;

    @GetMapping
    public List<FavoriteDto> getMyFavorites(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String sortBy) {

        return favoriteService.getFavorites(
                currentUser.getUser().getId(),
                sortBy
        );
    }

    @PostMapping
    public ResponseEntity<FavoriteDto> createFavorite(
            @Valid @RequestBody SaveFavoriteRequest request,
            @AuthenticationPrincipal CurrentUser currentUser,
            UriComponentsBuilder uriBuilder) {

        FavoriteDto favoriteDto =
                favoriteService.addToFavorites(currentUser.getUser().getId(), request);

        var uri = uriBuilder.path("/favorites/{id}")
                .buildAndExpand(favoriteDto.getId())
                .toUri();

        return ResponseEntity.created(uri).body(favoriteDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {

        favoriteService.remove(currentUser.getUser().getId(), id);
        return ResponseEntity.noContent().build();
    }
}