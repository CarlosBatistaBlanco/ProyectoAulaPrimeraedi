/*
 * Este archivo abre un canal de comunicación en vivo para que los mensajes del chat
 * de soporte y las alertas de la campanita lleguen al instante sin tener que recargar la página.
 * Trabaja en segundo plano en los paneles de cliente, empleado y administrador.
 */
package com.tecnicsecure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Define los canales de salida (para avisos y chats) y de entrada (para mensajes que envía el usuario)
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    // Abre la puerta de conexión "/ws" donde se enlaza el navegador para recibir avisos al instante
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
}
