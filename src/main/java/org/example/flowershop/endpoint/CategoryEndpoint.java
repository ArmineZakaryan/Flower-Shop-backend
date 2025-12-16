package org.example.flowershop.endpoint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.SaveCategoryRequest;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor

@RequestMapping("/categories")
@Slf4j
public class CategoryEndpoint {
    private final CategoryService categoryServiceImpl;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {

        log.info("Fetching all categories with pagination and sorting: page={}, size={}, sortBy={}, sortOrder={}",
                page, size, sortBy, sortOrder);

        Sort sort = Sort.by(Sort.Order.by(sortBy));
        if ("desc".equalsIgnoreCase(sortOrder)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CategoryDto> categoriesPage = categoryServiceImpl.findAllPageable(pageable);

        return ResponseEntity.ok(categoriesPage.getContent());
    }


    @GetMapping("/{name}")
    public ResponseEntity<CategoryDto> getCategoryByName(@PathVariable String name) {
        log.info("Fetching category with name: {}", name);
        CategoryDto category = categoryServiceImpl.findByName(name);
        if (category != null) {
            log.info("Category found: {}", category.getName());
            return ResponseEntity.ok(category);
        } else {
            log.warn("Category with name {} not found", name);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<CategoryDto> create(
            @RequestBody @Valid SaveCategoryRequest saveCategoryRequest,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("Attempting to create category: {}", saveCategoryRequest.getName());

        if (currentUser == null) {
            log.error("User is not authenticated");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        if (currentUser.getUserType() != UserType.ADMIN) {
            log.error("Access denied: Only admins can create categories");
            throw new AccessDeniedException("Only admins can create categories");
        }
        try {
            CategoryDto created = categoryServiceImpl.save(saveCategoryRequest);
            log.info("Category created successfully: {}", created.getName());
            return ResponseEntity.status(201).body(created);
        } catch (RuntimeException e) {
            log.error("Error creating category: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> update(
            @PathVariable Long id,
            @RequestBody @Valid SaveCategoryRequest request,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("Attempting to update category with id: {}", id);

        if (currentUser == null) {
            log.error("User is not authenticated");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        if (currentUser.getUserType() != UserType.ADMIN) {
            log.error("Access denied: Only admins can update categories");
            throw new AccessDeniedException("Only admins can update categories");
        }

        CategoryDto updated = categoryServiceImpl.update(id, request);
        log.info("Category updated successfully: {}", updated.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("Attempting to delete category with id: {}", id);

        if (currentUser == null) {
            log.error("User is not authenticated");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        if (currentUser.getUserType() != UserType.ADMIN) {
            log.error("Access denied: Only admins can delete categories");
            throw new AccessDeniedException("Only admins can delete categories");
        }

        try {
            categoryServiceImpl.deleteById(id);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        return ResponseEntity.noContent().build();
    }
}