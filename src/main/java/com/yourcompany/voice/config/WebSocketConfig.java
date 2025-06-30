package com.yourcompany.voice.config;

import com.yourcompany.voice.config.interceptor.QueryParamHandshakeInterceptor;
import com.yourcompany.voice.controller.WakeWordWebSocketHandler;
import com.yourcompany.voice.controller.AudioWebSocketHandler;
import com.yourcompany.voice.controller.UIControlWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WakeWordWebSocketHandler wakeWordWebSocketHandler;
    private final AudioWebSocketHandler audioWebSocketHandler;
    private final UIControlWebSocketHandler uiControlWebSocketHandler;

    public WebSocketConfig(WakeWordWebSocketHandler wakeWordWebSocketHandler,
                           AudioWebSocketHandler audioWebSocketHandler,
                           UIControlWebSocketHandler uiControlWebSocketHandler) {
        this.wakeWordWebSocketHandler = wakeWordWebSocketHandler;
        this.audioWebSocketHandler = audioWebSocketHandler;
        this.uiControlWebSocketHandler = uiControlWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Use allowedOriginPatterns instead of allowedOrigins to avoid credentials conflict
        String[] allowedOriginPatterns = { "*" };

        // Wake-word WebSocket with SockJS fallback for Azure
        registry.addHandler(wakeWordWebSocketHandler, "/ws/wake-word")
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS()  // SockJS fallback for Azure compatibility
                .setHeartbeatTime(25000);  // Keep connection alive (Azure has 4min timeout)

        // Audio WebSocket with SockJS fallback
        registry.addHandler(audioWebSocketHandler, "/ws/audio")
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .addInterceptors(new QueryParamHandshakeInterceptor())
                .withSockJS()
                .setHeartbeatTime(25000);
                
        // Also add a native WebSocket endpoint for audio (without SockJS)
        registry.addHandler(audioWebSocketHandler, "/ws/audio-native")
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .addInterceptors(new QueryParamHandshakeInterceptor());

        // UI Control WebSocket with SockJS fallback
        registry.addHandler(uiControlWebSocketHandler, "/ws/ui-control")
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS()
                .setHeartbeatTime(25000);
    }
}
