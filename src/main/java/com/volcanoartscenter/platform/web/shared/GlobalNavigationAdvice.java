package com.volcanoartscenter.platform.web.shared;

import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalNavigationAdvice {

    private final UserRepository userRepository;

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName());
    }

    @ModelAttribute("navContext")
    public NavContext navContext(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return NavContext.guest();
        }
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return NavContext.guest();
        }
        Set<String> roles = user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet());
        String userName = user.getFullName();
        if (roles.contains("SUPER_ADMIN") || roles.contains("ADMIN")) {
            return new NavContext(true, "Super Admin", "/admin/dashboard", userName,
                    true, false, false, false, false, false);
        }
        if (roles.contains("CONTENT_MANAGER")) {
            return new NavContext(true, "Content Manager", "/admin/content/products", userName,
                    false, true, false, false, false, false);
        }
        if (roles.contains("OPS_MANAGER")) {
            return new NavContext(true, "Ops Manager", "/admin/ops/bookings", userName,
                    false, false, true, false, false, false);
        }
        if (roles.contains("TOUR_OPERATOR")) {
            return new NavContext(true, "Tour Operator", "/tour-operators/portal", userName,
                    false, false, false, true, false, false);
        }
        if (roles.contains("TALENT_APPLICANT")) {
            return new NavContext(true, "Talent Applicant", "/talent/dashboard", userName,
                    false, false, false, false, true, false);
        }
        if (roles.contains("REGISTERED_CLIENT")) {
            return new NavContext(true, "Registered Client", "/client/dashboard", userName,
                    false, false, false, false, false, true);
        }
        return NavContext.guest();
    }

    public record NavContext(
            boolean authenticated,
            String roleLabel,
            String dashboardUrl,
            String userName,
            boolean superAdmin,
            boolean contentManager,
            boolean opsManager,
            boolean tourOperator,
            boolean talentApplicant,
            boolean registeredClient
    ) {
        static NavContext guest() {
            return new NavContext(false, "Guest", "/login", "Guest", false, false, false, false, false, false);
        }
    }
}

