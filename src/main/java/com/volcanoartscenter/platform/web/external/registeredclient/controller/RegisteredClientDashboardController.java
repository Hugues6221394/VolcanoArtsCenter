package com.volcanoartscenter.platform.web.external.registeredclient.controller;

import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.web.external.registeredclient.service.RegisteredClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RegisteredClientDashboardController {

    private final RegisteredClientService registeredClientService;

    @GetMapping("/client/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
<<<<<<< HEAD
        model.addAttribute("currentPage", "client-dashboard");
=======
        model.addAttribute("currentPage", "art-store");
>>>>>>> f8e8bc756db02040ef57e12be3260849005b05ac
        model.addAttribute("pageTitle", "Registered Client Dashboard");
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("user", user);
        model.addAttribute("orders", registeredClientService.ordersForUser(user));
        model.addAttribute("bookings", registeredClientService.bookingsForUser(user));
        model.addAttribute("donations", registeredClientService.donationsForUser(user));
        model.addAttribute("reviews", registeredClientService.reviewsForUser(user));
        model.addAttribute("savedItems", registeredClientService.savedItemsForUser(user));
        return "external/registered-client/dashboard";
    }

    @PostMapping("/client/profile")
    public String updateProfile(Authentication authentication,
                                @RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String country,
                                RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (user == null) {
            return "redirect:/login";
        }
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setCountry(country);
        registeredClientService.saveUserProfile(user);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
        return "redirect:/client/dashboard";
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return registeredClientService.findUserByEmail(authentication.getName()).orElse(null);
    }
}
