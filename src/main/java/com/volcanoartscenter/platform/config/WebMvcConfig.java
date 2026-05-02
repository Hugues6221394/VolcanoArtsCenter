package com.volcanoartscenter.platform.config;

import com.volcanoartscenter.platform.security.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Protect high-value and sensitive processing paths from floods/abuse.
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/webhooks/**")
                .addPathPatterns("/cart/checkout")
                .addPathPatterns("/api/auth/**");
    }
}
