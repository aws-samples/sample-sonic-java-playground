package org.example.api.controller;

import org.example.constants.NovaSonicConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for handling transcription configuration.
 */
@RestController
@RequestMapping("/api/transcription")
@CrossOrigin(origins = "http://localhost:3000") // Allow requests from any origin for development
public class TranscriptionController {
    private static final Logger logger = LoggerFactory.getLogger(TranscriptionController.class);
    
    /**
     * Endpoint for getting the default configuration values.
     *
     * @return The default configuration values
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getDefaultConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxTokens", NovaSonicConstants.DEFAULT_MAX_TOKENS);
        config.put("topP", NovaSonicConstants.DEFAULT_TOP_P);
        config.put("topT", NovaSonicConstants.DEFAULT_TOP_T);
        config.put("systemPrompt", NovaSonicConstants.DEFAULT_SYSTEM_PROMPT);
        config.put("validSampleRates", NovaSonicConstants.VALID_SAMPLE_RATES);
        config.put("websocketEndpoint", "/ws/audio");
        config.put("language", NovaSonicConstants.LANG_EN_US); // Set default language
        config.put("useFeminineVoice", false); // Set default voice type
        
        return ResponseEntity.ok(config);
    }

    /**
     * Endpoint for getting the language-specific system prompt.
     *
     * @param language The language code (en-US, en-GB, fr, it, de, es)
     * @return The system prompt for the specified language
     */
    @GetMapping("/prompt/{language}")
    public ResponseEntity<Map<String, String>> getSystemPrompt(@PathVariable String language) {
        String prompt = switch (language) {
            case "en-US", "en-GB" -> NovaSonicConstants.ENGLISH_SYSTEM_PROMPT;
            case "es" -> NovaSonicConstants.SPANISH_SYSTEM_PROMPT;
            case "fr" -> NovaSonicConstants.FRENCH_SYSTEM_PROMPT;
            case "it" -> NovaSonicConstants.ITALIAN_SYSTEM_PROMPT;
            case "de" -> NovaSonicConstants.GERMAN_SYSTEM_PROMPT;
            default -> {
                logger.warn("Unsupported language code: {}. Falling back to default English prompt.", language);
                yield NovaSonicConstants.DEFAULT_SYSTEM_PROMPT;
            }
        };

        Map<String, String> response = new HashMap<>();
        response.put("systemPrompt", prompt);
        
        return ResponseEntity.ok(response);
    }
}
