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
        // FIX: ensure wake-word handler is registered correctly
        registry.addHandler(wakeWordWebSocketHandler, "/ws/wake-word")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addHandler(audioWebSocketHandler, "/ws/audio")
                .setAllowedOrigins("*")
                .addInterceptors(new QueryParamHandshakeInterceptor());

        registry.addHandler(uiControlWebSocketHandler, "/ws/ui-control")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
