import { useState, useRef } from 'react';
import './AudioInput.css';

/**
 * Component for handling microphone audio input
 */
const AudioInput = ({ wsManager, isProcessing }) => {
  const [isRecording, setIsRecording] = useState(false);
  const [recordingError, setRecordingError] = useState(null);
  const audioContextRef = useRef(null);
  const sourceRef = useRef(null);
  const processorRef = useRef(null);
  const streamRef = useRef(null);

  /**
   * Starts audio recording
   */
  const startRecording = async () => {
    try {
      if (!wsManager?.isConnected) {
        setRecordingError('Not connected to server. Please wait or refresh the page.');
        return;
      }

      setRecordingError(null);

      // Initialize audio player on user gesture
      try {
        await wsManager.audioPlayer.start();
      } catch (error) {
        console.error('Error initializing audio:', error);
        setRecordingError('Error initializing audio. Please try again.');
        return;
      }
      
      const stream = await navigator.mediaDevices.getUserMedia({ 
        audio: {
          channelCount: 1,
          sampleRate: 24000,
          sampleSize: 16,
          echoCancellation: true  // Enable browser's echo cancellation
          // noiseSuppression: true,  // Enable noise suppression
          // autoGainControl: true    // Enable automatic gain control
        } 
      });

      // Create audio context and processing pipeline
      const audioContext = new AudioContext({
        sampleRate: 24000,
        latencyHint: 'interactive'
      });

      const source = audioContext.createMediaStreamSource(stream);
      const processor = audioContext.createScriptProcessor(512, 1, 1);

      processor.onaudioprocess = (e) => {
        if (wsManager?.isConnected) {
          const inputData = e.inputBuffer.getChannelData(0);
          // Convert float32 to int16
          const pcmData = new Int16Array(inputData.length);
          for (let i = 0; i < inputData.length; i++) {
            pcmData[i] = Math.max(-1, Math.min(1, inputData[i])) * 0x7FFF;
          }
          wsManager.sendAudioChunk(pcmData.buffer);
        }
      };

      source.connect(processor);
      processor.connect(audioContext.destination);
      
      // Store references for cleanup
      audioContextRef.current = audioContext;
      sourceRef.current = source;
      processorRef.current = processor;
      streamRef.current = stream;
      
      setIsRecording(true);
    } catch (error) {
      console.error('Error accessing microphone:', error);
      setRecordingError('Error accessing microphone. Please ensure you have granted permission.');
    }
  };

  /**
   * Stops audio recording
   */
  const stopRecording = () => {
    if (isRecording) {
      // Disconnect audio processing pipeline
      if (sourceRef.current && processorRef.current && audioContextRef.current) {
        sourceRef.current.disconnect(processorRef.current);
        processorRef.current.disconnect(audioContextRef.current.destination);
      }

      // Stop and cleanup media stream
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(track => track.stop());
      }

      // Send stop signal to server
      wsManager?.stop();

      setIsRecording(false);
    }
  };

  /**
   * Toggles recording state
   */
  const toggleRecording = () => {
    if (isRecording) {
      stopRecording();
    } else {
      startRecording();
    }
  };

  return (
    <div className="audio-controls">
      <h3 className="audio-section-title">Voice Input</h3>
      <div className="audio-input-row">
        <div className="mic-container">
          <button 
            className={`mic-button ${isRecording ? 'recording' : ''}`}
            onClick={toggleRecording}
            disabled={!wsManager || isProcessing || !wsManager.isConnected}
            title={!wsManager ? 'Initializing...' : (isRecording ? 'Stop Recording' : 'Start Recording')}
          >
            <svg viewBox="0 0 24 24" className="mic-icon">
              <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
              <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
            </svg>
          </button>
          
          {isRecording && (
            <div className="recording-indicator">
              <div className="pulse"></div>
            </div>
          )}
        </div>

        <div className="status-container">
          <div className={`connection-status ${wsManager ? (wsManager.isConnected ? 'connected' : 'disconnected') : 'disconnected'}`}>
            {!wsManager ? 'Initializing...' : (wsManager.isConnected ? 'Connected' : 'Connecting...')}
          </div>
        </div>
      </div>

      {recordingError && (
        <div className="error-message">{recordingError}</div>
      )}
    </div>
  );
};

export default AudioInput;
