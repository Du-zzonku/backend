package com.test.dosa_backend.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(
  info = @Info(
    title = "Dosa Backend API",
    version = "v1",
    description = "3D Model Assembly API"
  )
)
public class OpenApiConfig {}
