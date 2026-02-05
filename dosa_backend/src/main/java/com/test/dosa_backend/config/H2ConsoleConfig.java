package com.test.dosa_backend.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class H2ConsoleConfig {


    ServletRegistrationBean<JakartaWebServlet> h2Console() {
        return new ServletRegistrationBean<>(
                new org.h2.server.web.JakartaWebServlet(),
                "/h2-console/*"
        );
    }

}
