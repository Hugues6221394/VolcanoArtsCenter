package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.ShippingOrder;
import com.volcanoartscenter.platform.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ShippingOrderRepository extends JpaRepository<ShippingOrder, Long> {
    Optional<ShippingOrder> findByOrderReference(String orderReference);
    List<ShippingOrder> findByUserOrderByCreatedAtDesc(User user);
    Optional<ShippingOrder> findByPaymentTransactionId(String paymentTransactionId);

    /**
     * Review eligibility check: has this user received delivery of this product?
     * Checks both the legacy single-product FK and the new order_items table.
     */
    @Query("SELECT COUNT(o) > 0 FROM ShippingOrder o " +
           "WHERE o.user.id = :userId AND o.product.id = :productId " +
           "AND o.status = com.volcanoartscenter.platform.shared.model.ShippingOrder.OrderStatus.DELIVERED")
    boolean hasDeliveredProduct(Long userId, Long productId);
}

