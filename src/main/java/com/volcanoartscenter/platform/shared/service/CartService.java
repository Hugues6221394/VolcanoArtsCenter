package com.volcanoartscenter.platform.shared.service;

import com.volcanoartscenter.platform.shared.model.Cart;
import com.volcanoartscenter.platform.shared.model.CartItem;
import com.volcanoartscenter.platform.shared.model.Product;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.CartItemRepository;
import com.volcanoartscenter.platform.shared.repository.CartRepository;
import com.volcanoartscenter.platform.shared.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public Cart getOrCreateCart(User user, String anonSessionId) {
        if (user != null) {
            return cartRepository.findByUser(user).orElseGet(() -> 
                cartRepository.save(Cart.builder().user(user).build())
            );
        } else if (anonSessionId != null) {
            return cartRepository.findByAnonSessionId(anonSessionId).orElseGet(() -> 
                cartRepository.save(Cart.builder().anonSessionId(anonSessionId).build())
            );
        }
        throw new IllegalArgumentException("Must provide either user or anonymous session ID");
    }

    @Transactional
    public void addItemToCart(User user, String anonSessionId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(user, anonSessionId);
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (!product.isInStock()) {
            throw new IllegalStateException("Product is out of stock or unavailable.");
        }

        // Reservation Engine: Lock the item (15 min TTL) handled by CartReservationSweeper
        if (product.getInventoryType() == Product.InventoryType.UNIQUE) {
            if (product.getReservedQuantity() == null) product.setReservedQuantity(0);
            if (product.getReservedQuantity() > 0) {
                throw new IllegalStateException("This unique artwork is currently reserved by another buyer. Check back in 15 minutes.");
            }
            product.setReservedQuantity(1);
            product.setReservedUntil(LocalDateTime.now().plusMinutes(15));
            productRepository.save(product);
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .build();
            cartItemRepository.save(newItem);
        }
    }

    @Transactional
    public void removeItemFromCart(User user, String anonSessionId, Long productId) {
        Cart cart = getOrCreateCart(user, anonSessionId);
        Product product = productRepository.findById(productId).orElseThrow();
        
        cartItemRepository.findByCartAndProduct(cart, product).ifPresent(item -> {
            cartItemRepository.delete(item);
            
            // Release reservation if it's a unique item
            if (product.getInventoryType() == Product.InventoryType.UNIQUE) {
                product.setReservedQuantity(0);
                product.setReservedUntil(null);
                productRepository.save(product);
            }
        });
    }

    @Transactional
    public void clearCart(User user, String anonSessionId) {
        Cart cart = getOrCreateCart(user, anonSessionId);
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.getInventoryType() == Product.InventoryType.UNIQUE) {
                product.setReservedQuantity(0);
                product.setReservedUntil(null);
                productRepository.save(product);
            }
        }
        cartItemRepository.deleteAll(cart.getItems());
    }
}
