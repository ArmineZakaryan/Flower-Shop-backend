package org.example.flowershop.service.impl;

import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.SaveCategoryRequest;
import org.example.flowershop.exception.CategoryNotFoundException;
import org.example.flowershop.mapper.CategoryMapper;
import org.example.flowershop.model.entity.Category;
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
import static org.mockito.ArgumentMatchers.any;
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
    void findAllPageable_shouldReturnPageOfCategory() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Category> categories = List.of(new Category(1L, "A", List.of()),
                new Category(2L, "B", List.of()));

        Page<Category> page = new PageImpl<>(categories);

        when(categoryRepository.findAll(pageable)).thenReturn(page);
        when(categoryMapper.toDto(any(Category.class)))
                .thenAnswer(inv -> {
                    Category c = inv.getArgument(0);
                    return new CategoryDto(c.getId(), c.getName());
                });
        Page<CategoryDto> result = categoryServiceImpl.findAllPageable(pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("A", result.getContent().get(0).getName());
    }

    @Test
    void findById_shouldReturnListOfCategory() {
        Category category = new Category(1L, "A", List.of());

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category)).thenReturn(new CategoryDto(1L, "A"));

        CategoryDto result = categoryServiceImpl.findById(1L);

        assertEquals("A", result.getName());
    }

    @Test
    void findById_shouldThrowException_whenCategoryNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class,
                () -> categoryServiceImpl.findById(1L));
    }


    @Test
    void findByName_shouldReturnCategory() {
        Category category = new Category(1L, "A", List.of());


        when(categoryRepository.findByName(any())).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category)).thenReturn(new CategoryDto(1L, "A"));

        CategoryDto result = categoryServiceImpl.findByName("A");

        assertEquals("A", result.getName());
    }

    @Test
    void findByName_shouldThrowException_whenCategoryNotFound() {
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryServiceImpl.findByName("A"));
    }

    @Test
    void save_shouldReturnCategory() {
        SaveCategoryRequest request = new SaveCategoryRequest();
        request.setName("A");

        Category category = new Category(1L, "A", List.of());

        when(categoryMapper.toEntity(any(SaveCategoryRequest.class))).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(any(Category.class))).thenAnswer(inv -> {

            Category c = inv.getArgument(0);
            return new CategoryDto(c.getId(), c.getName());
        });

        CategoryDto result = categoryServiceImpl.save(request);

        assertEquals(1L, result.getId());
        assertEquals("A", category.getName());

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void deleteById_shouldDeleteCategory() {
        long id = 1L;

        Category category = new Category();
        category.setId(id);
        category.setProducts(Collections.emptyList());

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(category));

        categoryServiceImpl.deleteById(id);

        verify(categoryRepository).deleteById(id);
    }

    @Test
    void deleteById_shouldThrowExceptionIfCategoryNotFound() {
        long id = 1L;

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class,
                () -> categoryServiceImpl.deleteById(id));

        assertEquals("Category not found with id 1", exception.getMessage());
    }

    @Test
    void update_shouldUpdateCategory() {
        long id = 1L;

        SaveCategoryRequest request = new SaveCategoryRequest("A");
        request.setName("Updated");

        Category existing = new Category(id, "Old", List.of());

        Category updatedEntity = new Category(id, "Old", List.of());


        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(updatedEntity);
        when(categoryMapper.toDto(updatedEntity)).thenReturn(new CategoryDto(id, "Updated"));

        CategoryDto result = categoryServiceImpl.update(id, request);

        assertEquals(id, result.getId());
        assertEquals("Updated", result.getName());

        verify(categoryRepository).findById(id);
        verify(categoryRepository).save(existing);
        verify(categoryMapper).toDto(updatedEntity);
    }

    @Test
    void update_shouldThrowExceptionIfCategoryNotFound() {
        long id = 1L;

        SaveCategoryRequest request = new SaveCategoryRequest();
        request.setName("Anything");

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(
                CategoryNotFoundException.class,
                () -> categoryServiceImpl.update(id, request)
        );

        assertEquals("Category not found with 1 id", exception.getMessage());
    }
}