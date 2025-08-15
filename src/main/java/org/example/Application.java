package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.example.handler.NovaWebSocketHandler;
import org.example.handler.NovaSonicEventHandler;

@SpringBootApplication
@EnableWebSocket
public class Application implements WebSocketConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        try {
            applicationContext = SpringApplication.run(Application.class, args);
            logger.info("NovaSonicPlayground server started successfully");
        } catch (Exception e) {
            logger.error("Error starting NovaSonicPlayground server: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(novaWebSocketHandler(novaSonicEventHandler()), "/ws/audio")
               .setAllowedOrigins("http://localhost:3000"); // Configure CORS as needed
    }

    @Bean
    public NovaSonicEventHandler novaSonicEventHandler() {
        return new NovaSonicEventHandler();
    }

    @Bean
    public NovaWebSocketHandler novaWebSocketHandler(NovaSonicEventHandler eventHandler) {
        return new NovaWebSocketHandler(eventHandler);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Set maximum message size to 10MB to handle audio chunks
        container.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);
        container.setMaxTextMessageBufferSize(1024 * 1024);
        // Set timeout to 30 minutes to handle long transcription sessions
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L);
        return container;
    }
}
