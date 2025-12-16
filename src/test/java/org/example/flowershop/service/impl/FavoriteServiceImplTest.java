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

        Favorite favorite = Favorite.builder()
                .user(user)
                .product(product)
                .build();

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

        when(favoriteMapper.toEntity(request))
                .thenReturn(favorite);

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
        verify(favoriteMapper).toEntity(request);
        verify(favoriteRepository).save(any(Favorite.class));
        verify(favoriteMapper).toDto(savedFavorite);
    }


    @Test
    void addToFavorites_shouldThrowExceptionIfUserNotFound() {
        long userId = 1L;

        SaveFavoriteRequest request = new SaveFavoriteRequest();
        request.setProductId(2);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> favoriteServiceImpl.addToFavorites(userId, request));
        assertEquals("User not found", exception.getMessage());
    }


    @Test
    void addToFavorites_shouldThrowExceptionIfProductNotFound() {
        long userId = 1L;

        SaveFavoriteRequest request = new SaveFavoriteRequest();
        request.setProductId(2);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(new User()));
        when(productRepository.findById(5L))
                .thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> favoriteServiceImpl.addToFavorites(userId, request));

        assertEquals("Product not found", exception.getMessage());
    }


    @Test
    void getFavorites_shouldReturnSortedFavorites() {
        long userId = 1L;
        String sortBy = "productName";

        User user = new User();
        user.setId(userId);

        Product product1 = new Product();
        product1.setId(5L);
        product1.setName("Rose");

        Product product2 = new Product();
        product2.setId(6L);
        product2.setName("Tulip");

        Favorite favorite1 = new Favorite();
        favorite1.setId(1L);
        favorite1.setUser(user);
        favorite1.setProduct(product1);

        Favorite favorite2 = new Favorite();
        favorite2.setId(2L);
        favorite2.setUser(user);
        favorite2.setProduct(product2);

        List<Favorite> expectedFavorites = List.of(favorite1, favorite2);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(favoriteRepository.findAllByUserId(userId, Sort.by(sortBy).ascending()))
                .thenReturn(expectedFavorites);

        List<Favorite> result = favoriteServiceImpl.getFavorites(userId, sortBy);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Rose", result.get(0).getProduct().getName());
    }


    @Test
    void getFavorites_userNotFound_shouldThrowException() {
        long userId = 1L;
        String sortBy = "productName";

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> {
            favoriteServiceImpl.getFavorites(userId, sortBy);
        });
    }

    @Test
    void removeFromFavorites_shouldRemove() {
        long userId = 1L;

        User user = new User();
        user.setId(userId);


        Favorite existing = new Favorite();
        existing.setId(10);
        existing.setUser(user);

        when(favoriteRepository.findById(10L)).thenReturn(Optional.of(existing));

        favoriteServiceImpl.remove(userId, 10L);

        verify(favoriteRepository).delete(existing);
    }
}