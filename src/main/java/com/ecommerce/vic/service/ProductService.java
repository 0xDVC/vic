package com.ecommerce.vic.service;

import com.ecommerce.vic.dto.product.ProductRequest;
import com.ecommerce.vic.dto.product.ProductResponse;
import com.ecommerce.vic.exception.InsufficientStockException;
import com.ecommerce.vic.exception.ResourceNotFoundException;
import com.ecommerce.vic.exception.UnauthorizedException;
import com.ecommerce.vic.model.Product;
import com.ecommerce.vic.model.User;
import com.ecommerce.vic.constants.UserRole;
import com.ecommerce.vic.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final UserService userService;

    public Page<ProductResponse> getAllProducts(String category, int page, int size, String sort) {
        Sort sorting = createSort(sort);
        Pageable pageable = PageRequest.of(page, size, sorting);

        Page<Product> products = category == null ?
                productRepository.findAll(pageable) :
                productRepository.findByCategory(category, pageable);

        return products.map(this::mapToResponse);
    }

    public ProductResponse getProduct(Long id) {
        return productRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public ProductResponse createProduct(ProductRequest request) {
        User admin = userService.getCurrentUser();
        validateAdminRole(admin);

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .imageUrl(request.imageUrl())
                .category(request.category())
                .size(request.size())
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();

        return mapToResponse(productRepository.save(product));
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        User admin = userService.getCurrentUser();
        validateAdminRole(admin);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
        product.setCategory(request.category());
        product.setSize(request.size());
        product.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        User admin = userService.getCurrentUser();
        validateAdminRole(admin);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        productRepository.delete(product);
    }

    public ProductResponse updateStock(Long id, Integer quantity) {
        User admin = userService.getCurrentUser();
        validateAdminRole(admin);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        int newQuantity = product.getStockQuantity() + quantity;
        if (newQuantity < 0) {
            throw new InsufficientStockException("Insufficient stock");
        }

        product.setStockQuantity(newQuantity);
        product.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(productRepository.save(product));
    }

    public Page<ProductResponse> searchProducts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.searchProducts(query, pageable)
                .map(this::mapToResponse);
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getCategory(),
                product.getSize(),
                product.getAdmin().getFirstName() + " " + product.getAdmin().getLastName(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private void validateAdminRole(User user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Only administrators can perform this operation");
        }
    }

    private Sort createSort(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",");
        String field = parts[0];
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        return Sort.by(direction, field);
    }
}
