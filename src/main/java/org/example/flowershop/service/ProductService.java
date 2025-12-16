package org.example.flowershop.service;

import jakarta.validation.Valid;
import org.example.flowershop.dto.ProductDto;
import org.example.flowershop.dto.SaveProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    Page<ProductDto> findAll(Pageable pageable);

    ProductDto findById(long id);

    ProductDto findByName(String name);


    ProductDto save(@Valid SaveProductRequest request, Long userId);

    ProductDto update(Long productId, @Valid SaveProductRequest request, Long userId);

    void deleteById(Long productId, Long userId);

    byte[] getImage(String imageName);
}
