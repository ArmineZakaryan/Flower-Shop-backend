package org.example.flowershop.endpoint;

import org.example.flowershop.dto.FavoriteDto;
import org.example.flowershop.dto.SaveFavoriteRequest;
import org.example.flowershop.mapper.FavoriteMapper;
import org.example.flowershop.model.entity.Category;
import org.example.flowershop.model.entity.Favorite;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.security.CurrentUser;
import org.example.flowershop.service.impl.FavoriteServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FavoriteEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FavoriteServiceImpl favoriteServiceImpl;

    @MockitoBean
    private FavoriteMapper favoriteMapper;

    User testUser = new User(
            1L, "Jon", "asdf", "Jon11",
            "jon.@email.com", "jon1122", UserType.USER
    );

    Category category = new Category(1L, "Wedding flowers", List.of());

    Product testProduct = new Product(1L, "rose", "white rose", 100, category, "rose.png", testUser, List.of(), List.of(), List.of());


    @Test
    void getFavoritesByUser_shouldReturnFavorites() throws Exception {

        Favorite favorite = Favorite.builder()
                .id(1L)
                .user(testUser)
                .product(testProduct)
                .build();

        FavoriteDto favoriteDto = FavoriteDto.builder()
                .id(1L)
                .userId(testUser.getId())
                .productId(testProduct.getId())
                .productName(testProduct.getName())
                .productPrice(testProduct.getPrice())
                .productDescription(testProduct.getDescription())
                .productImage(testProduct.getImage())
                .build();

        when(favoriteServiceImpl.getFavorites(testUser.getId(), "productName"))
                .thenReturn(List.of(favorite));

        when(favoriteMapper.toDto(favorite))
                .thenReturn(favoriteDto);

        mockMvc.perform(get("/favorites/{id}", testUser.getId())
                        .with(user(new CurrentUser(testUser)))
                        .param("sortBy", "productName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].productName").value("rose"))
                .andExpect(jsonPath("$[0].productPrice").value(100))
                .andExpect(jsonPath("$[0].productDescription").value("white rose"))
                .andExpect(jsonPath("$[0].productImage").value("rose.png"));
    }

    @Test
    void addToFavorite_shouldReturnCreated() throws Exception {

        SaveFavoriteRequest request = new SaveFavoriteRequest(1L);

        FavoriteDto favoriteDto = FavoriteDto.builder()
                .id(1L)
                .productId(2L)
                .productName("rose")
                .productPrice(100)
                .productDescription("white rose")
                .productImage("rose.png")
                .build();

        when(favoriteServiceImpl.addToFavorites(eq(testUser.getId()), any()))
                .thenReturn(favoriteDto);

        mockMvc.perform(post("/favorites")
                        .with(user(new CurrentUser(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.productName").value("rose"))
                .andExpect(jsonPath("$.productPrice").value(100))
                .andExpect(jsonPath("$.productDescription").value("white rose"))
                .andExpect(jsonPath("$.productImage").value("rose.png"));
    }


    @Test
    void addToFavorite_shouldReturnUnauthorized_whenUserNotAuthenticated() throws Exception {

        SaveFavoriteRequest request = new SaveFavoriteRequest(2L);

        mockMvc.perform(post("/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteFromFavorites_shouldReturnNoContent() throws Exception {

        long favoriteId = 1L;

        mockMvc.perform(delete("/favorites/{id}", favoriteId)
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(
                                        new CurrentUser(testUser),
                                        null,
                                        List.of()
                                )
                        )))
                .andExpect(status().isNoContent());

        verify(favoriteServiceImpl, times(1))
                .remove(testUser.getId(), favoriteId);
    }

    @Test
    void deleteFromFavorites_shouldReturnUnauthorized() throws Exception {

        mockMvc.perform(delete("/favorites/1"))
                .andExpect(status().isUnauthorized());

        verify(favoriteServiceImpl, never())
                .remove(anyLong(), anyLong());
    }
}