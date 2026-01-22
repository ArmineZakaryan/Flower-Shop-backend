package org.example.flowershop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.SaveCategoryRequest;
import org.example.flowershop.exception.CategoryAlreadyExistsException;
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
    public Page<CategoryDto> findAll(Pageable pageable) {
        log.info("Fetching categories with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<CategoryDto> page = categoryRepository.findAll(pageable)
                .map(categoryMapper::toDto);
        log.info("Fetched {} categories", page.getNumberOfElements());
        return page;
    }


    @Override
    public CategoryDto findById(long id) {
        log.info("Fetching category by id={}", id);
        Category category = categoryRepository
                .findById(id)
                .orElseThrow(() -> {
                    log.error("Category not found with id={}", id);
                    return new CategoryNotFoundException("Category not found with " + id + " id");
                });
        log.info("Category found: id={}, name={}", category.getId(), category.getName());
        return categoryMapper.toDto(category);
    }

    @Override
    public CategoryDto findByName(String name) {
        log.info("Fetching category by name={}", name);
        Category category = categoryRepository
                .findByName(name)
                .orElseThrow(() -> {
                    log.error("Category not found with name={}", name);
                    return new CategoryNotFoundException("Category not found with  name " + name);
                });
        log.info("Category found: id={}, name={}", category.getId(), category.getName());
        return categoryMapper.toDto(category);
    }

    @Override
    public CategoryDto save(SaveCategoryRequest request) {
        log.info("Attempting to save new category with name={}", request.getName());

        if (categoryRepository.findByName(request.getName()).isPresent()) {
            log.error("Category with name {} already exists", request.getName());
            throw new CategoryAlreadyExistsException("Category with name " + request.getName() + " already exists");
        }
        Category category = categoryMapper.toEntity(request);
        Category saved = categoryRepository.save(category);

        log.info("Category saved successfully id={}, name={}", saved.getId(), saved.getName());

        return categoryMapper.toDto(saved);
    }

    @Override
    public CategoryDto update(long id, SaveCategoryRequest request) {
        log.info("Attempting to update category id={} with new name={}", id, request.getName());

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id " + id));

        categoryRepository.findByName(request.getName())
                .filter(existing -> existing.getId() != id)
                .ifPresent(existing -> {
                    log.error("Cannot update category id={} to name={} because it already exists (id={})",
                            id, request.getName(), existing.getId());
                    throw new CategoryAlreadyExistsException("Category with name " + request.getName() + " already exists");
                });
        category.setName(request.getName());
        Category updated = categoryRepository.save(category);
        log.info("Category updated successfully id={}, name={}", updated.getId(), updated.getName());

        return categoryMapper.toDto(updated);
    }

    @Override
    public void deleteById(long id) {
        log.info("Attempting to delete category id={}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Category not found with id {}", id);
                    return new CategoryNotFoundException("Category not found with id " + id);
                });

        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            log.warn("Cannot delete category id={} because it has products", id);
            throw new IllegalStateException(
                    "Cannot delete category because it has products. Remove or reassign them first."
            );
        }
        categoryRepository.deleteById(id);
        log.info("Category with id={} deleted successfully", id);
    }
}