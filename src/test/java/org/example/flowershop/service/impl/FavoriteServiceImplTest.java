package org.example.flowershop.service.impl;

import org.example.flowershop.dto.FavoriteDto;
import org.example.flowershop.dto.SaveFavoriteRequest;
import org.example.flowershop.exception.ProductNotFoundException;
import org.example.flowershop.exception.UserNotFoundException;
import org.example.flowershop.mapper.FavoriteMapper;
import org.example.flowershop.model.entity.Favorite;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.repository.FavoriteRepository;
import org.example.flowershop.repository.ProductRepository;
import org.example.flowershop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FavoriteServiceImplTest {

    @InjectMocks
    private FavoriteServiceImpl favoriteServiceImpl;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addToFavorites_shouldAdd() {
        long userId = 1L;

        User user = new User();
        user.setId(userId);

        Product product = new Product();
        product.setId(5L);

        SaveFavoriteRequest request = new SaveFavoriteRequest();
        request.setProductId(5L);

        Favorite savedFavorite = Favorite.builder()
                .id(100L)
                .user(user)
                .product(product)
                .build();

        FavoriteDto expectedDto = new FavoriteDto();
        expectedDto.setId(100L);
        expectedDto.setUserId(userId);
        expectedDto.setProductId(5L);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(productRepository.findById(5L))
                .thenReturn(Optional.of(product));
        when(favoriteRepository.save(any(Favorite.class)))
                .thenReturn(savedFavorite);
        when(favoriteMapper.toDto(savedFavorite))
                .thenReturn(expectedDto);

        FavoriteDto result = favoriteServiceImpl.addToFavorites(userId, request);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(5L, result.getProductId());

        verify(userRepository).findById(userId);
        verify(productRepository).findById(5L);
        verify(favoriteRepository).save(any(Favorite.class));
        verify(favoriteMapper).toDto(savedFavorite);
    }

    @Test
    void addToFavorites_shouldThrowExceptionIfUserNotFound() {
        long userId = 1L;

        SaveFavoriteRequest request = new SaveFavoriteRequest();
        request.setProductId(2L);

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> favoriteServiceImpl.addToFavorites(userId, request)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void addToFavorites_shouldThrowExceptionIfProductNotFound() {
        long userId = 1L;

        SaveFavoriteRequest request = new SaveFavoriteRequest();
        request.setProductId(2L);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(new User()));
        when(productRepository.findById(2L))
                .thenReturn(Optional.empty());

        ProductNotFoundException ex = assertThrows(
                ProductNotFoundException.class,
                () -> favoriteServiceImpl.addToFavorites(userId, request)
        );

        assertEquals("Product not found", ex.getMessage());
    }


    @Test
    void getFavorites_shouldReturnSortedFavorites() {
        long userId = 1L;
        String sortBy = "product.name";

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(favoriteRepository.findAllByUserId(userId, Sort.by(sortBy).ascending()))
                .thenReturn(List.of(new Favorite(), new Favorite()));

        List<FavoriteDto> result = favoriteServiceImpl.getFavorites(userId, sortBy);

        assertEquals(2, result.size());
    }

    @Test
    void getFavorites_userNotFound_shouldThrowException() {
        when(userRepository.existsById(1L))
                .thenReturn(false);

        assertThrows(
                UserNotFoundException.class,
                () -> favoriteServiceImpl.getFavorites(1L, "product.name")
        );
    }

    @Test
    void remove_shouldDelete_whenOwner() {
        long userId = 1L;

        User user = new User();
        user.setId(userId);

        Favorite favorite = new Favorite();
        favorite.setId(10L);
        favorite.setUser(user);

        when(favoriteRepository.findById(10L))
                .thenReturn(Optional.of(favorite));

        favoriteServiceImpl.remove(userId, 10L);

        verify(favoriteRepository).delete(favorite);
    }

    @Test
    void remove_shouldThrowAccessDenied_whenNotOwner() {
        User owner = new User();
        owner.setId(2L);

        Favorite favorite = new Favorite();
        favorite.setId(10L);
        favorite.setUser(owner);

        when(favoriteRepository.findById(10L))
                .thenReturn(Optional.of(favorite));

        assertThrows(
                AccessDeniedException.class,
                () -> favoriteServiceImpl.remove(1L, 10L)
        );
    }
}