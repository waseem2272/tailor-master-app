package com.example.tailormaster.config;

import com.example.tailormaster.filter.LoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor())
                .addPathPatterns("/**").excludePathPatterns(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/font/**",
                        "/favicon.ico"
                ); // Intercept all endpoints
    }
}
