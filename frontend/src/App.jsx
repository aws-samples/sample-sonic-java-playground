import React, { useState, useCallback, useEffect, useRef } from 'react';
import axios from 'axios';
import './App.css';
import './components/AudioInput.css';
import TranscriptionView from './components/TranscriptionView';
import ASRView from './components/ASRView';
import ConfigPanel from './components/ConfigPanel';
import WebSocketEventManager from './lib/WebSocketEventManager';

function App() {
  const [messages, setMessages] = useState([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState(null);
  const wsManagerRef = useRef(null);
  const messageCounter = useRef(0);
  const MAX_MESSAGES = 100; // Limit message history for performance
  const [wsInitialized, setWsInitialized] = useState(false);
  const [config, setConfig] = useState({
    maxTokens: 1024,
    topP: 0.9,
    topT: 0.7,
    websocketEndpoint: '/ws/audio'
  });

  // Initialize WebSocket Manager
  useEffect(() => {
    const wsManager = WebSocketEventManager.getInstance('ws://localhost:8008/ws/audio');
    console.log('Initializing WebSocket Manager');
    wsManagerRef.current = wsManager;

    wsManager.onTranscriptionUpdate = (message) => {
      // Create unique message ID using timestamp, role, and counter
      messageCounter.current += 1;
      const messageId = `${Date.now()}-${message.role}-${messageCounter.current}`;
      const newMessage = {
        id: messageId,
        text: message.text,
        role: message.role,
        timestamp: Date.now()
      };

      setMessages(prev => {
        const newMessages = [...prev, newMessage];
        // Keep only the last N messages if limit exceeded
        return newMessages.length > MAX_MESSAGES 
          ? newMessages.slice(-MAX_MESSAGES) 
          : newMessages;
      });
    };

    wsManager.onStatusChange = (data) => {
      console.log('WebSocket status change:', data);
      if (data.status === 'processing') {
        setIsProcessing(true);
      } else if (data.status === 'completed') {
        setIsProcessing(false);
      } else if (data.status === 'connected') {
        console.log('WebSocket fully connected, enabling microphone...');
        setWsInitialized(true);
        setError(null);
      } else if (data.status === 'disconnected') {
        console.log('WebSocket disconnected...');
        setWsInitialized(false);
        setError(null);
      }
    };

    wsManager.onError = (message) => {
      setError(message);
      setIsProcessing(false);
    };
  }, []);

  // Load configuration on component mount
  useEffect(() => {
    const loadConfig = async () => {
      try {
        const response = await axios.get('/api/transcription/config');
        setConfig(prev => ({
          ...prev,
          ...response.data
        }));
      } catch (err) {
        console.error('Error loading configuration:', err);
        setError('Failed to load configuration. Please refresh the page.');
      }
    };
    loadConfig();
  }, []);

  const handleConfigChange = useCallback((newConfig) => {
    setConfig(prev => ({ ...prev, ...newConfig }));
  }, []);

  const handleNewSession = async () => {
    try {
      setMessages([]); // Clear messages
      messageCounter.current = 0; // Reset message counter
      if (wsInitialized) {
        await wsManagerRef.current.resetNovaSonicSession();
      }
      await wsManagerRef.current.connect(config);
    } catch (error) {
      console.error('Failed to start session:', error);
      setError('Failed to start session. Please try again.');
    }
  };


  return (
    <div className="App">
      <header className="App-header">
        <h1>Sonic Java Playground</h1>
      </header>
      
      <main className="App-content">
        <div className="chat-main">
          <div className="transcription-container">
            <TranscriptionView 
              messages={messages} 
              isLoading={isProcessing} 
              error={error}
              isConnected={wsInitialized}
              wsManager={wsManagerRef.current}
              isProcessing={isProcessing}
            />
          </div>
          <div className="asr-view-container">
            <ASRView messages={messages} />
          </div>
        </div>
        <div className="chat-sidebar">
          <ConfigPanel 
            onConfigChange={handleConfigChange} 
            disabled={isProcessing}
            isConnected={wsInitialized}
            onNewSession={handleNewSession}
          />
          {/* Audio input moved to TranscriptionView */}
        </div>
      </main>
    </div>
  );
}

export default App;
