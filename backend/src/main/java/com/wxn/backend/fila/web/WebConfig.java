package com.wxn.backend.fila.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// O React de desenvolvimento roda em localhost:3000 e chama a API em 8080.
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins("http://localhost:3000")
				.allowedMethods("GET", "POST", "OPTIONS")
				.allowedHeaders("*");
	}
}
