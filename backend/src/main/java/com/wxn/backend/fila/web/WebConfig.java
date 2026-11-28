package com.wxn.backend.fila.web;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final String[] origensPermitidas;

	public WebConfig(@Value("${wxn.cors.allowed-origins:http://localhost:3000}") String origens) {
		String[] configuradas = Arrays.stream(origens.split(","))
				.map(String::trim)
				.filter(origem -> !origem.isBlank())
				.toArray(String[]::new);
		this.origensPermitidas = configuradas.length == 0
				? new String[] { "http://localhost:3000" }
				: configuradas;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(origensPermitidas)
				.allowedMethods("GET", "POST", "OPTIONS")
				.allowedHeaders("*");
	}
}
