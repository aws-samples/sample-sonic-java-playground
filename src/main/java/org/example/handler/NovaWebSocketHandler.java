package org.example.handler;

import org.example.client.NovaSonicClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.constants.NovaSonicConstants.DEFAULT_SYSTEM_PROMPT;
import static org.example.constants.NovaSonicConstants.DEFAULT_AUDIO_FORMAT;

@Component
public class NovaWebSocketHandler extends TextWebSocketHandler implements NovaSonicEventHandler.WebSocketMessageSender {
    private static final Logger logger = LoggerFactory.getLogger(NovaWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, NovaSonicClient> novaSonicClients = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> sessionInitializationFlags = new ConcurrentHashMap<>();

    private void validateSessionState(WebSocketSession session) throws IOException {
        if (!session.isOpen()) {
            throw new IOException("Session is no longer open");
        }

        // Verify local origin
        String origin = session.getHandshakeHeaders().getOrigin();
        if (!origin.startsWith("http://localhost:")) {
            throw new IOException("Invalid origin");
        }

        AtomicBoolean initFlag = sessionInitializationFlags.get(session.getId());
        if (initFlag == null || !initFlag.get()) {
            throw new IOException("Session not fully initialized");
        }
    }

    private void cleanupNovaSonicClient(WebSocketSession session, boolean removeSession) {
        String sessionId = session.getId();
        logger.info("Cleaning up resources for session {}", sessionId);
        
        try {
            NovaSonicClient client = novaSonicClients.remove(sessionId);
            if (client != null) {
                try {
                    client.completeSession();
                    client.close();
                } catch (Exception e) {
                    logger.warn("Error during client cleanup: {}", e.getMessage());
                }
            }

            if (removeSession) {
                sessionInitializationFlags.remove(sessionId);
                sessions.remove(sessionId);
                if (session.isOpen()) {
                    try {
                        session.close(CloseStatus.SERVER_ERROR);
                    } catch (IOException e) {
                        logger.warn("Error closing session: {}", e.getMessage());
                    }
                }
            } else {
                sessionInitializationFlags.put(sessionId, new AtomicBoolean(false));
            }
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }

    private void cleanupSession(WebSocketSession session) {
        cleanupNovaSonicClient(session, true);
    }

    private final NovaSonicEventHandler eventHandler;

    public NovaWebSocketHandler(NovaSonicEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("WebSocket connection established: {}", session.getId());
        sessions.put(session.getId(), session);
        sessionInitializationFlags.put(session.getId(), new AtomicBoolean(false));
        
        try {
            // Parse configuration from query parameters
            String query = session.getUri().getQuery();
            java.util.Map<String, String> params = parseQueryString(query);
            
            // Extract configuration values with defaults
            int maxTokens = Integer.parseInt(params.getOrDefault("maxTokens", "1024"));
            double topP = Double.parseDouble(params.getOrDefault("topP", "0.9"));
            double topT = Double.parseDouble(params.getOrDefault("topT", "0.7"));
            String systemPrompt = params.getOrDefault("systemPrompt", "");
            String language = params.getOrDefault("language", "en-US");
            boolean useFeminineVoice = Boolean.parseBoolean(params.getOrDefault("useFeminineVoice", "false"));
            
            // Initialize Nova Sonic client for this session with configuration
            NovaSonicClient novaSonicClient = new NovaSonicClient(
                maxTokens,
                topP,
                topT,
                systemPrompt.isEmpty() ? DEFAULT_SYSTEM_PROMPT : systemPrompt,
                language,
                useFeminineVoice,
                eventHandler
            );
            
            eventHandler.setMessageSender(this);
            novaSonicClient.setSessionId(session.getId());
            novaSonicClient.initializeSession(DEFAULT_AUDIO_FORMAT);
            novaSonicClients.put(session.getId(), novaSonicClient);
            
            // Mark session as initialized and send ready message
            sessionInitializationFlags.get(session.getId()).set(true);
            session.sendMessage(new TextMessage("{\"type\":\"status\",\"status\":\"ready\"}"));
        } catch (Exception e) {
            logger.error("Error initializing session: {}", e.getMessage());
        }
    }

    private java.util.Map<String, String> parseQueryString(String query) {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }

        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            try {
                String key = java.net.URLDecoder.decode(pair[0], "UTF-8");
                String value = pair.length > 1 ? java.net.URLDecoder.decode(pair[1], "UTF-8") : "";
                params.put(key, value);
            } catch (java.io.UnsupportedEncodingException e) {
                logger.error("Error decoding query parameter: {}", e.getMessage());
            }
        }
        return params;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
        cleanupSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage(), exception);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        try {
            validateSessionState(session);
            NovaSonicClient client = novaSonicClients.get(session.getId());
            if (client != null) {
                try {
                    byte[] audioData = message.getPayload().array();
                    client.sendAudioChunk(java.nio.ByteBuffer.wrap(audioData));
                } catch (Exception e) {
                    logger.error("Error processing audio chunk: {}", e.getMessage());
                    try {
                        session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"Error processing audio\"}"));
                    } catch (IOException ex) {
                        logger.error("Error sending error message: {}", ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error initializing session: {}", e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        try {
            switch (payload) {
                case "stop":
                    session.sendMessage(new TextMessage("{\"type\":\"status\",\"status\":\"stopped\"}"));
                    break;
                case "close":
                    cleanupNovaSonicClient(session, false);
                    session.close();
                    break;
                case "reset_session":
                    cleanupNovaSonicClient(session, false);
                    session.sendMessage(new TextMessage("{\"type\":\"status\",\"status\":\"ready\"}"));
                    break;
            }
        } catch (Exception e) {
            logger.error("Error handling text message: {}", e.getMessage());
        }
    }

    public void sendTranscriptionUpdate(String sessionId, String transcript, String role) {
        // logger.info("sendTranscriptionUpdate: SessionID={}, transcript={}, role={}", sessionId, transcript, role);
        WebSocketSession session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            logger.warn("Cannot send transcription - invalid session state for {}", sessionId);
            return;
        }
        try {
            String message = String.format(
                "{\"type\":\"transcription\",\"text\":\"%s\",\"role\":\"%s\"}",
                transcript.replace("\"", "\\\""),
                role
            );
            session.sendMessage(new TextMessage(message));
            logger.info("Sent sendTranscriptionUpdate: Role {} {}",role, message);
        } catch (IOException e) {
            logger.error("Error sending transcription to session {}: {}", sessionId, e.getMessage());
        }
    }

    public void sendAudioResponse(String sessionId, String audioData) {
        WebSocketSession session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            logger.warn("Cannot send audio response - invalid session state for {}", sessionId);
            return;
        }
        try {
            String message = String.format("{\"type\":\"audio\",\"data\":\"%s\"}", audioData);
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            logger.error("Error sending audio response to session {}: {}", sessionId, e.getMessage());
        }
    }
}
