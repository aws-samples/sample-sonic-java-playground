import { useState, useEffect } from 'react';
import axios from 'axios';
import './ConfigPanel.css';

/**
 * Component for configuring transcription parameters and managing connection
 */
const ConfigPanel = ({ onConfigChange, disabled, isConnected, onNewSession }) => {
  const [config, setConfig] = useState({
    maxTokens: 1024,
    topP: 0.9,
    topT: 0.7,
    systemPrompt: '',
    language: 'en-US',
    useFeminineVoice: false
  });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  /**
   * Fetches default configuration from the server
   */
  useEffect(() => {
    const fetchConfig = async () => {
      try {
        setIsLoading(true);
        const response = await axios.get('/api/transcription/config');
        setConfig(response.data);
        onConfigChange(response.data);
      } catch (err) {
        console.error('Error fetching configuration:', err);
        setError('Failed to load configuration. Using default values.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchConfig();
  }, [onConfigChange]);

  /**
   * Handles input changes
   */
  const handleInputChange = async (e) => {
    const { name, value } = e.target;
    
    // Convert numeric values
    let parsedValue = value;
    if (name === 'maxTokens') {
      parsedValue = value === '' ? '' : parseInt(value, 10);
      if (isNaN(parsedValue)) parsedValue = '';
    } else if (name === 'topP' || name === 'topT') {
      parsedValue = parseFloat(value) || 0;
    }
    
    // If language changes, fetch the corresponding prompt
    if (name === 'language') {
      try {
        const response = await axios.get(`/api/transcription/prompt/${value}`);
        const updatedConfig = {
          ...config,
          [name]: parsedValue,
          systemPrompt: response.data.systemPrompt,
          // Force feminine voice for GB English
          useFeminineVoice: parsedValue === 'en-GB' ? true : config.useFeminineVoice
        };
        setConfig(updatedConfig);
        onConfigChange(updatedConfig);
      } catch (err) {
        console.error('Error fetching language prompt:', err);
        // Still update the language even if prompt fetch fails
        const updatedConfig = {
          ...config,
          [name]: parsedValue,
          // Force feminine voice for GB English
          useFeminineVoice: parsedValue === 'en-GB' ? true : config.useFeminineVoice
        };
        setConfig(updatedConfig);
        onConfigChange(updatedConfig);
      }
    } else if (name === 'useFeminineVoice') {
      const updatedConfig = {
        ...config,
        [name]: value === 'true'  // Convert string 'true'/'false' to boolean
      };
      setConfig(updatedConfig);
      onConfigChange(updatedConfig);
    } else {
      const updatedConfig = {
        ...config,
        [name]: parsedValue
      };
      setConfig(updatedConfig);
      onConfigChange(updatedConfig);
    }
  };

  if (isLoading) {
    return <div className="loading">Loading configuration...</div>;
  }

  return (
    <div className="config-section">
      <div className="config-header">
        <h3>Configuration</h3>
        <button 
          className="new-session-button"
          onClick={onNewSession}
        >
          New Session
        </button>
      </div>
      
      {error && <div className="error-message">{error}</div>}
      
      <div className="config-form">
        <div className={`connection-status ${isConnected ? 'connected' : ''}`} style={{
          backgroundColor: isConnected ? '#e8f5e9' : '#f5f5f5',
          color: isConnected ? '#2e7d32' : '#757575'
        }}>
          {isConnected ? 'Connected' : 'Not Started'}
        </div>
        <div className="form-group">
          <label htmlFor="maxTokens">Max Tokens:</label>
          <input
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            id="maxTokens"
            name="maxTokens"
            value={config.maxTokens}
            onChange={handleInputChange}
            placeholder="Enter max tokens"
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="topP">Top P:</label>
          <input
            type="number"
            id="topP"
            name="topP"
            value={config.topP}
            onChange={handleInputChange}
            min="0"
            max="1"
            step="0.1"
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="topT">Temperature:</label>
          <input
            type="number"
            id="topT"
            name="topT"
            value={config.topT}
            onChange={handleInputChange}
            min="0"
            max="1"
            step="0.1"
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="language">Language:</label>
          <select
            id="language"
            name="language"
            value={config.language}
            onChange={handleInputChange}
            className="language-select"
          >
            <option value="en-US">English (US)</option>
            <option value="en-GB">English (GB)</option>
            <option value="fr">French</option>
            <option value="it">Italian</option>
            <option value="de">German</option>
            <option value="es">Spanish</option>
          </select>
        </div>

        {/* Only show voice selection if NOT English GB */}
        {config.language !== 'en-GB' && (
          <div className="form-group">
            <label htmlFor="voiceType">Voice Type:</label>
            <select
              id="voiceType"
              name="useFeminineVoice"
              value={config.useFeminineVoice}
              onChange={handleInputChange}
              className="voice-select"
            >
              <option value={false}>Masculine</option>
              <option value={true}>Feminine</option>
            </select>
          </div>
        )}

        <div className="form-group">
          <label htmlFor="systemPrompt">System Prompt:</label>
          <textarea
            id="systemPrompt"
            name="systemPrompt"
            value={config.systemPrompt}
            onChange={handleInputChange}
            rows="6"
            style={{ resize: 'vertical' }}
          />
        </div>
      </div>
    </div>
  );
};

export default ConfigPanel;
