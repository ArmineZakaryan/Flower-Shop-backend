package org.example.flowershop.service;

import org.example.flowershop.dto.ProductDto;
import org.example.flowershop.dto.SaveProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
    Page<ProductDto> findAll(Pageable pageable);

    ProductDto findById(Long id);

    ProductDto findByName(String name);

    ProductDto save(SaveProductRequest request, long userId, MultipartFile image);

    ProductDto update(Long productId, SaveProductRequest request, MultipartFile image, long userId);

    void deleteById(Long productId, long userId);

    byte[] getImage(String imageName);
}
