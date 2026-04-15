package com.volcanoartscenter.platform.security;

import com.volcanoartscenter.platform.shared.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public pages — accessible by everyone
                .requestMatchers(
                    "/", "/about", "/departments/**",
                    "/art-store/**", "/experiences/**",
                    "/conservation/**", "/talent/**",
                    "/blog/**", "/contact",
                    "/tour-operators/request", "/tour-operators/register",
                    "/talent/register",
                    "/css/**", "/js/**", "/images/**", "/fonts/**",
                    "/api/public/**"
                ).permitAll()

                // Internal role-based access
                .requestMatchers("/admin/dashboard", "/admin/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/admin/content/**").hasAnyRole("CONTENT_MANAGER", "SUPER_ADMIN", "ADMIN")
                .requestMatchers("/admin/ops/**").hasAnyRole("OPS_MANAGER", "SUPER_ADMIN", "ADMIN")
                .requestMatchers("/tour-operators/portal/**").hasRole("TOUR_OPERATOR")
                .requestMatchers("/talent/dashboard/**").hasRole("TALENT_APPLICANT")
                .requestMatchers("/client/**").hasRole("REGISTERED_CLIENT")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(roleBasedSuccessHandler())
                .usernameParameter("username")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logged_out")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByEmail(username)
                .map(user -> User.withUsername(user.getEmail())
                        .password(user.getPassword())
                        .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                        .authorities(
                                user.getRoles().stream()
                                        .map(role -> "ROLE_" + role.getName())
                                        .toArray(String[]::new)
                        )
                        .build()
                )
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isSuperAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
            boolean isContentManager = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_CONTENT_MANAGER".equals(a.getAuthority()));
            boolean isOpsManager = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_OPS_MANAGER".equals(a.getAuthority()));
            boolean isTourOperator = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_TOUR_OPERATOR".equals(a.getAuthority()));
            boolean isTalentApplicant = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_TALENT_APPLICANT".equals(a.getAuthority()));
            if (isSuperAdmin) {
                response.sendRedirect("/admin/dashboard");
            } else if (isContentManager) {
                response.sendRedirect("/admin/content/products");
            } else if (isOpsManager) {
                response.sendRedirect("/admin/ops/bookings");
            } else if (isTourOperator) {
                response.sendRedirect("/tour-operators/portal");
            } else if (isTalentApplicant) {
                response.sendRedirect("/talent/dashboard");
            } else if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_REGISTERED_CLIENT".equals(a.getAuthority()))) {
                response.sendRedirect("/client/dashboard");
            } else {
                response.sendRedirect("/");
            }
        };
    }
}
