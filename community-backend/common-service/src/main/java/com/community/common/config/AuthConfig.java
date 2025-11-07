package com.community.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class AuthConfig {
    @Bean
    public UserDetailsService userDetailsService() {
        // 提供一个空的用户库，防�?Spring Boot 自动生成默认用户和提�?
        return new InMemoryUserDetailsManager();
    }
}
