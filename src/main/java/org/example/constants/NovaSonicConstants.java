package org.example.constants;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Constants for the NovaSonicPlayground application.
 */
public final class NovaSonicConstants {
    private NovaSonicConstants() {
        // Private constructor to prevent instantiation
    }

    // Bedrock Configuration
    public static final String NOVA_SONIC_MODEL_ID = "amazon.nova-sonic-v1:0";
    public static final String NOVA_SONIC_REGION = "us-east-1"; // Nova Sonic only available in us-east-1

    // Bedrock Streaming Constants
    public static final long REPLAY_PROCESSOR_EXPIRY_TIME = 3;
    public static final TimeUnit REPLAY_PROCESSOR_EXPIRY_UNIT = TimeUnit.MINUTES;

    // Audio Format Constants
    public static final int SIXTEEN_BIT = 16;
    public static final int VALID_CHANNELS = 1;
    public static final Set<Float> VALID_SAMPLE_RATES = new HashSet<>(Arrays.asList(8000F, 16000F, 24000F));
    public static final javax.sound.sampled.AudioFormat DEFAULT_AUDIO_FORMAT = 
        new javax.sound.sampled.AudioFormat(16000, SIXTEEN_BIT, VALID_CHANNELS, true, true);

    // Streaming Constants
    public static final int SESSION_CREATION_TIMEOUT_SECONDS = 15;
    public static final int STREAM_LATCH_TIMEOUT = 30;

    // Audio Output Waiting Constants
    public static final int AUDIO_START_TIMEOUT_SECONDS = 10;
    public static final int AUDIO_END_TIMEOUT_SECONDS = 30;
    public static final float SILENCE_THRESHOLD_SECONDS = 4.0f;
    public static final float CHECK_INTERVAL_SECONDS = 0.1f;
    public static final int ONE_SEC_IN_MILLS = 1000;

    // Default values for configuration
    public static final int DEFAULT_MAX_TOKENS = 1024;
    public static final double DEFAULT_TOP_P = 0.9;
    public static final double DEFAULT_TOP_T = 0.7;

    // System prompts for different languages
    public static final String ENGLISH_SYSTEM_PROMPT = """
You are a friendly assistant. The user and you will engage in a spoken dialog 
exchanging the transcripts of a natural real-time conversation. Keep your responses short, 
generally two or three sentences for chatty scenarios.
    """;

    public static final String SPANISH_SYSTEM_PROMPT = """
Eres un asistente amigable. El usuario y usted entablarán un diálogo hablado. 
intercambiando las transcripciones de una conversación natural en tiempo real. Mantenga sus respuestas breves, 
generalmente dos o tres oraciones para escenarios conversadores.
    """;

    public static final String FRENCH_SYSTEM_PROMPT = """
Vous êtes un assistant sympathique. L'utilisateur et vous engagerez un dialogue parlé 
échanger les transcriptions d’une conversation naturelle en temps réel. Gardez vos réponses courtes, 
généralement deux ou trois phrases pour les scénarios bavards.
    """;

    public static final String ITALIAN_SYSTEM_PROMPT = """
Sei un assistente amichevole. L'utente e tu ti impegnerai in un dialogo parlato 
scambiando le trascrizioni di una conversazione naturale in tempo reale. Mantieni le tue risposte brevi, 
generalmente due o tre frasi per scenari loquaci.
    """;

    public static final String GERMAN_SYSTEM_PROMPT = """
Sie sind ein freundlicher Assistent. Der Benutzer und Sie führen einen gesprochenen Dialog 
Austausch der Transkripte eines natürlichen Echtzeitgesprächs. Halten Sie Ihre Antworten kurz, 
im Allgemeinen zwei oder drei Sätze für gesprächige Szenarien.
    """;

    // Language codes
    public static final String LANG_EN_US = "en-US";
    public static final String LANG_EN_GB = "en-GB";
    public static final String LANG_FR = "fr";
    public static final String LANG_IT = "it";
    public static final String LANG_DE = "de";
    public static final String LANG_ES = "es";

    // Voice IDs
    public static final Map<String, String> VOICE_IDS = Map.ofEntries(
        Map.entry(LANG_EN_US + "_F", "tiffany"),
        Map.entry(LANG_EN_US + "_M", "matthew"),
        Map.entry(LANG_EN_GB, "amy"),          // GB English only has amy
        Map.entry(LANG_FR + "_F", "ambre"),
        Map.entry(LANG_FR + "_M", "florian"),
        Map.entry(LANG_IT + "_F", "beatrice"),
        Map.entry(LANG_IT + "_M", "lorenzo"),
        Map.entry(LANG_DE + "_F", "greta"),
        Map.entry(LANG_DE + "_M", "lennart"),
        Map.entry(LANG_ES + "_F", "lupe"),
        Map.entry(LANG_ES + "_M", "carlos")
    );

    public static final String DEFAULT_SYSTEM_PROMPT = ENGLISH_SYSTEM_PROMPT;

    // Nova Sonic response constants
    public static final String EVENT_KEY = "event";
    public static final String CONTENT_KEY = "content";
    public static final String ROLE_KEY = "role";
    public static final String USER_ROLE = "USER";

    // Output event types
    public static final String TEXT_OUTPUT = "textOutput";
    public static final String CONTENT_START = "contentStart";
    public static final String AUDIO_OUTPUT = "audioOutput";
    public static final String COMPLETION_END = "completionEnd";
    public static final String COMPLETION_START = "completionStart";
    public static final String USAGE_EVENT = "usageEvent";
    public static final String CONTENT_END = "contentEnd";
}
