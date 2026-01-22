package org.example.flowershop.service;

import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.SaveCategoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {
    Page<CategoryDto> findAll(Pageable pageable);

    CategoryDto findById(long id);

    CategoryDto findByName(String name);

    CategoryDto save(SaveCategoryRequest categoryRequest);

    void deleteById(long id);

    CategoryDto update(long id, SaveCategoryRequest request);
}