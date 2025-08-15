package org.example.api.service;

import org.example.client.NovaSonicClient;
import org.example.handler.NovaSonicEventHandler;
import static org.example.constants.NovaSonicConstants.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for handling audio transcription using the NovaSonic client.
 */
@Service
public class TranscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(TranscriptionService.class);
    
    @Autowired
    private NovaSonicEventHandler eventHandler;
    
    /**
     * Creates a new NovaSonic client for WebSocket streaming.
     *
     * @param maxTokens The maximum number of tokens to generate
     * @param topP The top-p value for sampling
     * @param topT The top-t value for sampling (temperature)
     * @param systemPrompt The system prompt text
     * @return The initialized NovaSonic client
     */
    /**
     * Creates a new NovaSonic client for WebSocket streaming.
     *
     * @param maxTokens The maximum number of tokens to generate
     * @param topP The top-p value for sampling
     * @param topT The top-t value for sampling (temperature)
     * @param systemPrompt The system prompt text
     * @param language The language for transcription
     * @return The initialized NovaSonic client
     * @throws RuntimeException if client creation fails
     */
    public NovaSonicClient createStreamingClient(int maxTokens, double topP, double topT, String systemPrompt, String language) {
        return createStreamingClient(maxTokens, topP, topT, systemPrompt, language, false);
    }

    /**
     * Creates a new NovaSonic client for WebSocket streaming.
     *
     * @param maxTokens The maximum number of tokens to generate
     * @param topP The top-p value for sampling
     * @param topT The top-t value for sampling (temperature)
     * @param systemPrompt The system prompt text
     * @param language The language for transcription
     * @param useFeminineVoice Whether to use feminine voice (true) or masculine voice (false)
     * @return The initialized NovaSonic client
     * @throws RuntimeException if client creation fails
     */
    public NovaSonicClient createStreamingClient(int maxTokens, double topP, double topT, String systemPrompt, String language, boolean useFeminineVoice) {
        try {
            NovaSonicClient client = new NovaSonicClient(maxTokens, topP, topT, systemPrompt, language, useFeminineVoice, eventHandler);
            client.initializeSession(DEFAULT_AUDIO_FORMAT);
            return client;
        } catch (Exception e) {
            logger.error("Error creating streaming client: {}", e.getMessage());
            throw new RuntimeException("Error creating streaming client: " + e.getMessage(), e);
        }
    }
}
