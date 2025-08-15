package org.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.example.constants.NovaSonicConstants.SIXTEEN_BIT;
import static org.example.constants.NovaSonicConstants.VALID_CHANNELS;
import static org.example.constants.NovaSonicConstants.VALID_SAMPLE_RATES;

/**
 * Utility class for audio processing.
 */
public class AudioUtil {
    private static final Logger logger = LoggerFactory.getLogger(AudioUtil.class);

    private AudioUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Validates that the audio format meets the requirements for Nova Sonic processing.
     *
     * @param audioFormat The audio format to validate
     * @return True if the format is valid, false otherwise
     */
    public static boolean isValidAudioFormat(AudioFormat audioFormat) {
        boolean isValid = audioFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                && audioFormat.getChannels() == VALID_CHANNELS
                && audioFormat.getSampleSizeInBits() == SIXTEEN_BIT
            && VALID_SAMPLE_RATES.contains(audioFormat.getSampleRate());

        if (!isValid) {
            logger.error("Invalid audio format: {}. Must be PCM signed 16-bit, mono, with sample rate of 8/16/24kHz",
                    audioFormat);
        }

        return isValid;
    }

    /**
     * Gets the audio format from a file.
     *
     * @param audioFile The audio file
     * @return The audio format
     * @throws UnsupportedAudioFileException If the audio file format is not supported
     * @throws IOException If an I/O error occurs
     */
    public static AudioFormat getAudioFormat(File audioFile) throws UnsupportedAudioFileException, IOException {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
            return audioInputStream.getFormat();
        }
    }

    /**
     * Gets the duration of an audio file in milliseconds.
     *
     * @param audioFile The audio file
     * @return The duration in milliseconds
     * @throws UnsupportedAudioFileException If the audio file format is not supported
     * @throws IOException If an I/O error occurs
     */
    public static long getAudioDurationMillis(File audioFile) throws UnsupportedAudioFileException, IOException {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            return (long) (frames / format.getFrameRate() * 1000);
        }
    }

    /**
     * Creates an audio input stream from a file.
     *
     * @param audioFile The audio file
     * @return The audio input stream
     * @throws UnsupportedAudioFileException If the audio file format is not supported
     * @throws IOException If an I/O error occurs
     */
    public static AudioInputStream createAudioInputStream(File audioFile)
            throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioInputStream(audioFile);
    }

    /**
     * Reads a chunk of audio data from an audio input stream.
     *
     * @param audioInputStream The audio input stream
     * @param chunkSizeBytes The size of the chunk in bytes
     * @return A ByteBuffer containing the audio data, or an empty buffer if end of stream
     * @throws IOException If an I/O error occurs
     */
    public static ByteBuffer readAudioChunk(AudioInputStream audioInputStream, int chunkSizeBytes) throws IOException {
        byte[] buffer = new byte[chunkSizeBytes];
        int bytesRead = audioInputStream.read(buffer);

        if (bytesRead <= 0) {
            // End of stream
            return ByteBuffer.allocate(0);
        }

        if (bytesRead < chunkSizeBytes) {
            // Partial read, create a buffer of the exact size
            byte[] exactBuffer = new byte[bytesRead];
            System.arraycopy(buffer, 0, exactBuffer, 0, bytesRead);
            return ByteBuffer.wrap(exactBuffer);
        }

        return ByteBuffer.wrap(buffer);
    }

    /**
     * Resolves a file path relative to the current working directory.
     *
     * @param filePath The file path
     * @return The resolved file
     */
    public static File resolveFilePath(String filePath) {
        if (filePath.startsWith("~")) {
            filePath = System.getProperty("user.home") + filePath.substring(1);
        }

        Path path = Paths.get(filePath);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"), filePath);
        }

        return path.toFile();
    }
}
