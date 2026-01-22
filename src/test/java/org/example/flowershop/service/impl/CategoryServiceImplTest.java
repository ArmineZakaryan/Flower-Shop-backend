package org.example.flowershop.service.impl;

import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.SaveCategoryRequest;
import org.example.flowershop.exception.CategoryAlreadyExistsException;
import org.example.flowershop.exception.CategoryNotFoundException;
import org.example.flowershop.mapper.CategoryMapper;
import org.example.flowershop.model.entity.Category;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceImplTest {

    @InjectMocks
    private CategoryServiceImpl categoryServiceImpl;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findAll_shouldReturnPageOfCategories() {
        Pageable pageable = PageRequest.of(0, 10);

        Category c1 = new Category(1L, "A", List.of());
        Category c2 = new Category(2L, "B", List.of());

        Page<Category> page = new PageImpl<>(List.of(c1, c2));

        when(categoryRepository.findAll(pageable)).thenReturn(page);
        when(categoryMapper.toDto(c1)).thenReturn(new CategoryDto(1L, "A"));
        when(categoryMapper.toDto(c2)).thenReturn(new CategoryDto(2L, "B"));

        Page<CategoryDto> result = categoryServiceImpl.findAll(pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("A", result.getContent().get(0).getName());
        assertEquals("B", result.getContent().get(1).getName());
    }


    @Test
    void findById_shouldReturnCategory() {
        Category category = new Category(1L, "Flowers", List.of());

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category))
                .thenReturn(new CategoryDto(1L, "Flowers"));

        CategoryDto result = categoryServiceImpl.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Flowers", result.getName());
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        CategoryNotFoundException ex = assertThrows(
                CategoryNotFoundException.class,
                () -> categoryServiceImpl.findById(1L)
        );

        assertEquals("Category not found with 1 id", ex.getMessage());
    }


    @Test
    void findByName_shouldReturnCategory() {
        Category category = new Category(1L, "Roses", List.of());

        when(categoryRepository.findByName("Roses"))
                .thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category))
                .thenReturn(new CategoryDto(1L, "Roses"));

        CategoryDto result = categoryServiceImpl.findByName("Roses");

        assertEquals("Roses", result.getName());
    }

    @Test
    void findByName_shouldThrowException_whenNotFound() {
        when(categoryRepository.findByName("Roses"))
                .thenReturn(Optional.empty());

        CategoryNotFoundException ex = assertThrows(
                CategoryNotFoundException.class,
                () -> categoryServiceImpl.findByName("Roses")
        );

        assertEquals("Category not found with  name Roses", ex.getMessage());
    }


    @Test
    void save_shouldCreateCategory() {
        SaveCategoryRequest request = new SaveCategoryRequest("Tulips");

        Category category = new Category(1L, "Tulips", List.of());

        when(categoryRepository.findByName("Tulips"))
                .thenReturn(Optional.empty());
        when(categoryMapper.toEntity(request)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toDto(category))
                .thenReturn(new CategoryDto(1L, "Tulips"));

        CategoryDto result = categoryServiceImpl.save(request);

        assertEquals(1L, result.getId());
        assertEquals("Tulips", result.getName());

        verify(categoryRepository).save(category);
    }

    @Test
    void save_shouldThrowException_whenCategoryAlreadyExists() {
        SaveCategoryRequest request = new SaveCategoryRequest("Tulips");

        when(categoryRepository.findByName("Tulips"))
                .thenReturn(Optional.of(new Category()));

        CategoryAlreadyExistsException ex = assertThrows(
                CategoryAlreadyExistsException.class,
                () -> categoryServiceImpl.save(request)
        );

        assertEquals("Category with name Tulips already exists", ex.getMessage());
    }


    @Test
    void update_shouldUpdateCategory() {
        long id = 1L;
        SaveCategoryRequest request = new SaveCategoryRequest("Updated");

        Category existing = new Category(id, "Old", List.of());

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(existing));
        when(categoryRepository.findByName("Updated"))
                .thenReturn(Optional.empty());
        when(categoryRepository.save(existing))
                .thenAnswer(inv -> inv.getArgument(0));
        when(categoryMapper.toDto(existing))
                .thenReturn(new CategoryDto(id, "Updated"));

        CategoryDto result = categoryServiceImpl.update(id, request);

        assertEquals("Updated", result.getName());
        verify(categoryRepository).save(existing);
    }

    @Test
    void update_shouldThrowException_whenCategoryNotFound() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        CategoryNotFoundException ex = assertThrows(
                CategoryNotFoundException.class,
                () -> categoryServiceImpl.update(1L, new SaveCategoryRequest("Any"))
        );

        assertEquals("Category not found with id 1", ex.getMessage());
    }


    @Test
    void deleteById_shouldDeleteCategory() {
        Category category = new Category(1L, "Flowers", Collections.emptyList());

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        categoryServiceImpl.deleteById(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteById_shouldThrowException_whenCategoryHasProducts() {
        Product product = new Product();
        Category category = new Category(
                1L,
                "Flowers",
                List.of(product)
        );

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> categoryServiceImpl.deleteById(1L)
        );

        assertEquals(
                "Cannot delete category because it has products. Remove or reassign them first.",
                ex.getMessage()
        );

        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteById_shouldThrowException_whenNotFound() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        CategoryNotFoundException ex = assertThrows(
                CategoryNotFoundException.class,
                () -> categoryServiceImpl.deleteById(1L)
        );

        assertEquals("Category not found with id 1", ex.getMessage());
    }
}