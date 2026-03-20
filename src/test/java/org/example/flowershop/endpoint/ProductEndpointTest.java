package org.example.flowershop.endpoint;

import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.ProductDto;
import org.example.flowershop.dto.SaveProductRequest;
import org.example.flowershop.exception.CategoryNotFoundException;
import org.example.flowershop.exception.ProductNotFoundException;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.security.CurrentUser;
import org.example.flowershop.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductEndpointTest {

    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private ProductService productService;


    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    User testUser = new User(1L, "Jon", "asdf", "Jon11", "jon.@email.com", "jon1122", UserType.USER);


    @Test
    void getAllProducts() throws Exception {
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(1L);
        categoryDto.setName("Wedding flowers");

        ProductDto productDto = new ProductDto();
        productDto.setId(1);
        productDto.setName("test");
        productDto.setDescription("test");
        productDto.setPrice(100);
        productDto.setImage("image");
        productDto.setCategory(categoryDto);

        Page<ProductDto> productPage = new PageImpl<>(List.of(productDto));

        when(productService.findAll(any(Pageable.class)))
                .thenReturn(productPage);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("test"))
                .andExpect(jsonPath("$.content[0].description").value("test"))
                .andExpect(jsonPath("$.content[0].price").value(100))
                .andExpect(jsonPath("$.content[0].image").value("image"))
                .andExpect(jsonPath("$.content[0].category.name").value(categoryDto.getName()));
    }


    @Test
    void testGetProduct() throws Exception {
        ProductDto dto = new ProductDto();
        dto.setId(20);
        dto.setName("Roses");

        when(productService.findByName("Roses"))
                .thenReturn(dto);

        mockMvc.perform(get("/products/by-name/Roses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Roses"))
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void testGetProduct_NotFound() throws Exception {

        when(productService.findByName("Roses"))
                .thenThrow(new ProductNotFoundException("Product not found with id: 20"));

        mockMvc.perform(get("/products/by-name/Roses"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateProduct() throws Exception {

        testUser.setUserType(UserType.ADMIN);
        testUser.setId(99L);

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        currentUserDetails,
                        null,
                        currentUserDetails.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ProductDto createdProduct = new ProductDto();
        createdProduct.setId(1);
        createdProduct.setName("Rose");
        createdProduct.setPrice(100);
        createdProduct.setDescription("test");

        when(productService.save(any(SaveProductRequest.class), eq(99L), any()))
                .thenReturn(createdProduct);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "rose.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/products")
                        .file(image)
                        .param("name", "Rose")
                        .param("description", "Red rose")
                        .param("price", "100")
                        .param("categoryId", "1")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Rose"));
    }

    @Test
    void create_whenUserIsNull_shouldReturnUnauthorized() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test image".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/products")
                        .file(image)
                        .param("name", "Rose")
                        .param("description", "Red rose")
                        .param("price", "100")
                        .param("categoryId", "1")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_whenUserIsNotAdmin_shouldThrowAccessDenied() throws Exception {
        testUser.setId(99L);
        testUser.setUserType(UserType.USER);

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        currentUserDetails,
                        null,
                        currentUserDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "rose.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image".getBytes()
        );

        when(productService.save(any(SaveProductRequest.class), eq(99L), any()))
                .thenThrow(new AccessDeniedException("Only admins can create products"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/products")
                        .file(image)
                        .param("name", "Rose")
                        .param("price", "100")
                        .param("description", "Beautiful flower")
                        .param("categoryId", "1")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void update_whenUserIsNull_shouldReturnUnauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "rose.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image".getBytes()
        );

        mockMvc.perform(multipart("/products/1")
                        .file(image)
                        .with(req -> {
                            req.setMethod("PUT"); // needed for multipart PUT
                            return req;
                        })
                        .param("name", "Rose")
                        .param("description", "Beautiful flower")
                        .param("price", "100")
                        .param("categoryId", "1")
                )
                .andExpect(status().isUnauthorized());
    }
    @Test
    void update_whenUserIsAdmin_shouldReturnOk() throws Exception {
        testUser.setUserType(UserType.ADMIN);
        testUser.setId(1L);

        SaveProductRequest request = new SaveProductRequest();
        request.setName("Rose");
        request.setDescription("Beautiful flower");
        request.setPrice(100);
        request.setCategoryId(1L);

        CategoryDto category = new CategoryDto();
        category.setId(request.getCategoryId());

        ProductDto updatedProduct = new ProductDto();
        updatedProduct.setId(1L);
        updatedProduct.setName(request.getName());
        updatedProduct.setDescription(request.getDescription());
        updatedProduct.setPrice(request.getPrice());
        updatedProduct.setCategory(category);

        when(productService.update(eq(1L), any(SaveProductRequest.class), any(), eq(testUser.getId())))
                .thenReturn(updatedProduct);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "rose.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image".getBytes()
        );

        mockMvc.perform(multipart("/products/1")
                        .file(image)
                        .with(user(new CurrentUser(testUser))) // ✅
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        })
                        .param("name", request.getName())
                        .param("description", request.getDescription())
                        .param("price", String.valueOf(request.getPrice()))
                        .param("categoryId", String.valueOf(request.getCategoryId()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Rose"))
                .andExpect(jsonPath("$.description").value("Beautiful flower"))
                .andExpect(jsonPath("$.price").value(100))
                .andExpect(jsonPath("$.category.id").value(1));
    }
//    @Test
//    void update_whenUserIsAdmin_shouldReturnOk() throws Exception {
//        testUser.setUserType(UserType.ADMIN);
//        testUser.setId(1L);
//
//        CurrentUser currentUserDetails = new CurrentUser(testUser);
//        UsernamePasswordAuthenticationToken authentication =
//                new UsernamePasswordAuthenticationToken(
//                        currentUserDetails,
//                        null,
//                        currentUserDetails.getAuthorities()
//                );
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//
//        SaveProductRequest request = new SaveProductRequest();
//        request.setName("Rose");
//        request.setDescription("Beautiful flower");
//        request.setPrice(100);
//        request.setCategoryId(1L);
//
//        CategoryDto category = new CategoryDto();
//        category.setId(request.getCategoryId());
//
//        ProductDto updatedProduct = new ProductDto();
//        updatedProduct.setId(1L);
//        updatedProduct.setName(request.getName());
//        updatedProduct.setDescription(request.getDescription());
//        updatedProduct.setPrice(request.getPrice());
//        updatedProduct.setCategory(category);
//
//        when(productService.update(eq(1L), any(SaveProductRequest.class), any(), eq(testUser.getId())))
//                .thenReturn(updatedProduct);
//
//        MockMultipartFile image = new MockMultipartFile(
//                "image",
//                "rose.jpg",
//                MediaType.IMAGE_JPEG_VALUE,
//                "fake image".getBytes()
//        );
//
//        mockMvc.perform(multipart("/products/1")
//                        .file(image)
//                        .with(req -> {
//                            req.setMethod("PUT");
//                            return req;
//                        })
//                        .param("name", request.getName())
//                        .param("description", request.getDescription())
//                        .param("price", String.valueOf(request.getPrice()))
//                        .param("categoryId", String.valueOf(request.getCategoryId()))
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(1))
//                .andExpect(jsonPath("$.name").value("Rose"))
//                .andExpect(jsonPath("$.description").value("Beautiful flower"))
//                .andExpect(jsonPath("$.price").value(100))
//                .andExpect(jsonPath("$.category.id").value(1));
//    }


    @Test
    void delete_whenUserIsAdmin_shouldReturnNoContent() throws Exception {

        testUser.setId(99L);
        testUser.setUserType(UserType.ADMIN);

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        doNothing().when(productService).deleteById(1L, 99L);

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithAnonymousUser
    void delete_whenUserIsAnonymous_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void delete_whenUserIsNotAdmin_shouldReturnForbidden() throws Exception {

        testUser.setId(55L);
        testUser.setUserType(UserType.USER);

        CurrentUser currentUserDetails = new CurrentUser(testUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getImage_shouldReturnBytes_whenFileExists() throws Exception {
        byte[] expectedBytes = {1, 2, 3, 4};

        when(productService.getImage("image.jpeg")).thenReturn(expectedBytes);

        mockMvc.perform(get("/products/img/{imageName}", "image.jpeg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/jpeg"))
                .andExpect(content().bytes(expectedBytes));
    }

    @Test
    void getImage_shouldReturnNotFound_whenFileDoesNotExist() throws Exception {
        when(productService.getImage("rose.png")).thenReturn(null);

        mockMvc.perform(get("/products/img/{imageName}", "rose.png"))
                .andExpect(status().isNotFound())
                .andExpect(result -> {
                    assertTrue(result.getResolvedException() instanceof ResponseStatusException);
                    ResponseStatusException ex = (ResponseStatusException) result.getResolvedException();
                    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
                    assertTrue(ex.getReason().contains("Image not found: rose.png"));
                });

        verify(productService).getImage("rose.png");
    }

    @Test
    void getProductsByCategory_shouldReturnProducts() throws Exception {
        String category = "Flowers";

        ProductDto productDto = new ProductDto();
        productDto.setName("Rose");

        Page<ProductDto> page = new PageImpl<>(List.of(productDto));

        when(productService.findByCategory(eq(category), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/products/by-category")
                        .param("category", category)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Rose"));

        verify(productService).findByCategory(eq(category), any(Pageable.class));
    }

    @Test
    void getProductsByCategory_shouldReturnEmptyList() throws Exception {
        String category = "Flowers";

        Page<ProductDto> emptyPage = Page.empty();

        when(productService.findByCategory(eq(category), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/products/by-category")
                        .param("category", category))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getProductsByCategory_shouldReturnBadRequest_whenCategoryMissing() throws Exception {
        mockMvc.perform(get("/products/by-category"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProductsByCategory_shouldReturnNotFound_whenServiceThrows() throws Exception {
        String category = "Unknown";

        when(productService.findByCategory(eq(category), any(Pageable.class)))
                .thenThrow(new CategoryNotFoundException("Not found"));

        mockMvc.perform(get("/products/by-category")
                        .param("category", category))
                .andExpect(status().isNotFound());
    }
}