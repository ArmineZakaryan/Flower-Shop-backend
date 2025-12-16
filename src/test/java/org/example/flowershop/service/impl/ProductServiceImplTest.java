package org.example.flowershop.service.impl;

import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.ProductDto;
import org.example.flowershop.dto.SaveProductRequest;
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

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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
    void save_shouldReturnProduct_whenUserIsAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setUserType(UserType.ADMIN);

        Category category = new Category();
        category.setId(1L);
        category.setName("Flowers");

        CategoryDto categoryDto = new CategoryDto(1L, "Flowers");

        SaveProductRequest request = new SaveProductRequest();
        request.setName("rose");
        request.setDescription("white rose");
        request.setPrice(10.2);
        request.setImage("image.jpeg");
        request.setCategoryId(1L);

        Product testProduct = new Product(1L, "rose", "white rose", 100, category, "rose.png", null, List.of(), List.of(), List.of());


        ProductDto expectedDto = new ProductDto(
                1L, "rose", "white rose", 10.2,
                "image.jpeg", categoryDto
        );

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.findByName("rose")).thenReturn(Optional.empty());
        when(productMapper.toEntity(request)).thenReturn(testProduct);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(productMapper.toDto(testProduct)).thenReturn(expectedDto);

        ProductDto result = productServiceImpl.save(request, admin.getId());

        assertEquals("rose", result.getName());
        assertEquals("Flowers", result.getCategory().getName());

        verify(productRepository).save(any(Product.class));
    }


    @Test
    void findAllPageable_shouldReturnPageOfProduct() {
        Pageable pageable = PageRequest.of(0, 10);

        Category category = new Category();
        category.setId(1L);
        category.setName("Flowers");

        List<Product> products = List.of(
                new Product(1L, "rose", "white rose", 10.2, category, "image.jpeg", null, List.of(), List.of(), List.of()),
                new Product(2L, "lily", "white lily", 15.2, category, "image.jpeg", null, List.of(), List.of(), List.of())
        );

        Page<Product> page = new PageImpl<>(products);

        when(productRepository.findAll(pageable)).thenReturn(page);
        when(productMapper.toDto(any(Product.class)))
                .thenAnswer(inv -> {
                    Product p = inv.getArgument(0);
                    return new ProductDto(
                            p.getId(),
                            p.getName(),
                            p.getDescription(),
                            p.getPrice(),
                            p.getImage(),
                            new CategoryDto(p.getCategory().getId(), p.getCategory().getName())
                    );
                });

        Page<ProductDto> result = productServiceImpl.findAll(pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("rose", result.getContent().get(0).getName());
        assertEquals("lily", result.getContent().get(1).getName());
    }


    @Test
    void findById_shouldReturnProduct() {
        Category category = new Category(1L, "Flowers", null);
        Product testProduct = new Product(1L, "rose", "white rose", 100, category, "rose.png", null, List.of(), List.of(), List.of());


        ProductDto dto = new ProductDto(
                1L, "rose", "white rose", 10.2,
                "image.jpeg", new CategoryDto(1L, "Flowers")
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(testProduct)).thenReturn(dto);

        ProductDto result = productServiceImpl.findById(1L);

        assertEquals("rose", result.getName());
    }


    @Test
    void findByName_shouldReturnProduct() {
        Category category = new Category(1L, "Flowers", null);
        Product testProduct = new Product(1L, "rose", "white rose", 100, category, "rose.png", null, List.of(), List.of(), List.of());


        ProductDto dto = new ProductDto(
                1L, "rose", "white rose", 10.2,
                "image.jpeg", new CategoryDto(1L, "Flowers")
        );

        when(productRepository.findByName("rose")).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(testProduct)).thenReturn(dto);

        ProductDto result = productServiceImpl.findByName("rose");

        assertEquals("rose", result.getName());
    }


    @Test
    void getImage_shouldThrowException_whenImageNotFound() {
        String imageName = "image.jpeg";

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(any(Path.class)))
                    .thenReturn(false);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> productServiceImpl.getImage(imageName)
            );

            assertEquals("Image file not found: image.jpeg", exception.getMessage());
        }
    }
}