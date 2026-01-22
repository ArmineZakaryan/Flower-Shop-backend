package org.example.flowershop.endpoint;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.ProductDto;
import org.example.flowershop.dto.SaveProductRequest;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLConnection;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
@Slf4j
public class ProductEndpoint {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductDto>> getAllProducts(Pageable pageable) {
        log.info("GET /products {}", pageable);
        return ResponseEntity.ok(productService.findAll(pageable));
    }

    @GetMapping("/{name}")
    public ResponseEntity<ProductDto> getProductByName(@PathVariable String name) {
        log.info("GET /products/{}", name);
        return ResponseEntity.ok(productService.findByName(name));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDto> create(
            @ModelAttribute SaveProductRequest request,
            @RequestPart MultipartFile image,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("POST /products userId={}", currentUser.getId());

        ProductDto created =
                productService.save(request, currentUser.getId(), image);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDto> update(
            @PathVariable long id,
            @ModelAttribute SaveProductRequest request,
            @RequestPart(required = false) MultipartFile image,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("PUT /products/{} userId={}", id, currentUser.getId());

        return ResponseEntity.ok(
                productService.update(id, request, image, currentUser.getId())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable long id,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("DELETE /products/{} userId={}", id, currentUser.getId());

        productService.deleteById(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/img/{imageName}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> getImage(@PathVariable String imageName) {
        log.info("Fetching image with name: {}", imageName);

        byte[] imageData = productService.getImage(imageName);

        if (imageData == null || imageData.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Image not found: " + imageName);
        }

        String mimeType = URLConnection.guessContentTypeFromName(imageName);
        if (mimeType == null) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        log.info("Successfully fetched image with name: {}", imageName);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .body(imageData);
    }
}