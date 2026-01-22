package org.example.flowershop.service.impl;

import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.ProductDto;
import org.example.flowershop.dto.SaveProductRequest;
import org.example.flowershop.exception.ImageNotFoundException;
import org.example.flowershop.exception.ProductNotFoundException;
import org.example.flowershop.mapper.ProductMapper;
import org.example.flowershop.model.entity.Category;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.repository.CategoryRepository;
import org.example.flowershop.repository.ProductRepository;
import org.example.flowershop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl productServiceImpl;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        Field field = ProductServiceImpl.class.getDeclaredField("imageUploadPath");
        field.setAccessible(true);
        field.set(productServiceImpl, "/uploads");
    }


    @Test
    void save_givenAdminUser_shouldReturnProductDto() {
        User admin = new User();
        admin.setId(1L);
        admin.setUserType(UserType.ADMIN);

        Category category = new Category(1L, "Flowers", null);

        SaveProductRequest request = new SaveProductRequest();
        request.setName("rose");
        request.setDescription("white rose");
        request.setPrice(10);
        request.setCategoryId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setName("rose");

        ProductDto dto = new ProductDto(
                1L, "rose", "white rose", 10,
                null, new CategoryDto(1L, "Flowers")
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(productRepository.findByName("rose")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toDto(product)).thenReturn(dto);

        ProductDto result = productServiceImpl.save(request, 1L, null);

        assertEquals("rose", result.getName());
        verify(productRepository).save(product);
    }

    @Test
    void save_givenNonAdmin_shouldThrowAccessDenied() {
        User user = new User();
        user.setUserType(UserType.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(
                AccessDeniedException.class,
                () -> productServiceImpl.save(new SaveProductRequest(), 1L, null)
        );
    }

    @Test
    void update_givenAdmin_shouldUpdateProduct() {
        User admin = new User();
        admin.setUserType(UserType.ADMIN);

        Category category = new Category(1L, "Flowers", null);

        Product product = new Product();
        product.setId(1L);

        SaveProductRequest request = new SaveProductRequest();
        request.setName("lily");
        request.setDescription("white");
        request.setPrice(12);
        request.setCategoryId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("lily")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDto(product)).thenReturn(
                new ProductDto(1L, "lily", "white", 12, null,
                        new CategoryDto(1L, "Flowers"))
        );

        ProductDto result = productServiceImpl.update(1L, request, null, 1L);

        assertEquals("lily", result.getName());
    }

    @Test
    void update_whenSameProductName_shouldNotThrowException() {
        User admin = new User();
        admin.setUserType(UserType.ADMIN);

        Product product = new Product();
        product.setId(200L);

        SaveProductRequest request = new SaveProductRequest();
        request.setName("rose");
        request.setCategoryId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(productRepository.findById(200L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("rose")).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category()));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toDto(any(Product.class))).thenReturn(new ProductDto());

        assertDoesNotThrow(() ->
                productServiceImpl.update(200L, request, null, 1L)
        );
    }

    @Test
    void delete_givenAdminAndNoRelations_shouldDeleteProduct() {
        User admin = new User();
        admin.setUserType(UserType.ADMIN);

        Product product = new Product();
        product.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productServiceImpl.deleteById(1L, 1L);

        verify(productRepository).delete(product);
    }

    @Test
    void findAll_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Category category = new Category(1L, "Flowers", null);
        Product product = new Product(1L, "rose", "desc", 10,
                category, null, null, List.of(), List.of(), List.of());

        Page<Product> page = new PageImpl<>(List.of(product));

        when(productRepository.findAll(pageable)).thenReturn(page);
        when(productMapper.toDto(any(Product.class)))
                .thenReturn(new ProductDto(
                        1L, "rose", "desc", 10,
                        null, new CategoryDto(1L, "Flowers"))
                );

        Page<ProductDto> result = productServiceImpl.findAll(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findById_whenNotFound_shouldThrowException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productServiceImpl.findById(1L)
        );
    }


    @Test
    void getImage_whenImageDoesNotExist_shouldThrowException() {
        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.exists(any(Path.class))).thenReturn(false);

            ImageNotFoundException ex = assertThrows(
                    ImageNotFoundException.class,
                    () -> productServiceImpl.getImage("test.jpg")
            );
            assertEquals("Image not found", ex.getMessage());
        }
    }
}