package org.example.flowershop.service;

import jakarta.validation.Valid;
import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.SaveCategoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {
    Page<CategoryDto> findAllPageable(Pageable pageable);


    CategoryDto findById(long id);

    CategoryDto findByName(String name);

    CategoryDto save(SaveCategoryRequest categoryRequest);

    void deleteById(long id);

    CategoryDto update(Long id, @Valid SaveCategoryRequest request);
}
