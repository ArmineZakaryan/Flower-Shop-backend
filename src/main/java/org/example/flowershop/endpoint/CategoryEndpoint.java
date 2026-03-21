package org.example.flowershop.endpoint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.CategoryDto;
import org.example.flowershop.dto.SaveCategoryRequest;
import org.example.flowershop.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
@Slf4j
public class CategoryEndpoint {
    private final CategoryService categoryService;


    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {

        Sort sort = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CategoryDto> categoriesPage = categoryService.findAll(pageable);

        return ResponseEntity.ok(categoriesPage.getContent());
    }


    @GetMapping("/{name}")
    public ResponseEntity<CategoryDto> getCategoryByName(@PathVariable String name) {
        return ResponseEntity.ok(categoryService.findByName(name));
    }

    @PostMapping
    public ResponseEntity<CategoryDto> create(
            @RequestBody @Valid SaveCategoryRequest request) {

        CategoryDto created = categoryService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> update(
            @PathVariable Long id,
            @RequestBody @Valid SaveCategoryRequest request) {

        CategoryDto updated = categoryService.update(id, request);
        return ResponseEntity.ok(updated);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable long id) {

        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}