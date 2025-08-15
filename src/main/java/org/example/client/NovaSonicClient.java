package org.example.client;

import io.reactivex.rxjava3.processors.ReplayProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.example.handler.NovaSonicEventHandler;
import org.example.handler.NovaSonicResponseHandler;
import org.example.util.NovaSonicMessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithBidirectionalStreamInput;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithBidirectionalStreamRequest;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;

import static org.example.constants.NovaSonicConstants.*;

/**
 * Client for streaming audio to the Nova Sonic API and receiving results.
 */
public class NovaSonicClient implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(NovaSonicClient.class);

    private String sessionId;
    private BedrockRuntimeAsyncClient bedrockClient;
    private final String promptName;
    private final String audioContentName;
    private final String systemContentName;
    private final int maxTokens;
    private final double topP;
    private final double topT;
    private final String systemPrompt;
    private final String language;
    private final boolean useFeminineVoice;
    private final List<String> transcripts;
    private final NovaSonicEventHandler eventHandler;

    private boolean onCompleteCalled = false;
    private boolean audioContentStarted = false;

    // Bidirectional stream publisher
    private ReplayProcessor<InvokeModelWithBidirectionalStreamInput> publisher;

    /**
     * Creates a new Nova Sonic client with custom configuration.
     */
    public NovaSonicClient(int maxTokens, double topP, double topT, String systemPrompt, String language, boolean useFeminineVoice, NovaSonicEventHandler eventHandler) {
        logger.info("Creating client using maxtokens; {}, topP: {}, topT: {}, systemPrompt: {}, language: {}, useFeminineVoice: {}", maxTokens, topP, topT, systemPrompt, language, useFeminineVoice);
        this.maxTokens = maxTokens;
        this.topP = topP;
        this.topT = topT;
        this.systemPrompt = systemPrompt;
        this.language = language != null ? language : "en-US";
        this.useFeminineVoice = useFeminineVoice;

        // Generate unique IDs
        this.promptName = "prompt-" + UUID.randomUUID();
        this.audioContentName = "audio-content-" + UUID.randomUUID();
        this.systemContentName = "system-" + UUID.randomUUID();
        this.transcripts = new ArrayList<>();
        this.eventHandler = eventHandler;
    }

    /**
     * Creates and configures the Bedrock client with optimized settings.
     */
    private BedrockRuntimeAsyncClient createBedrockClient() {
        AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
        // Configure Netty HTTP client with proper timeouts and protocol settings
        NettyNioAsyncHttpClient.Builder nettyBuilder = NettyNioAsyncHttpClient.builder()
                .readTimeout(Duration.of(180, ChronoUnit.SECONDS))
                .maxConcurrency(100)
                .protocol(Protocol.HTTP2)
                .protocolNegotiation(ProtocolNegotiation.ALPN);

        // Create and configure Bedrock client
        return BedrockRuntimeAsyncClient.builder()
                .region(Region.of(NOVA_SONIC_REGION))
                .httpClient(nettyBuilder.build())
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * Initializes the streaming session.
     */
    public void initializeSession(final AudioFormat audioFormat) {
        try {
            // Initialize state atomically
            synchronized(this) {
                if (this.onCompleteCalled) {
                    throw new IllegalStateException("Session already completed");
                }
                this.onCompleteCalled = false;
                this.audioContentStarted = false;
            }

            // Create new Bedrock client for this session
            this.bedrockClient = createBedrockClient();

            // Create ReplayProcessor with time-based expiry
            this.publisher = ReplayProcessor.createWithTime(
                    REPLAY_PROCESSOR_EXPIRY_TIME,
                    REPLAY_PROCESSOR_EXPIRY_UNIT,
                    Schedulers.io()
            );

            // Set this client instance in the event handler
            eventHandler.setNovaSonicClient(this);
            NovaSonicResponseHandler responseHandler = new NovaSonicResponseHandler(eventHandler);

            // Create stream request
            var streamRequest = InvokeModelWithBidirectionalStreamRequest.builder()
                    .modelId(NOVA_SONIC_MODEL_ID)
                    .build();

            // Initiate bidirectional stream
            var completableFuture = bedrockClient.invokeModelWithBidirectionalStream(
                    streamRequest, publisher, responseHandler);

            // Handle completion and errors properly
            completableFuture.exceptionally(throwable -> {
                publisher.onError(throwable);
                handleError("Error in bidirectional stream: " + throwable.getMessage());
                return null;
            });

            // Send SessionStart event as the first message
            var sessionStartJson = """
            {
              "event": {
                "sessionStart": {
                  "inferenceConfiguration": {
                    "maxTokens": %d,
                    "topP": %f,
                    "temperature": %f
                  }
                }
              }
            }""".formatted(maxTokens, topP, topT);
            sendMessageThroughStream(sessionStartJson);

            // Send remaining configuration messages
            sendConfigurationMessages();
            sendAudioContentStartEvent(audioFormat);
        } catch (Exception e) {
            handleError("Failed to initialize session: " + e.getMessage());
            throw e; // Propagate error for proper handling
        }
    }

    /**
     * Sends a message through the bidirectional stream.
     */
    private void sendMessageThroughStream(String message) {
        try {
            if (publisher == null) {
                handleError("Publisher is not initialized");
                return;
            }
            logger.debug("Sending data ");

            var input = InvokeModelWithBidirectionalStreamInput.chunkBuilder()
                    .bytes(SdkBytes.fromUtf8String(message))
                    .build();

            publisher.onNext(input);
        } catch (Exception e) {
            handleError("Error sending message through stream: " + e.getMessage());
        }
    }

    /**
     * Sends the configuration messages after SessionStart.
     */
    private void sendConfigurationMessages() {
        try {
            // Send prompt start event
            String promptConfig = NovaSonicMessageUtil.getPromptStartEvent(promptName, language, useFeminineVoice);
            logger.info(promptConfig);
            logger.info("language: {}", language);
            sendMessageThroughStream(promptConfig);

            // Send system prompt content start if system prompt is provided
            String systemPromptConfig = NovaSonicMessageUtil.getSystemPromptContentStart(
                        promptName, systemContentName);
            sendMessageThroughStream(systemPromptConfig);

            // Send system text input
            String systemTextPromptConfig = NovaSonicMessageUtil.getSystemTextInput(
                    promptName, systemContentName, this.systemPrompt);
            sendMessageThroughStream(systemTextPromptConfig);

            // Send system content end event
            String systemContentEnd = NovaSonicMessageUtil.getContentEndEvent(promptName, systemContentName);
            sendMessageThroughStream(systemContentEnd);
        } catch (Exception e) {
            handleError("Error sending configuration messages: " + e.getMessage());
        }
    }

    /**
     * Sends the audio content start event.
     */
    private void sendAudioContentStartEvent(AudioFormat audioFormat) {
        String audioContentStart = NovaSonicMessageUtil.getAudioContentStartEvent(
            promptName, audioContentName,
            (int) audioFormat.getSampleRate(),
            audioFormat.getSampleSizeInBits(),
            audioFormat.getChannels());

        sendMessageThroughStream(audioContentStart);
        audioContentStarted = true;
    }

    /**
     * Sends an audio chunk for processing.
     */
    public void sendAudioChunk(ByteBuffer audioBuffer) {
        if (!audioContentStarted) {
            logger.warn("Audio content not started yet, ignoring chunk");
            return;
        }

        try {
            // Convert audio buffer to bytes
            byte[] audioData = new byte[audioBuffer.remaining()];
            audioBuffer.get(audioData);

            // Encode audio to base64
            String audioBase64 = Base64.getEncoder().encodeToString(audioData);

            // Create and send audio input event
            String audioInputEvent = NovaSonicMessageUtil.getAudioInputEvent(
                    promptName, audioContentName, audioBase64);
            sendMessageThroughStream(audioInputEvent);

        } catch (Exception e) {
            handleError("Error sending audio chunk: " + e.getMessage());
        }
    }

    /**
     * Sends the audio content end event.
     */
    private void sendAudioContentEndEvent() {
        String audioContentEnd = NovaSonicMessageUtil.getContentEndEvent(promptName, audioContentName);
        sendMessageThroughStream(audioContentEnd);
    }

    /**
     * Handles errors consistently throughout the class.
     */
    private void handleError(String errorMessage) {
        logger.error(errorMessage);
    }

    public void addTranscript(final String content) {
        this.transcripts.add(content);
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    /**
     * Completes the session by sending prompt end and session end events.
     */
    public void completeSession() {
        if (onCompleteCalled) {
            return;
        }

        try {
            // Send audio content end if needed
            if (audioContentStarted) {
                sendAudioContentEndEvent();
            }

            // Send prompt end event
            String promptEndEvent = NovaSonicMessageUtil.getPromptEndEvent(promptName);
            sendMessageThroughStream(promptEndEvent);

            // Send session end event
            String sessionEndEvent = NovaSonicMessageUtil.getSessionEndEvent();
            sendMessageThroughStream(sessionEndEvent);

            // Complete the publisher
            if (publisher != null) {
                publisher.onComplete();
            }

            // Mark as completed
            onCompleteCalled = true;

            logger.info("Completed session for sessionID = {}", this.sessionId);
        } catch (Exception e) {
            handleError("Error completing session: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            // Complete the session if active
            if (!onCompleteCalled) {
                completeSession();
            }

            // Close Bedrock client
            if (bedrockClient != null) {
                bedrockClient.close();
            }
        } catch (Exception e) {
            logger.error("Error during close: {}", e.getMessage(), e);
        } finally {
            // Clean up resources
            if (publisher != null && !publisher.hasComplete()) {
                publisher.onComplete();
            }
        }
        logger.info("Closed");
    }
}
