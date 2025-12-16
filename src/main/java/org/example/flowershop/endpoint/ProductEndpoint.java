package org.example.flowershop.endpoint;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.ProductDto;
import org.example.flowershop.dto.SaveProductRequest;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
@Slf4j
public class ProductEndpoint {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductDto>> getAllProducts(Pageable pageable) {
        log.info("Received request to fetch products with pagination and sorting. Pageable: {}", pageable);

        Page<ProductDto> productDto = productService.findAll(pageable);

        log.info("Successfully fetched {} products with pagination and sorting.", productDto.getTotalElements());

        return ResponseEntity.ok(productDto);
    }

    @GetMapping("/{name}")
    public ResponseEntity<ProductDto> getProductByName(@PathVariable String name) {
        log.info("Fetching product with name: {}", name);

        ProductDto product = productService.findByName(name);

        log.info("Successfully fetched product with name: {}", name);

        return ResponseEntity.ok(product);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDto> create(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") double price,
            @RequestParam("categoryId") long categoryId,
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal(expression = "user") User currentUser) throws IOException {

        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        if (currentUser.getUserType() != UserType.ADMIN) {
            log.warn("User with id: {} attempted to create a product without admin rights", currentUser.getId());
            throw new AccessDeniedException("Only admins can create products");
        }

        log.info("User with id: {} is creating a new product", currentUser.getId());

        String imageUrl = saveImage(image);
        SaveProductRequest request = new SaveProductRequest(name, description, price, imageUrl, categoryId);
        ProductDto created = productService.save(request, currentUser.getId());

        log.info("Successfully created product with id: {}", created.getId());

        return ResponseEntity.status(201).body(created);
    }

    private String saveImage(MultipartFile image) throws IOException {
        String uploadDir = "C:\\JavaInter\\flowerShopParent\\images";
        String imageName = UUID.randomUUID() + "_" + image.getOriginalFilename();
        Path imagePath = Paths.get(uploadDir, imageName);

        Files.copy(image.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);

        return "/images/" + imageName;
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDto> update(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") double price,
            @RequestParam("categoryId") long categoryId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal(expression = "user") User currentUser) throws IOException {

        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        if (currentUser.getUserType() != UserType.ADMIN) {
            log.warn("User with id: {} attempted to update product with id: {} without admin rights", currentUser.getId(), id);
            throw new AccessDeniedException("Only admins can update products");
        }

        log.info("User with id: {} is updating product with id: {}", currentUser.getId(), id);

        ProductDto existingProduct = productService.findById(id);
        if (existingProduct == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        String imageUrl = existingProduct.getImage();
        if (image != null && !image.isEmpty()) {
            imageUrl = saveImage(image);
        }

        SaveProductRequest request = new SaveProductRequest(name, description, price, imageUrl, categoryId);
        ProductDto updated = productService.update(id, request, currentUser.getId());
        log.info("Successfully updated product with id: {}", id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        if (currentUser.getUserType() != UserType.ADMIN) {
            log.warn("User with id: {} attempted to delete product with id: {} without admin rights", currentUser.getId(), id);
            throw new AccessDeniedException("Only admins can delete products");
        }

        log.info("User with id: {} is attempting to delete product with id: {}", currentUser.getId(), id);

        try {
            productService.deleteById(id, currentUser.getId());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        log.info("Successfully deleted product with id: {}", id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping(value = "/img/{imageName}", produces = MediaType.IMAGE_PNG_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> getImage(@PathVariable String imageName) {
        log.info("Fetching image with name: {}", imageName);

        try {
            byte[] imageData = productService.getImage(imageName);

            log.info("Successfully fetched image with name: {}", imageName);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageData);
        } catch (RuntimeException ex) {
            log.error("Failed to fetch image with name: {}", imageName, ex);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ex.getMessage(),
                    ex
            );
        }
    }
}