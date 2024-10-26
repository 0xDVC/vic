package com.ecommerce.vic.repository;

import com.ecommerce.vic.dto.order.OrderStats;
import com.ecommerce.vic.model.Order;
import com.ecommerce.vic.model.User;
import com.ecommerce.vic.constants.OrderStatus;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByUserAndStatus(User user, OrderStatus status, Pageable pageable);
    boolean existsByUserAndStatusNot(User user, OrderStatus status);
    List<Order> findByCreatedAtBetweenAndStatus(
            LocalDateTime startDate,
            LocalDateTime endDate,
            OrderStatus status
    );
    long countByStatus(OrderStatus status);
    @Query("""
        SELECT new com.ecommerce.vic.dto.order.OrderStats(
            COUNT(o),
            SUM(o.totalAmount),
            AVG(o.totalAmount)
        )
        FROM Order o
        WHERE o.status = :status
        AND o.createdAt BETWEEN :startDate AND :endDate
    """)
    OrderStats getOrderStats(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Find recent orders for a user
    @Query("SELECT o FROM Order o WHERE o.user = :user ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(@Param("user") User user, Pageable pageable);

    // Search orders by order number or user email (for admin)
    @Query("""
        SELECT o FROM Order o
        JOIN o.user u
        WHERE LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """)
    Page<Order> searchOrders(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Get orders requiring attention (pending for too long)
    @Query("""
        SELECT o FROM Order o
        WHERE o.status = 'PENDING'
        AND o.createdAt < :threshold
        ORDER BY o.createdAt ASC
    """)
    List<Order> findOrdersRequiringAttention(@Param("threshold") LocalDateTime threshold);

//    @Query("SELECT o FROM Order o WHERE o.user.id = ?#{principal.id} ORDER BY o.createdAt DESC")
//    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
