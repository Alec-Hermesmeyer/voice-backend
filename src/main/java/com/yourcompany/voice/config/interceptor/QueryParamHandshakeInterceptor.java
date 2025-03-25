package com.yourcompany.voice.config.interceptor;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

public class QueryParamHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        URI uri = request.getURI();
        String query = uri.getQuery();

        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("stt_engine=")) {
                    String sttEngine = param.split("=")[1];
                    attributes.put("stt_engine", sttEngine);
                    System.out.println("🔄 Interceptor - Selected STT Engine: " + sttEngine);
                }
            }
        }

        return true; // proceed with handshake
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing here for now
    }
}
