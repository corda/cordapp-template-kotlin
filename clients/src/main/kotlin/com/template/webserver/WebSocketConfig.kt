package com.template.webserver

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry


@Configuration
@EnableWebSocket
open class WebSocketConfig(@Autowired val websocketHandler: SocketController) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(webSocketHandlerRegistry: WebSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(websocketHandler, "/vault-events").setAllowedOrigins("*")
    }
}