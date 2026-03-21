package org.example.flowershop.endpoint;

import org.example.flowershop.dto.OrderDto;
import org.example.flowershop.dto.SaveOrderRequest;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.Status;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.security.CurrentUser;
import org.example.flowershop.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class OrderEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    User testUser = new User(1L, "Jon", "asdf", "Jon11", "jon.@email.com", "jon1122", UserType.USER);

    @Test
    void getMyOrders() throws Exception {
        OrderDto orderDto = new OrderDto();
        orderDto.setId(1L);
        orderDto.setUserId(testUser.getId());
        orderDto.setPrice(100);
        orderDto.setStatus(Status.NEW);
        orderDto.setOrderDate(LocalDateTime.now());
        orderDto.setAddress("address");

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(orderService.getOrdersByUser(testUser.getId(), "orderDate"))
                .thenReturn(List.of(orderDto));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(testUser.getId()))
                .andExpect(jsonPath("$[0].price").value(100))
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[0].orderDate").exists())
                .andExpect(jsonPath("$[0].address").value("address"));
    }


    @Test
    void getOrderById() throws Exception {
        OrderDto orderDto = new OrderDto();
        orderDto.setId(1L);
        orderDto.setUserId(1L);
        orderDto.setPrice(100);
        orderDto.setStatus(Status.NEW);
        orderDto.setOrderDate(LocalDateTime.now());
        orderDto.setAddress("address");

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        currentUserDetails,
                        null,
                        currentUserDetails.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(orderService.findByIdForUser(1L, testUser)).thenReturn(orderDto);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.price").value(100))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.orderDate").exists())
                .andExpect(jsonPath("$.address").value("address"));
    }

    @Test
    void getOrderById_shouldReturn403_whenUserIsNotOwner() throws Exception {
        CurrentUser currentUserDetails = new CurrentUser(testUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUserDetails, null, currentUserDetails.getAuthorities())
        );

        when(orderService.findByIdForUser(1L, testUser))
                .thenThrow(new AccessDeniedException("You are not allowed to access this order"));

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof AccessDeniedException))
                .andExpect(result ->
                        assertEquals("You are not allowed to access this order", result.getResolvedException().getMessage()));
    }


    @Test
    void createOrder() throws Exception {
        SaveOrderRequest request = new SaveOrderRequest();
        request.setAddress("address");
        request.setQuantity(2);
        request.setProductId(5L);

        OrderDto orderDto = new OrderDto();
        orderDto.setId(1L);
        orderDto.setUserId(testUser.getId());
        orderDto.setPrice(200);
        orderDto.setStatus(Status.NEW);
        orderDto.setOrderDate(LocalDateTime.now());
        orderDto.setAddress("address");

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        currentUserDetails, null, currentUserDetails.getAuthorities())
        );

        when(orderService.save(any(SaveOrderRequest.class), eq(testUser.getId())))
                .thenReturn(orderDto);

        mockMvc.perform(post("/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.price").value(200))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.orderDate").exists())
                .andExpect(jsonPath("$.address").value("address"));
    }


    @Test
    void update_shouldReturn200_whenUserHasPermission() throws Exception {
        long orderId = 1L;
        SaveOrderRequest request = new SaveOrderRequest();
        request.setAddress("address");
        request.setQuantity(2);
        request.setProductId(5L);

        OrderDto orderDto = new OrderDto();
        orderDto.setId(orderId);
        orderDto.setUserId(testUser.getId());
        orderDto.setPrice(200);
        orderDto.setStatus(Status.NEW);
        orderDto.setOrderDate(LocalDateTime.now());
        orderDto.setAddress("address");

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        currentUserDetails, null, currentUserDetails.getAuthorities())
        );

        when(orderService.update(orderId, request, testUser))
                .thenReturn(orderDto);

        mockMvc.perform(put("/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.price").value(200))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.orderDate").exists())
                .andExpect(jsonPath("$.address").value("address"));
    }

    @Test
    void update_shouldReturn403_whenUserDoesNotHavePermission() throws Exception {
        User otherUser = new User(2L, "oil", "asdf", "oil11", "oil.@email.com", "oil1122", UserType.USER);
        long orderId = 1L;

        SaveOrderRequest request = new SaveOrderRequest();
        request.setAddress("address");
        request.setQuantity(2);
        request.setProductId(5L);

        CurrentUser currentUserDetails = new CurrentUser(otherUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        currentUserDetails, null, currentUserDetails.getAuthorities())
        );

        when(orderService.update(orderId, request, otherUser))
                .thenThrow(new AccessDeniedException("You cannot update this order"));

        mockMvc.perform(put("/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof AccessDeniedException))
                .andExpect(result ->
                        assertEquals("You cannot update this order", result.getResolvedException().getMessage()));
    }
}