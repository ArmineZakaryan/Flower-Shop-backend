package org.example.flowershop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.SaveCategoryRequest;
import org.example.flowershop.exception.CategoryNotFoundException;
import org.example.flowershop.mapper.CategoryMapper;
import org.example.flowershop.model.entity.Category;
import org.example.flowershop.repository.CategoryRepository;
import org.example.flowershop.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDto> findAllPageable(Pageable pageable) {
        log.info("Fetching categories pageable: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        return categoryRepository.findAll(pageable)
                .map(categoryMapper::toDto);
    }


    @Override
    public CategoryDto findByName(String name) {
        log.info("Fetching category by name={}", name);
        Category category = categoryRepository
                .findByName(name)
                .orElseThrow(() -> {
                    log.error("Category not found with name={}", name);
                    return new CategoryNotFoundException("Category not found with " + name + " name");
                });
        return categoryMapper.toDto(category);
    }

    @Override
    public CategoryDto save(SaveCategoryRequest categoryRequest) {
        log.info("Saving new category with name={}", categoryRequest.getName());
        Category category = categoryRepository.save(categoryMapper.toEntity(categoryRequest));
        log.info("Category saved with id={}", category.getId());
        return categoryMapper.toDto(category);
    }

    @Override
    public void deleteById(long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id " + id));

        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            log.warn("Cannot delete category id={} because it has products", id);
            throw new IllegalStateException(
                    "Cannot delete category because it has products. Remove or reassign them first."
            );
        }

        categoryRepository.deleteById(id);
        log.info("Category with id={} deleted successfully", id);
    }

    @Override
    public CategoryDto update(Long id, SaveCategoryRequest request) {
        log.info("Updating category with id={} with new name={}", id, request.getName());
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Category not found with id={}", id);
                    return new CategoryNotFoundException("Category not found with " + id + " id");
                });
        category.setName(request.getName());
        Category updated = categoryRepository.save(category);
        log.info("Category with id={} updated", updated.getId());
        return categoryMapper.toDto(updated);
    }
}