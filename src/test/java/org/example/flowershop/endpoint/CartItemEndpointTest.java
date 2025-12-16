package org.example.flowershop.endpoint;

import org.example.flowershop.dto.CartDto;
import org.example.flowershop.dto.SaveCartItemRequest;
import org.example.flowershop.mapper.CartItemMapper;
import org.example.flowershop.model.entity.CartItem;
import org.example.flowershop.model.entity.Category;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.security.CurrentUser;
import org.example.flowershop.service.impl.CartItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CartItemEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @MockitoBean
    private CartItemMapper cartItemMapper;


    @MockitoBean
    private CartItemServiceImpl cartItemServiceImpl;


    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    User testUser = new User(1L, "Jon", "asdf", "Jon11", "jon.@email.com", "jon1122", UserType.USER);
    Category category = new Category(1L, "Wedding flowers", List.of());
    Product testProduct = new Product(1L, "rose", "white rose", 100, category, "rose.png", testUser, List.of(), List.of(), List.of());


    @Test
    void getUserCartItems() throws Exception {

        CartItem cartItem = CartItem.builder()
                .user(testUser)
                .product(testProduct)
                .build();

        CartDto cartDto = new CartDto();
        cartDto.setId(1L);
        cartDto.setProductId(1L);
        cartDto.setProductName("rose");
        cartDto.setProductDescription("white rose");
        cartDto.setProductPrice(100);
        cartDto.setProductImage("rose.png");

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(cartItemServiceImpl.getCartByUser(testUser.getId(), "productName"))
                .thenReturn(List.of(cartItem));

        when(cartItemMapper.toDto(cartItem))
                .thenReturn(cartDto);

        mockMvc.perform(get("/cartItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }


    @Test
    void getUserCartItems_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/cartItem"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addToCart() throws Exception {
        SaveCartItemRequest request = new SaveCartItemRequest(1L);

        CartItem savedCartItem = CartItem.builder()
                .id(1L)
                .user(testUser)
                .product(testProduct)
                .build();

        CartDto dto = new CartDto();
        dto.setId(1L);
        dto.setProductId(1L);
        dto.setProductName("rose");
        dto.setProductDescription("white rose");
        dto.setProductPrice(100);
        dto.setProductImage("rose.png");

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);


        when(cartItemServiceImpl.addToCart(testUser.getId(), request))
                .thenReturn(dto);


        when(cartItemMapper.toDto(savedCartItem))
                .thenReturn(dto);

        mockMvc.perform(post("/cartItem")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("rose"))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.id").value(1));
    }


    @Test
    void addToCart_shouldReturn401_whenNotAuthenticated() throws Exception {
        SaveCartItemRequest request = new SaveCartItemRequest();
        request.setProductId(1L);

        mockMvc.perform(post("/cartItem")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteFromCart() throws Exception {
        long cartItemId = 1L;

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        mockMvc.perform(delete("/cartItem/{id}", cartItemId))
                .andExpect(status().isNoContent());

        verify(cartItemServiceImpl, times(1)).remove(testUser.getId(), cartItemId);
    }

    @Test
    void delete_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/cartItem/1"))
                .andExpect(status().isUnauthorized());

        verify(cartItemServiceImpl, never()).remove(anyLong(), anyLong());
    }
}