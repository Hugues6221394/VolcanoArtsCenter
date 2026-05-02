package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Cart;
import com.volcanoartscenter.platform.shared.model.User;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.EntityGraph;
=======
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
<<<<<<< HEAD
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUser(User user);
    @EntityGraph(attributePaths = {"items", "items.product"})
=======
    Optional<Cart> findByUser(User user);
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
    Optional<Cart> findByAnonSessionId(String anonSessionId);
}
