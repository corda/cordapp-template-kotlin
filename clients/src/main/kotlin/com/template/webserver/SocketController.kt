package com.template.webserver

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import net.corda.core.contracts.ContractState
import net.corda.core.node.services.Vault
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Component
class SocketController(objectMapper: ObjectMapper, private val rpc: NodeRPCConnection): TextWebSocketHandler() {
    private val sessions: MutableMap<String, WebSocketSession> = ConcurrentHashMap()
    private val objectWriter: ObjectWriter = objectMapper.writerWithDefaultPrettyPrinter()

    private val executor = Executors.newSingleThreadScheduledExecutor()

    companion object {
        private val log = LoggerFactory.getLogger(SocketController::class.java)
    }

    @PostConstruct
    fun connectToVault() {
        executor.scheduleWithFixedDelay({
            val vault = rpc.proxy.vaultQuery(ContractState::class.java)
            println("THIS IS THE VAULT " + vault)
            sendMessageToAll(VaultUpdatedEvent(vault))
        }, 0, 1, TimeUnit.SECONDS)

        log.info("Monitoring vault updates")
    }

    private fun sendMessageToAll(event: VaultSocketEvent) {
        val data = objectWriter.writeValueAsString(event)
            println("THIS IS THE VAULT data " + data)
        val textMessage =
                TextMessage(data)
            println("THIS IS THE VAULT text message " + textMessage)

        sessions.forEach { (key: String?, value: WebSocketSession) ->
            try {
                log.info("Send message {} to socketId: {}", data, key)
                value.sendMessage(textMessage)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    @Throws(Exception::class)
    override fun handleTransportError(session: WebSocketSession, throwable: Throwable) {
        log.error("error occured at sender $session", throwable)
    }

    @Throws(Exception::class)
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        log.info(
                String.format(
                        "Session %s closed because of %s",
                        session.id,
                        status.reason
                )
        )
        sessions.remove(session.id)
    }

    @Throws(Exception::class)
    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("Connected ... " + session.id)
        sessions[session.id] = session
    }

    @Throws(Exception::class)
    override fun handleTextMessage(
            session: WebSocketSession,
            message: TextMessage
    ) {
        log.info("Handling message: {}", message)
    }

    // Allows extension to filter specific vault updates on client side
    abstract class VaultSocketEvent(val eventName: String)

    class VaultUpdatedEvent(val vault: Vault.Page<ContractState>): VaultSocketEvent("VAULT_UPDATE")
}