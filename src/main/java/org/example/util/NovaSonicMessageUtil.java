package org.example.util;

import org.example.constants.NovaSonicConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for creating Nova Sonic API messages using string literals.
 * This approach is more efficient than building JSONObjects.
 */
public final class NovaSonicMessageUtil {
        private static final Logger logger = LoggerFactory.getLogger(NovaSonicMessageUtil.class);
    private static final int OUTPUT_AUDIO_SAMPLE_RATE = 24000;
    private static final int OUTPUT_AUDIO_SAMPLE_SIZE_IN_BITS = 16;
    private static final int OUTPUT_AUDIO_CHANNEL = 1;

    private NovaSonicMessageUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a session start configuration message.
     *
     * @param maxTokens The maximum number of tokens to generate
     * @param topP The top-p value for sampling
     * @param temperature The temperature for sampling
     * @return A string containing the session start configuration JSON
     */
    public static String getSessionStartEvent(final int maxTokens, final double topP, final double temperature) {
        return String.format(
                "{\n"
                        + "  \"event\": {\n"
                        + "    \"sessionStart\": {\n"
                        + "      \"inferenceConfiguration\": {\n"
                        + "        \"maxTokens\": %d,\n"
                        + "        \"topP\": %f,\n"
                        + "        \"temperature\": %f\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                maxTokens, topP, temperature);
    }

    /**
     * Creates a prompt start event message.
     *
     * @param promptName The name of the prompt
     * @return A string containing the prompt start event JSON
     */
    public static String getPromptStartEvent(final String promptName, final String language, final boolean useFeminineVoice) {
        String voiceKey;
        if (language.equals(NovaSonicConstants.LANG_EN_GB)) {
            voiceKey = NovaSonicConstants.LANG_EN_GB;  // GB English always uses amy
            logger.info("Using En GB language ", voiceKey);
        } else {
            voiceKey = language + (useFeminineVoice ? "_F" : "_M");
        }
        String voiceId = NovaSonicConstants.VOICE_IDS.getOrDefault(voiceKey, NovaSonicConstants.VOICE_IDS.get(NovaSonicConstants.LANG_EN_US + "_M"));
        return String.format(
                "{\n"
                        + "  \"event\": {\n"
                        + "    \"promptStart\": {\n"
                        + "      \"promptName\": \"%s\",\n"
                        + "      \"textOutputConfiguration\": {\n"
                        + "        \"mediaType\": \"text/plain\"\n"
                        + "      },\n"
                        + "      \"audioOutputConfiguration\": {\n"
                        + "        \"mediaType\": \"audio/lpcm\",\n"
                        + "        \"sampleRateHertz\": %d,\n"
                        + "        \"sampleSizeBits\": %d,\n"
                        + "        \"channelCount\": %d,\n"
                        + "        \"voiceId\": \"%s\",\n"
                        + "        \"encoding\": \"base64\",\n"
                        + "        \"audioType\": \"SPEECH\"\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                promptName, OUTPUT_AUDIO_SAMPLE_RATE, OUTPUT_AUDIO_SAMPLE_SIZE_IN_BITS, OUTPUT_AUDIO_CHANNEL, voiceId);
    }

    /**
     * Creates a system prompt content start event message.
     *
     * @param promptName The name of the prompt
     * @param contentName The name of the content
     * @return A string containing the system prompt content start event JSON
     */
    public static String getSystemPromptContentStart(final String promptName, final String contentName) {
        return String.format(
                "{\n"
                        + "  \"event\": {\n"
                        + "    \"contentStart\": {\n"
                        + "      \"promptName\": \"%s\",\n"
                        + "      \"contentName\": \"%s\",\n"
                        + "      \"type\": \"TEXT\",\n"
                        + "      \"interactive\": true,\n"
                        + "      \"role\": \"SYSTEM\",\n"
                        + "      \"textInputConfiguration\": {\n"
                        + "        \"mediaType\": \"text/plain\"\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                promptName, contentName);
    }

    /**
     * Creates a system text input event message.
     *
     * @param promptName The name of the prompt
     * @param contentName The name of the content
     * @param systemPrompt The system prompt text
     * @return A string containing the system text input event JSON
     */
    public static String getSystemTextInput(
            final String promptName, final String contentName, final String systemPrompt) {
        // Escape special characters in the system prompt
        String escapedPrompt = systemPrompt.replace("\"", "\\\"")
                                         .replace("\n", "\\n")
                                         .replace("\r", "\\r")
                                         .replace("\t", "\\t");
        
        return String.format(
                "{\n"
                        + "  \"event\": {\n"
                        + "    \"textInput\": {\n"
                        + "      \"promptName\": \"%s\",\n"
                        + "      \"contentName\": \"%s\",\n"
                        + "      \"content\": \"%s\"\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                promptName, contentName, escapedPrompt);
    }

    /**
     * Creates a content end event message.
     *
     * @param promptName The name of the prompt
     * @param contentName The name of the content
     * @return A string containing the content end event JSON
     */
    public static String getContentEndEvent(final String promptName, final String contentName) {
        return String.format(
                "{\n"
                        + "  \"event\": {\n"
                        + "    \"contentEnd\": {\n"
                        + "      \"promptName\": \"%s\",\n"
                        + "      \"contentName\": \"%s\"\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                promptName, contentName);
    }

    /**
     * Creates an audio content start event message.
     *
     * @param promptName The name of the prompt
     * @param contentName The name of the content
     * @param sampleRate The audio sample rate in Hz
     * @param sampleSizeBits The audio sample size in bits
     * @param channels The number of audio channels
     * @return A string containing the audio content start event JSON
     */
    public static String getAudioContentStartEvent(
            final String promptName,
            final String contentName,
            final int sampleRate,
            final int sampleSizeBits,
            final int channels) {

        return String.format(
                "{\n"
                        + "  \"event\": {\n"
                        + "    \"contentStart\": {\n"
                        + "      \"promptName\": \"%s\",\n"
                        + "      \"contentName\": \"%s\",\n"
                        + "      \"type\": \"AUDIO\",\n"
                        + "      \"interactive\": true,\n"
                        + "      \"role\": \"USER\",\n"
                        + "      \"audioInputConfiguration\": {\n"
                        + "        \"mediaType\": \"audio/lpcm\",\n"
                        + "        \"sampleRateHertz\": %d,\n"
                        + "        \"sampleSizeBits\": %d,\n"
                        + "        \"channelCount\": %d,\n"
                        + "        \"audioType\": \"SPEECH\",\n"
                        + "        \"encoding\": \"base64\"\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                promptName, contentName, sampleRate, sampleSizeBits, channels);
    }

    /**
     * Creates an audio input event message.
     *
     * @param promptName The name of the prompt
     * @param contentName The name of the content
     * @param audioBase64 The base64-encoded audio data
     * @return A string containing the audio input event JSON
     */
    public static String getAudioInputEvent(
            final String promptName, final String contentName, final String audioBase64) {
        // Using StringBuilder for potentially large audio content
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"event\": {\n");
        sb.append("    \"audioInput\": {\n");
        sb.append("      \"promptName\": \"").append(promptName).append("\",\n");
        sb.append("      \"contentName\": \"").append(contentName).append("\",\n");
        sb.append("      \"content\": \"").append(audioBase64).append("\"\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Creates a prompt end event message.
     *
     * @param promptName The name of the prompt
     * @return A string containing the prompt end event JSON
     */
    public static String getPromptEndEvent(final String promptName) {
        return String.format(
                "{\n"
                        + "  \"event\": {\n"
                        + "    \"promptEnd\": {\n"
                        + "      \"promptName\": \"%s\"\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                promptName);
    }

    /**
     * Creates a session end event message.
     *
     * @return A string containing the session end event JSON
     */
    public static String getSessionEndEvent() {
        return "{\n" + "  \"event\": {\n" + "    \"sessionEnd\": {}\n" + "  }\n" + "}";
    }
}
