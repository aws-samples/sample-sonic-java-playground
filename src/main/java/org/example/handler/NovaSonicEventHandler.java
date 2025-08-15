package org.example.handler;

import org.example.client.NovaSonicClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.example.constants.NovaSonicConstants.*;

/**
 * Handler for Nova Sonic API events. This class processes different types of events from the Nova Sonic API.
 */
public class NovaSonicEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(NovaSonicEventHandler.class);
    private NovaSonicClient novaSonicClient;
    private WebSocketMessageSender messageSender;
    private String currentGenerationStage = null;

    public interface WebSocketMessageSender {
        void sendAudioResponse(String sessionId, String audioData);
        void sendTranscriptionUpdate(String sessionId, String transcript, String role);
    }

    public void setMessageSender(WebSocketMessageSender sender) {
        this.messageSender = sender;
    }

    public void setNovaSonicClient(NovaSonicClient novaSonicClient) {
        this.novaSonicClient = novaSonicClient;
    }

    /**
     * Handles a message from the Nova Sonic API.
     *
     * @param message The message to handle
     */
    public void handleMessage(final String message) {
        try {
            final JSONObject jsonMessage = new JSONObject(message);
            logger.debug("Received message {}", message);

            // Check if the message contains an event
            if (jsonMessage.has(EVENT_KEY)) {
                handleEventMessage(jsonMessage.getJSONObject(EVENT_KEY));
            } else {
                logger.info("Received other message type {}", jsonMessage);
            }
        } catch (Exception e) {
            logger.error("Error processing message for {}", message, e);
        }
    }

    /**
     * Handles an event message.
     *
     * @param event The event JSON object
     */
    private void handleEventMessage(final JSONObject event) {
        if (event.has(TEXT_OUTPUT)) {
            handleTextOutputEvent(event.getJSONObject(TEXT_OUTPUT));
        } else if (event.has(AUDIO_OUTPUT)) {
            handleAudioOutputEvent(event.getJSONObject(AUDIO_OUTPUT));
        } else if (event.has(CONTENT_START)) {
            handleContentStartEvent(event.getJSONObject(CONTENT_START));
        } else if (event.has(COMPLETION_START)) {
            logger.info("completion start received");
        } else if (event.has(USAGE_EVENT)) {
            logger.info("usage event received");
        } else if (event.has(CONTENT_END)) {
            logger.info("Content end event received");
            currentGenerationStage = null; // Reset generation stage
        } else {
            logger.info("Received {}", event);
        }
    }

    private void handleContentStartEvent(final JSONObject contentStart) {
        try {
            if (contentStart.has("additionalModelFields")) {

                String additionalFields = contentStart.getString("additionalModelFields");
                JSONObject additionalFieldsJson = new JSONObject(additionalFields);
                this.currentGenerationStage = additionalFieldsJson.getString("generationStage");
                logger.info("Generation stage set to: {}", this.currentGenerationStage);
            }
        } catch (final JSONException e) {
            logger.error("Error parsing content start event: {}", e.getMessage());
        }
    }

    /**
     * Handles a text output event.
     *
     * @param textOutput The text output JSON object
     */
    private void handleAudioOutputEvent(final JSONObject audioOutput) {
        try {
            if (audioOutput.has("content") && messageSender != null) {
                messageSender.sendAudioResponse(
                    novaSonicClient.getSessionId(),
                    audioOutput.getString("content")
                );
            }
        } catch (final JSONException e) {
            logger.error("Error parsing audio output event: {}", e.getMessage());
        }
    }

    private void handleTextOutputEvent(final JSONObject textOutput) {
        try {
            final String content = textOutput.getString(CONTENT_KEY);
            final String role = textOutput.getString(ROLE_KEY);
            
            // Check if current generation is speculative
            boolean isSpeculative = "SPECULATIVE".equals(currentGenerationStage);
            logger.info("Speculation is {} for text", isSpeculative);

            if (!isSpeculative && messageSender != null && novaSonicClient != null) {
                messageSender.sendTranscriptionUpdate(novaSonicClient.getSessionId(), content, role);
            } else if (isSpeculative) {
                logger.debug("Skipping speculative output: [{}]: {}", role, content);
            }
        } catch (final JSONException e) {
            logger.error("Error parsing text output event: {}", e.getMessage());
        }
    }
}
