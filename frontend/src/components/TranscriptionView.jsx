import React, { useEffect, useRef, memo } from 'react';
import AudioInput from './AudioInput';

// Memoized message component for better performance
const Message = memo(({ message, isLoading, isLast }) => (
  <div 
    className={`message-wrapper ${message.role === 'USER' ? 'user' : 'assistant'}`}
  >
    <div className="message-bubble">
      {message.text}
      {isLast && isLoading && (
        <span className="cursor">|</span>
      )}
    </div>
  </div>
));

/**
 * Component for displaying transcription results in a chat-like interface
 */
const TranscriptionView = ({ messages, isLoading, error, isConnected, wsManager, isProcessing }) => {
  const chatContainerRef = useRef(null);

  // Auto-scroll to bottom when new messages are added
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  }, [messages]);

  return (
    <div className="chat-section">
      <div className="chat-header">
        <h3>Conversation</h3>
        <div className="voice-control-bar">
          <AudioInput 
            wsManager={wsManager}
            isProcessing={isProcessing}
          />
        </div>
      </div>
      
      <div className="chat-container" ref={chatContainerRef}>
        {error ? (
          <div className="error-message">{error}</div>
        ) : (
          <div className="messages-list">
            {messages.map((message, index) => (
              <Message
                key={message.id || index}
                message={message}
                isLoading={isLoading}
                isLast={index === messages.length - 1}
              />
            ))}
            {messages.length === 0 && !isLoading && (
              <div className={`placeholder ${!isConnected ? 'highlight-action' : ''}`}>
                {!isConnected 
                  ? "Please click 'New Session' in the configuration panel to begin conversation"
                  : "Record audio to test Nova Sonic"
                }
              </div>
            )}
            {messages.length === 0 && isLoading && (
              <div className="placeholder">
                Processing audio...
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default TranscriptionView;
