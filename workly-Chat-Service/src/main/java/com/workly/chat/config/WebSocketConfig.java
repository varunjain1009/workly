package com.workly.chat.config;

import com.workly.chat.handler.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    @NonNull
    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                            @NonNull ServerHttpResponse response,
                            @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes)
                            throws Exception {
                        // Extract query param ?userId=...
                        // In production, verify JWT token from Header/Query
                        // For MVP: Simple userId extraction
                        String query = request.getURI().getQuery();
                        if (query != null && query.contains("userId=")) {
                            String userId = query.split("userId=")[1].split("&")[0];
                            attributes.put("userId", userId);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                            @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {
                    }
                });
    }
}
