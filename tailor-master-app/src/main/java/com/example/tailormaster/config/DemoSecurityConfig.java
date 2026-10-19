package com.example.tailormaster.config;

import com.example.tailormaster.repository.UserRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@Profile("demo")
public class DemoSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DemoAuthenticationFilter demoAuthenticationFilter(
            UserRepository userRepository) {
        return new DemoAuthenticationFilter(userRepository);
    }

    @Bean
    public FilterRegistrationBean<DemoAuthenticationFilter>
    demoAuthenticationFilterRegistration(
            DemoAuthenticationFilter filter) {

        FilterRegistrationBean<DemoAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain demoSecurityFilterChain(
            HttpSecurity http,
            DemoAuthenticationFilter demoAuthenticationFilter
    ) throws Exception {

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                CookieCsrfTokenRepository.withHttpOnlyFalse()
                        )
                )
                .addFilterBefore(
                        demoAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }
}