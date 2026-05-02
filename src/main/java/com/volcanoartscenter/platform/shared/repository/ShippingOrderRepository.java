package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.ShippingOrder;
import com.volcanoartscenter.platform.shared.model.User;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.EntityGraph;
=======
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ShippingOrderRepository extends JpaRepository<ShippingOrder, Long> {
    Optional<ShippingOrder> findByOrderReference(String orderReference);
<<<<<<< HEAD
    @EntityGraph(attributePaths = {"user", "product", "orderItems", "orderItems.product"})
    List<ShippingOrder> findByUserOrderByCreatedAtDesc(User user);
    Optional<ShippingOrder> findByPaymentTransactionId(String paymentTransactionId);
    @Override
    @EntityGraph(attributePaths = {"user", "product", "orderItems", "orderItems.product"})
    List<ShippingOrder> findAll();
=======
    List<ShippingOrder> findByUserOrderByCreatedAtDesc(User user);
    Optional<ShippingOrder> findByPaymentTransactionId(String paymentTransactionId);
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac

    /**
     * Review eligibility check: has this user received delivery of this product?
     * Checks both the legacy single-product FK and the new order_items table.
     */
<<<<<<< HEAD
    @Query("SELECT COUNT(o) > 0 FROM ShippingOrder o LEFT JOIN o.orderItems oi " +
           "WHERE o.user.id = :userId " +
           "AND ((o.product IS NOT NULL AND o.product.id = :productId) OR oi.product.id = :productId) " +
           "AND o.status = com.volcanoartscenter.platform.shared.model.ShippingOrder.OrderStatus.DELIVERED")
    boolean hasDeliveredProduct(Long userId, Long productId);
}
=======
    @Query("SELECT COUNT(o) > 0 FROM ShippingOrder o " +
           "WHERE o.user.id = :userId AND o.product.id = :productId " +
           "AND o.status = com.volcanoartscenter.platform.shared.model.ShippingOrder.OrderStatus.DELIVERED")
    boolean hasDeliveredProduct(Long userId, Long productId);
}

>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
