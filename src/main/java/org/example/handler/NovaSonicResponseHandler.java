package org.example.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.bedrockruntime.model.BidirectionalOutputPayloadPart;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithBidirectionalStreamOutput;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithBidirectionalStreamResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithBidirectionalStreamResponseHandler;

import java.nio.charset.StandardCharsets;

/**
 * Response handler for bidirectional streaming responses from the Bedrock Runtime API.
 */
public class NovaSonicResponseHandler implements InvokeModelWithBidirectionalStreamResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(NovaSonicResponseHandler.class);
    private final NovaSonicEventHandler eventHandler;

    /**
     * Creates a new response handler.
     *
     * @param eventHandler The event handler to process events
     */
    public NovaSonicResponseHandler(NovaSonicEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void responseReceived(InvokeModelWithBidirectionalStreamResponse response) {
        logger.info("Received bidirectional stream response for {}", response.toString());
    }

    @Override
    public void onEventStream(SdkPublisher<InvokeModelWithBidirectionalStreamOutput> sdkPublisher) {
        var completableFuture = sdkPublisher.subscribe((output) -> output.accept(new Visitor() {
            @Override
            public void visitChunk(BidirectionalOutputPayloadPart event) {
                String payloadString =
                    StandardCharsets.UTF_8.decode((event.bytes().asByteBuffer().rewind().duplicate())).toString();
                    eventHandler.handleMessage(payloadString);
            }
        }));

        // if any of the chunks fail to parse or be handled ensure to send an error or they will get lost
        completableFuture.exceptionally(t -> {
            logger.error("Stream error : {}", t.getMessage(), t);
            return null;
        });
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        logger.error("Exception occurred in bidirectional stream: {}", throwable.getMessage(), throwable);
    }

    @Override
    public void complete() {
        logger.info("Bidirectional stream completed");
    }
}
