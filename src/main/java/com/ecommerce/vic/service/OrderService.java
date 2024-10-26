package com.ecommerce.vic.service;

import com.ecommerce.vic.constants.OrderStatus;
import com.ecommerce.vic.dto.order.CreateOrderRequest;
import com.ecommerce.vic.dto.order.OrderItemResponse;
import com.ecommerce.vic.dto.order.OrderResponse;
import com.ecommerce.vic.dto.user.PartialUserResponse;
import com.ecommerce.vic.exception.ResourceNotFoundException;
import com.ecommerce.vic.exception.InvalidOperationException;
import com.ecommerce.vic.model.*;
import com.ecommerce.vic.repository.OrderRepository;
import com.ecommerce.vic.repository.ProductRepository;
import com.ecommerce.vic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User currentUser = getCurrentUser();

        // Process order items
        List<OrderItem> orderItems = request.items().stream()
                .map(item -> {
                    Product product = productRepository.findById(item.productId())
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.productId()));

                    if (product.getStockQuantity() < item.quantity()) {
                        throw new InvalidOperationException("Insufficient stock for product: " + product.getName());
                    }

                    // Update stock
                    product.setStockQuantity(product.getStockQuantity() - item.quantity());
                    productRepository.save(product);

                    return OrderItem.builder()
                            .product(product)
                            .quantity(item.quantity())
                            .unitPrice(product.getPrice())
                            .subtotal(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())))
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal total = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .user(currentUser)
                .orderItems(orderItems)
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.shippingAddress())
                .createdAt(LocalDateTime.now())
                .build();

        orderItems.forEach(item -> item.setOrder(order));

        Order savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(int page, int size) {
        User currentUser = getCurrentUser();
        return orderRepository.findByUserOrderByCreatedAtDesc(currentUser, PageRequest.of(page, size))
                .map(this::mapToOrderResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        Order order = findOrderAndVerifyAccess(id);
        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = findOrderAndVerifyAccess(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("Only pending orders can be cancelled");
        }

        // Restore product stock
        order.getOrderItems().forEach(item -> {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        });

        order.setStatus(OrderStatus.CANCELLED);
        return mapToOrderResponse(orderRepository.save(order));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Order findOrderAndVerifyAccess(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        User currentUser = getCurrentUser();
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new InvalidOperationException("Not authorized to access this order");
        }

        return order;
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getProductId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());
        PartialUserResponse userResponse = new PartialUserResponse(
                order.getUser().getUserId(),
                order.getUser().getEmail(),
                order.getUser().getFullName(),
                order.getUser().getPhone()
        );

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                userResponse,
                items,
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingAddress(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
