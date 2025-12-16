package org.example.flowershop.endpoint;

import org.example.flowershop.dto.OrderDto;
import org.example.flowershop.dto.SaveOrderRequest;
import org.example.flowershop.exception.OrderNotFoundException;
import org.example.flowershop.mapper.OrderMapper;
import org.example.flowershop.model.entity.Order;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class OrderEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    User testUser = new User(1L, "Jon", "asdf", "Jon11", "jon.@email.com", "jon1122", UserType.USER);

    @Test
    void getMyOrders() throws Exception {
        Order order = Order.builder()
                .user(testUser)
                .price(100)
                .status(Status.NEW)
                .orderDate(LocalDateTime.now())
                .address("address")
                .build();

        OrderDto orderDto = new OrderDto();
        orderDto.setId(1L);
        orderDto.setUserId(2L);
        orderDto.setPrice(100);
        orderDto.setStatus(Status.NEW);
        orderDto.setOrderDate(LocalDateTime.now());
        orderDto.setAddress("address");

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(orderService.getOrdersByUser(1L, "orderDate"))
                .thenReturn(List.of(order));

        when(orderMapper.toDto(order))
                .thenReturn(orderDto);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(2))
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

        when(orderService.findById(1L)).thenReturn(orderDto);

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
        OrderDto orderDto = new OrderDto();
        orderDto.setId(1L);
        orderDto.setUserId(999L);

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUserDetails, null, currentUserDetails.getAuthorities())
        );

        when(orderService.findById(1L)).thenReturn(orderDto);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isForbidden());
    }


    @Test
    void getOrderById_shouldReturn404_ifNotFound() throws Exception {
        long orderId = 999L;

        when(orderService.findById(orderId))
                .thenThrow(new OrderNotFoundException("Order not found"));

        mockMvc.perform(get("/orders/{id}", orderId))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(orderService).findById(orderId);
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