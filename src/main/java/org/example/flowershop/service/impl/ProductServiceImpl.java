package org.example.flowershop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.ProductDto;
import org.example.flowershop.dto.SaveProductRequest;
import org.example.flowershop.exception.CategoryNotFoundException;
import org.example.flowershop.exception.ProductAlreadyExistsException;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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
    public ProductDto save(SaveProductRequest productRequest, Long userId) {
        log.info("User with id: {} is creating a new product", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getUserType() != UserType.ADMIN) {
            log.warn("User with id: {} is not authorized to create products", userId);
            throw new AccessDeniedException("Only admins can create products");
        }

        Optional<Product> existingProduct = productRepository.findByName(productRequest.getName());
        if (existingProduct.isPresent()) {
            log.warn("Product with name '{}' already exists with id: {}", productRequest.getName(), existingProduct.get().getId());
            throw new ProductAlreadyExistsException("Product with name '" + productRequest.getName() + "' already exists");
        }

        Product product = productMapper.toEntity(productRequest);
        product.setUser(user);

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> {
                    log.error("Category not found with id: {}", productRequest.getCategoryId());
                    return new CategoryNotFoundException("Category not found with id " + productRequest.getCategoryId());
                });
        product.setCategory(category);

        Product saved = productRepository.save(product);

        log.info("Product with id: {} successfully created", saved.getId());
        return productMapper.toDto(saved);
    }


    @Override
    public ProductDto update(Long productId, SaveProductRequest request, Long userId) {
        log.info("User with id: {} is attempting to update product with id: {}", userId, productId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getUserType() != UserType.ADMIN) {
            log.warn("User with id: {} is not authorized to update product with id: {}", userId, productId);
            throw new AccessDeniedException("Only admins can update products");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + productId));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());

        Product updated = productRepository.save(product);

        log.info("Product with id: {} successfully updated", productId);
        return productMapper.toDto(updated);
    }


    @Override
    public void deleteById(Long productId, Long userId) {
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
            throw new IllegalStateException("Cannot delete product that has been ordered");
        }
        if (product.getFavoriteItem() != null && !product.getFavoriteItem().isEmpty()) {
            log.warn("Cannot delete product with id: {} because it is in favorites", productId);
            throw new IllegalStateException("Cannot delete product that has been added to favorites");
        }

        if (product.getCartItem() != null && !product.getCartItem().isEmpty()) {
            log.warn("Cannot delete product with id: {} because it is in cart", productId);
            throw new IllegalStateException("Cannot delete product that is in the cart");
        }

        productRepository.delete(product);
        log.info("Product with id: {} successfully deleted", productId);
    }


    @Override
    public byte[] getImage(String imageName) {
        log.info("Fetching image with name: {}", imageName);

        try {
            Path basePath = Path.of(imageUploadPath).toAbsolutePath().normalize();
            Path imagePath = basePath.resolve(imageName).normalize();

            if (!imagePath.startsWith(basePath)) {
                log.error("Invalid image path: {} - path traversal detected", imageName);
                throw new RuntimeException("Invalid image path: " + imageName);
            }

            if (!Files.exists(imagePath)) {
                log.error("Image file not found: {}", imageName);
                throw new RuntimeException("Image file not found: " + imageName);
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);
            log.info("Successfully fetched image: {}", imageName);
            return imageBytes;
        } catch (IOException e) {
            log.error("Error reading image file: {}", imageName, e);
            throw new RuntimeException("Could not read image file: " + imageName, e);
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
    public ProductDto findById(long id) {
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