package org.example.flowershop.endpoint;

import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.SaveCategoryRequest;
import org.example.flowershop.exception.CategoryNotFoundException;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.security.CurrentUser;
import org.example.flowershop.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CategoryEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;


    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllCategories() throws Exception {
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(1L);
        categoryDto.setName("Wedding flowers");

        Page<CategoryDto> page = new PageImpl<>(List.of(categoryDto));

        when(categoryService.findAllPageable(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/categories")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "name")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Wedding flowers"));
    }


    @Test
    void getCategoryByName() throws Exception {
        String categoryName = "Wedding flowers";

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(1L);
        categoryDto.setName(categoryName);

        when(categoryService.findByName(categoryName))
                .thenReturn(categoryDto);

        mockMvc.perform(get("/categories/{name}", categoryName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wedding flowers"));
    }


    @Test
    void getCategories_shouldReturn404_whenNotFound() throws Exception {
        String categoryName = "NonExistentCategory";

        when(categoryService.findByName(categoryName))
                .thenThrow(new CategoryNotFoundException("Category not found with " + categoryName + " name"));

        mockMvc.perform(get("/categories/{name}", categoryName))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void createCategory() throws Exception {
        User adminUser = new User(1L, "Admin", "password", "admin", "admin@email.com", "admin123", UserType.ADMIN);

        SaveCategoryRequest request = new SaveCategoryRequest();
        request.setName("Wedding flowers");

        CategoryDto createdDto = new CategoryDto();
        createdDto.setId(1L);
        createdDto.setName("Wedding flowers");

        CurrentUser currentUserDetails = new CurrentUser(adminUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(categoryService.save(any(SaveCategoryRequest.class)))
                .thenReturn(createdDto);

        mockMvc.perform(post("/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wedding flowers"));
    }

    @Test
    void createCategory_shouldReturn409_whenAlreadyExists() throws Exception {
        User adminUser = new User(1L, "Admin", "password", "admin", "admin@email.com", "admin123", UserType.ADMIN);

        SaveCategoryRequest request = new SaveCategoryRequest();
        request.setName("Wedding flowers");

        CurrentUser currentUserDetails = new CurrentUser(adminUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(categoryService.save(any(SaveCategoryRequest.class)))
                .thenThrow(new RuntimeException("Category already exists"));

        mockMvc.perform(post("/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }


    @Test
    void updateCategory() throws Exception {
        User adminUser = new User(1L, "Admin", "password", "admin", "admin@email.com", "admin123", UserType.ADMIN);

        SaveCategoryRequest request = new SaveCategoryRequest();
        request.setName("Wedding flowers");

        CategoryDto updatedDto = new CategoryDto();
        updatedDto.setId(2L);
        updatedDto.setName("Wedding flowers");

        CurrentUser currentUserDetails = new CurrentUser(adminUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(categoryService.update(2L, request))
                .thenReturn(updatedDto);

        mockMvc.perform(put("/categories/2")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Wedding flowers"));
    }


    @Test
    void deleteCategory() throws Exception {
        User adminUser = new User(1L, "Admin", "password", "admin", "admin@email.com", "admin123", UserType.ADMIN);
        long categoryId = 2L;

        CurrentUser currentUserDetails = new CurrentUser(adminUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        mockMvc.perform(delete("/categories/{id}", categoryId))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteById(categoryId);
    }

    @Test
    void deleteCategory_shouldReturn404_whenNotFound() throws Exception {
        User adminUser = new User(1L, "Admin", "password", "admin", "admin@email.com", "admin123", UserType.ADMIN);
        long categoryId = 999L;

        CurrentUser currentUserDetails = new CurrentUser(adminUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        doThrow(new CategoryNotFoundException("Category not found with " + categoryId + " id"))
                .when(categoryService).deleteById(categoryId);

        mockMvc.perform(delete("/categories/{id}", categoryId))
                .andExpect(status().isNotFound());

        verify(categoryService, times(1)).deleteById(categoryId);
    }
}
