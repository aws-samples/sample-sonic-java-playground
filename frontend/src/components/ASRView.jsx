import { useEffect, useRef } from 'react';

const ASRView = ({ messages }) => {
  const containerRef = useRef(null);
  const userMessages = messages.filter(msg => msg.role === 'USER');
  const textContent = userMessages.map(msg => msg.text).join('\n');

  // Auto-scroll to bottom when new messages are added
  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [userMessages]);

  return (
    <div className="asr-section">
      <h3>User Speech Transcription</h3>
      <div className="asr-container" ref={containerRef}>
        {userMessages.length === 0 ? (
          <div className="placeholder">
            No transcriptions yet. Start speaking to see ASR results.
          </div>
        ) : (
          <pre className="asr-text">
            {textContent}
          </pre>
        )}
      </div>
    </div>
  );
};

export default ASRView;
