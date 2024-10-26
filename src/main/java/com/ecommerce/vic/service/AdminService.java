package com.ecommerce.vic.service;

import com.ecommerce.vic.dto.order.OrderResponse;
import com.ecommerce.vic.dto.report.InventoryReportResponse;
import com.ecommerce.vic.dto.report.ProductInventoryInfo;
import com.ecommerce.vic.dto.report.SalesReportResponse;
import com.ecommerce.vic.dto.user.UserResponse;
import com.ecommerce.vic.model.Order;
import com.ecommerce.vic.model.OrderItem;
import com.ecommerce.vic.model.Product;
import com.ecommerce.vic.model.User;
import com.ecommerce.vic.constants.OrderStatus;
import com.ecommerce.vic.exception.InvalidOperationException;
import com.ecommerce.vic.exception.ResourceNotFoundException;
import com.ecommerce.vic.mapper.OrderMapper;
import com.ecommerce.vic.mapper.UserMapper;
import com.ecommerce.vic.repository.OrderRepository;
import com.ecommerce.vic.repository.ProductRepository;
import com.ecommerce.vic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    public Page<OrderResponse> getAllOrders(int page, int size) {
        Page<Order> orders = orderRepository.findAll(PageRequest.of(page, size));
        return orders.map(orderMapper::toOrderResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        
        // If order is cancelled, restore product stock
        if (newStatus == OrderStatus.CANCELLED) {
            restoreProductStock(order);
        }

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponse(savedOrder);
    }

    public Page<UserResponse> getAllUsers(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));
        return users.map(userMapper::toUserResponse);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if user has any active orders
        boolean hasActiveOrders = orderRepository.existsByUserAndStatusNot(user, OrderStatus.COMPLETED);
        if (hasActiveOrders) {
            throw new InvalidOperationException("Cannot delete user with active orders");
        }

        userRepository.delete(user);
    }

    public SalesReportResponse generateSalesReport(LocalDate startDate, LocalDate endDate) {
        List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatus(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                OrderStatus.COMPLETED
        );

        BigDecimal totalRevenue = calculateTotalRevenue(orders);
        Map<String, Long> productsSold = calculateProductsSold(orders);
        long totalOrders = orders.size();
        BigDecimal averageOrderValue = totalOrders > 0 
            ? totalRevenue.divide(BigDecimal.valueOf(totalOrders)).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new SalesReportResponse(
                startDate,
                endDate,
                totalRevenue,
                totalOrders,
                averageOrderValue,
                productsSold
        );
    }

    public InventoryReportResponse generateInventoryReport() {
        List<Product> products = productRepository.findAll();
        
        List<ProductInventoryInfo> inventoryInfo = products.stream()
                .map(product -> new ProductInventoryInfo(
                        product.getProductId(),
                        product.getName(),
                        product.getStockQuantity(),
                        product.getPrice(),
                        calculateReorderNeeded(product.getStockQuantity())
                ))
                .collect(Collectors.toList());

        return new InventoryReportResponse(
                LocalDate.now(),
                inventoryInfo,
                calculateTotalInventoryValue(products),
                calculateLowStockItems(products)
        );
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot update status of a " + currentStatus + " order");
        }

        if (currentStatus == OrderStatus.PROCESSING && newStatus == OrderStatus.PENDING) {
            throw new InvalidOperationException("Cannot move order from PROCESSING back to PENDING");
        }
    }

    private void restoreProductStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }
    }

    private BigDecimal calculateTotalRevenue(List<Order> orders) {
        return orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Long> calculateProductsSold(List<Order> orders) {
        return orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getName(),
                        Collectors.summingLong(OrderItem::getQuantity)
                ));
    }

    private boolean calculateReorderNeeded(int currentStock) {
        final int REORDER_THRESHOLD = 10;
        return currentStock <= REORDER_THRESHOLD;
    }

    private BigDecimal calculateTotalInventoryValue(List<Product> products) {
        return products.stream()
                .map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long calculateLowStockItems(List<Product> products) {
        final int LOW_STOCK_THRESHOLD = 5;
        return products.stream()
                .filter(product -> product.getStockQuantity() <= LOW_STOCK_THRESHOLD)
                .count();
    }
}
