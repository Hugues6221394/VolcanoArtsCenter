package com.volcanoartscenter.platform.web.external.registeredclient.controller;

import com.volcanoartscenter.platform.shared.model.Product;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.service.CaptchaService;
import com.volcanoartscenter.platform.web.external.registeredclient.service.RegisteredClientService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final RegisteredClientService registeredClientService;
    private final CaptchaService captchaService;

    private Map<Long, Integer> getCart(HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model, Authentication authentication) {
        Map<Long, Integer> cart = getCart(session);
        Map<Product, Integer> cartItems = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Product p = registeredClientService.listProducts(null, null, null, null)
                    .stream()
                    .filter(prod -> prod.getId().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);
            if (p != null) {
                cartItems.put(p, entry.getValue());
                total = total.add(p.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
            }
        }

        model.addAttribute("currentPage", "cart");
        model.addAttribute("pageTitle", "Shopping Cart — Volcano Arts Center");
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", total);
        
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
        model.addAttribute("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            User user = registeredClientService.findUserByEmail(authentication.getName()).orElse(null);
            model.addAttribute("currentUser", user);
        }

        return "external/registered-client/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Map<Long, Integer> cart = getCart(session);
        cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
        redirectAttributes.addFlashAttribute("successMessage", "Item added to your cart.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId, HttpSession session) {
        Map<Long, Integer> cart = getCart(session);
        cart.remove(productId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/checkout")
    public String checkoutCart(@RequestParam String recipientName,
                               @RequestParam String recipientEmail,
                               @RequestParam(required = false) String recipientPhone,
                               @RequestParam String addressLine1,
                               @RequestParam(required = false) String addressLine2,
                               @RequestParam String city,
                               @RequestParam(required = false) String state,
                               @RequestParam(required = false) String postalCode,
                               @RequestParam String country,
                               @RequestParam String paymentMethod,
                               @RequestParam(required = false) String captchaToken,
                               HttpSession session,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (!captchaService.verify(captchaToken)) {
            redirectAttributes.addFlashAttribute("successMessage", "Captcha validation failed.");
            return "redirect:/cart";
        }

        Map<Long, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("successMessage", "Your cart is empty.");
            return "redirect:/cart";
        }

        User user = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            user = registeredClientService.findUserByEmail(authentication.getName()).orElse(null);
        } else {
            // Force login exactly as art-store requires if strict policy applies.
            redirectAttributes.addFlashAttribute("successMessage", "Please register or sign in to complete checkout.");
            return "redirect:/login";
        }

        String cartOrderReference = "CART-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Product product = registeredClientService.listProducts(null, null, null, null)
                    .stream()
                    .filter(prod -> prod.getId().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);

            if (product != null) {
                registeredClientService.createShippingOrder(
                        product,
                        user,
                        recipientName,
                        recipientEmail,
                        recipientPhone,
                        addressLine1,
                        addressLine2,
                        city,
                        state,
                        postalCode,
                        country,
                        entry.getValue(),
                        paymentMethod
                );
            }
        }

        cart.clear();
        redirectAttributes.addFlashAttribute("successMessage", "Cart Checkout successful. Our team will contact you with details soon.");
        return "redirect:/client/dashboard";
    }
}
