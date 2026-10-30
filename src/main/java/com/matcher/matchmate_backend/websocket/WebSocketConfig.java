package com.matcher.matchmate_backend.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP broker for real-time room comments.
 * <ul>
 *   <li>Handshake: {@code /ws-comments} (raw WebSocket only — the only client is the
 *       Flutter app, which never falls back to SockJS).</li>
 *   <li>Clients subscribe to {@code /topic/rooms/{roomId}/comments}.</li>
 *   <li>Clients send to {@code /app/rooms/{roomId}/comment.send}.</li>
 *   <li>Per-session errors arrive on {@code /user/queue/errors}.</li>
 * </ul>
 * Authentication happens on the STOMP CONNECT frame ({@link StompAuthChannelInterceptor}),
 * not on the HTTP handshake, because browsers and SockJS cannot set custom upgrade headers.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT = "/ws-comments";

    private final StompAuthChannelInterceptor authInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registering a second, SockJS-wrapped endpoint at this same ENDPOINT path
        // used to overwrite this plain registration in Spring's handler map, so every
        // raw WebSocket upgrade (the only kind the Flutter client ever sends) got routed
        // to the SockJS handler instead and was rejected with HTTP 400.
        registry.addEndpoint(ENDPOINT).setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
