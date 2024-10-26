package com.ecommerce.vic.mapper;

import com.ecommerce.vic.dto.order.OrderItemResponse;
import com.ecommerce.vic.dto.order.OrderResponse;
import com.ecommerce.vic.model.Order;
import com.ecommerce.vic.model.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMapper {
    private final UserMapper userMapper;

    public OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                userMapper.toPartialUserResponse(order.getUser()),
                mapOrderItems(order.getOrderItems()),
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingAddress(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private List<OrderItemResponse> mapOrderItems(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getProductId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());
    }
}
