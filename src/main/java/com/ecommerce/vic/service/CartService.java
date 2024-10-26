package com.ecommerce.vic.service;

import com.ecommerce.vic.dto.cart.*;
import com.ecommerce.vic.exception.ResourceNotFoundException;
import com.ecommerce.vic.exception.InvalidOperationException;
import com.ecommerce.vic.model.*;
import com.ecommerce.vic.repository.CartRepository;
import com.ecommerce.vic.repository.ProductRepository;
import com.ecommerce.vic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart() {
        Cart cart = getOrCreateCart();
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        Cart cart = getOrCreateCart();
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if product is already in cart
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(request.productId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Update quantity if product already exists
            int newQuantity = existingItem.getQuantity() + request.quantity();
            validateStock(product, newQuantity);
            existingItem.setQuantity(newQuantity);
            existingItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            // Add new item if product doesn't exist in cart
            validateStock(product, request.quantity());
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.quantity())
                    .unitPrice(product.getPrice())
                    .subtotal(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())))
                    .build();
            cart.getItems().add(newItem);
        }

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return mapToCartResponse(savedCart);
    }

    @Transactional
    public CartResponse updateCartItem(Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart();
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        validateStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(request.quantity())));

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return mapToCartResponse(savedCart);
    }

    @Transactional
    public CartResponse removeFromCart(Long itemId) {
        Cart cart = getOrCreateCart();
        cart.getItems().removeIf(item -> item.getId().equals(itemId));

        updateCartTotals(cart);
        Cart savedCart = cartRepository.save(cart);
        return mapToCartResponse(savedCart);
    }

    @Transactional
    public void clearCart() {
        Cart cart = getOrCreateCart();
        cart.getItems().clear();
        updateCartTotals(cart);
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart() {
        User currentUser = getCurrentUser();
        return cartRepository.findByUser(currentUser)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(currentUser)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validateStock(Product product, int requestedQuantity) {
        if (product.getStockQuantity() < requestedQuantity) {
            throw new InvalidOperationException(
                    "Requested quantity exceeds available stock for product: " + product.getName());
        }
    }

    private void updateCartTotals(Cart cart) {
        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        cart.setSubtotal(subtotal);
        cart.setTotalItems(totalItems);
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> new CartItemResponse(
                        item.getId(),
                        item.getProduct().getProductId(),
                        item.getProduct().getName(),
                        item.getProduct().getImageUrl(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());

        return new CartResponse(
                cart.getId(),
                items,
                cart.getSubtotal(),
                cart.getTotalItems()
        );
    }
}
