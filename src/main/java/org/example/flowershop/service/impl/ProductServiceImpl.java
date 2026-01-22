package org.example.flowershop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.ProductDto;
import org.example.flowershop.dto.SaveProductRequest;
import org.example.flowershop.exception.CategoryNotFoundException;
import org.example.flowershop.exception.ImageNotFoundException;
import org.example.flowershop.exception.ImageReadException;
import org.example.flowershop.exception.ImageStorageException;
import org.example.flowershop.exception.ProductAlreadyExistsException;
import org.example.flowershop.exception.ProductHasRelationsException;
import org.example.flowershop.exception.ProductNotFoundException;
import org.example.flowershop.exception.UserNotFoundException;
import org.example.flowershop.mapper.ProductMapper;
import org.example.flowershop.model.entity.Category;
import org.example.flowershop.model.entity.Product;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.repository.CategoryRepository;
import org.example.flowershop.repository.ProductRepository;
import org.example.flowershop.repository.UserRepository;
import org.example.flowershop.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Value("${images.upload.path}")
    String imageUploadPath;


    @Override
    public ProductDto save(SaveProductRequest request, long userId, MultipartFile image) {

        log.info("Create product request started by userId={}", userId);

        if (request == null) {
            log.warn("Create product failed: request is null (userId={})", userId);
            throw new IllegalArgumentException("Product request must not be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Create product failed: user not found (userId={})", userId);
                    return new UserNotFoundException("User not found");
                });

        if (user.getUserType() != UserType.ADMIN) {
            log.warn("Create product denied: userId={} is not ADMIN", userId);
            throw new AccessDeniedException("Only admins can create products");
        }

        productRepository.findByName(request.getName())
                .ifPresent(p -> {
                    log.warn("Create product failed: product already exists (name={})", request.getName());
                    throw new ProductAlreadyExistsException(
                            "Product already exists with name " + request.getName()
                    );
                });

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> {
                    log.warn("Create product failed: category not found (categoryId={})", request.getCategoryId());
                    return new CategoryNotFoundException("Category not found");
                });

        Product product = productMapper.toEntity(request);
        product.setUser(user);
        product.setCategory(category);

        if (image != null && !image.isEmpty()) {
            String imageName = saveImage(image);
            product.setImage(imageName);
            log.info("Product image saved with name={}", imageName);
        } else {
            log.info("Product created without image (name={})", request.getName());
        }

        Product saved = productRepository.save(product);

        log.info("Product successfully created id={} by userId={}", saved.getId(), userId);

        return productMapper.toDto(saved);
    }

    private String saveImage(MultipartFile image) {
        try {
            String imageName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path uploadDir = Path.of(imageUploadPath);
            Files.createDirectories(uploadDir);

            Path imagePath = uploadDir.resolve(imageName);
            Files.copy(image.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);

            return imageName;
        } catch (IOException e) {
            log.error("Failed to save image", e);
            throw new ImageStorageException("Failed to save image", e);
        }
    }

    @Override
    public ProductDto update(Long productId, SaveProductRequest request, MultipartFile image, long userId) {

        log.info("Updating product id={} by userId={}", productId, userId);

        if (request == null) {
            log.warn("Update product failed: request is null (userId={})", userId);
            throw new IllegalArgumentException("Product request must not be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getUserType() != UserType.ADMIN) {
            log.warn("User {} is not allowed to update products", userId);
            throw new AccessDeniedException("Only admins can update products");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found with id " + productId)
                );

        productRepository.findByName(request.getName())
                .filter(p -> !p.getId().equals(productId))
                .ifPresent(p -> {
                    throw new ProductAlreadyExistsException(
                            "Product already exists with name " + request.getName()
                    );
                });

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found")
                );

        log.info("Updating product fields for productId={}", productId);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(category);

        if (image != null && !image.isEmpty()) {
            log.info("Updating image for productId={}", productId);
            product.setImage(saveImage(image));
        }

        Product saved = productRepository.save(product);

        log.info("Product updated successfully id={}", saved.getId());
        return productMapper.toDto(saved);
    }


    @Override
    public void deleteById(Long productId, long userId) {
        log.info("User with id: {} is attempting to delete product with id: {}", userId, productId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getUserType() != UserType.ADMIN) {
            log.warn("User with id: {} is not authorized to delete product with id: {}", userId, productId);
            throw new AccessDeniedException("Only admins can delete products");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + productId));

        if (product.getOrderItem() != null && !product.getOrderItem().isEmpty()) {
            log.warn("Cannot delete product with id: {} because it has been ordered", productId);
            throw new ProductHasRelationsException("Cannot delete product that has been ordered");
        }
        if (product.getFavoriteItem() != null && !product.getFavoriteItem().isEmpty()) {
            log.warn("Cannot delete product with id: {} because it is in favorites", productId);
            throw new ProductHasRelationsException("Cannot delete product that has been added to favorites");
        }

        if (product.getCartItem() != null && !product.getCartItem().isEmpty()) {
            log.warn("Cannot delete product with id: {} because it is in cart", productId);
            throw new ProductHasRelationsException("Cannot delete product that is in the cart");
        }

        productRepository.delete(product);
        log.info("Product with id: {} successfully deleted", productId);
    }


    @Override
    public byte[] getImage(String imageName) {
        log.info("Fetching image with name: {}", imageName);

        Path basePath = Path.of(imageUploadPath).toAbsolutePath().normalize();
        Path imagePath = basePath.resolve(imageName).normalize();

        if (!imagePath.startsWith(basePath)) {
            log.warn("Path traversal attempt detected: {}", imageName);
            throw new ImageNotFoundException("Invalid image path");
        }

        if (!Files.exists(imagePath)) {
            log.warn("Image not found: {}", imageName);
            throw new ImageNotFoundException("Image not found");
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imagePath);
            log.info("Successfully fetched image: {}", imageName);
            return imageBytes;
        } catch (IOException e) {
            log.error("Failed to read image file: {}", imageName, e);
            throw new ImageReadException("Could not read image file", e);
        }
    }


    @Override
    public Page<ProductDto> findAll(Pageable pageable) {
        log.info("Fetching products with pagination and sorting. Pageable: {}", pageable);

        Page<Product> productsPage = productRepository.findAll(pageable);

        Page<ProductDto> productDto = productsPage.map(productMapper::toDto);

        log.info("Successfully fetched {} products with pagination and sorting.", productDto.getTotalElements());
        return productDto;
    }

    @Override
    public ProductDto findById(Long id) {
        log.info("Fetching product with id: {}", id);

        ProductDto product = productMapper.toDto(
                productRepository.findById(id)
                        .orElseThrow(() -> {
                            log.error("Product not found with id: {}", id);

                            return new ProductNotFoundException("Product not found with id " + id);
                        })
        );

        log.info("Successfully fetched product with id: {}", id);
        return product;
    }

    @Override
    public ProductDto findByName(String name) {
        log.info("Fetching product with name: {}", name);

        ProductDto product = productMapper.toDto(
                productRepository.findByName(name)
                        .orElseThrow(() -> {
                            log.error("Product not found with name: {}", name);
                            return new ProductNotFoundException("Product not found with name: " + name);
                        })
        );
        log.info("Successfully fetched product with name: {}", name);
        return product;
    }
}