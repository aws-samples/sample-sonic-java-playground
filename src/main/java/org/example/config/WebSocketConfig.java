package org.example.config;

import org.example.handler.NovaWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final NovaWebSocketHandler novaWebSocketHandler;

    public WebSocketConfig(NovaWebSocketHandler novaWebSocketHandler) {
        this.novaWebSocketHandler = novaWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(novaWebSocketHandler, "/nova-audio")
               .setAllowedOrigins("http://localhost:3000");
    }
}
