# NovaSonicPlayground
This is sample code, for non-production usage. You should work with your security and legal teams to meet your organizational security, regulatory and compliance requirements before deployment

NovaSonicPlayground is a full-stack playground application for experimenting with Amazon's NovaSonic API capabilities, built with React (v18.2.0) and Spring Boot (v3.2.0). The application supports invoking NovaSonic Speech to Speech model by configuring supported paratmeters.

## Features

### Core Functionality
- Interactive chat interface
- Real-time ASR transcription
- WebSocket-based bidirectional communication
- Audio capture and playback
- Configuring model parameters
- Session management and cleanup

## Prerequisites

- Java 17 or higher
- Node.js 14 or higher
- npm 6 or higher
- Maven 3.6 or higher
- AWS credentials with Bedrock Runtime API access
- Browser Compatibility:
  * Recommended: Google Chrome (latest version)
  * Also works on: Safari
  * Not supported: Firefox

## Dependencies

### Backend
- Spring Boot 3.2.0
- AWS SDK 2.31.33
- RxJava 3.1.6

### Frontend
- React 18.2.0
- Axios 1.4.0
- React Scripts 5.0.1

## Project Structure

```
├── frontend/              # React frontend application
│   ├── public/           # Static assets
│   │   ├── index.html
│   │   └── manifest.json
│   └── src/              # Frontend source code
│       ├── components/   # React components
│       │   ├── ASRView.jsx
│       │   ├── AudioInput.jsx
│       │   ├── ConfigPanel.jsx
│       │   └── TranscriptionView.jsx
│       ├── lib/         # Audio processing utilities
│       │   ├── AudioPlayer.js
│       │   ├── AudioPlayerProcessor.worklet.js
│       │   └── WebSocketEventManager.js
│       └── util/        # Helper utilities
│           └── ObjectsExt.js
├── logs/                 # Application logs
├── src/                  # Backend source code
│   └── main/
│       ├── java/org/example/
│       │   ├── api/     # REST controllers and services
│       │   ├── client/  # NovaSonic client implementation
│       │   ├── config/  # Application configuration
│       │   ├── constants/# Application constants
│       │   ├── handler/ # WebSocket and event handlers
│       │   └── util/    # Utility classes
│       └── resources/   # Application resources
│           ├── application.properties
│           └── logback.xml
├── pom.xml              # Maven configuration
└── run.sh              # Application launcher script
```

## Quick Start

1. Clone the repository:
   ```bash
   git clone [repository-url]
   cd NovaSonicPlayground
   ```

2. Configure AWS credentials in `~/.aws/credentials` using command `aws configure`:
   ```
   AWS_ACCESS_KEY_ID=your-access-key
   AWS_SECRET_ACCESS_KEY=your-secret-key
   AWS_REGION=us-east-1
   ```

3. Run the application:
   ```bash
   ./run.sh
   ```

4. Open your browser and navigate to http://localhost:3000

## Development Setup

### Backend (Spring Boot)

1. Build the backend:
   ```bash
   mvn clean package
   ```

2. Run the backend:
   ```bash
   java -jar target/NovaSonicPlayground-1.0-SNAPSHOT.jar
   ```

The backend server will start on port 8008.

### Frontend (React)

1. Install dependencies:
   ```bash
   cd frontend
   npm install
   ```

2. Start development server:
   ```bash
   npm start
   ```

The frontend development server will start on port 3000.

## Building and Running

### Using run.sh (Recommended)

The `run.sh` script handles building and running both frontend and backend:
- Checks for required dependencies
- Builds the backend with Maven
- Installs frontend dependencies
- Starts both servers
- Provides cleanup on exit

### Manual Build and Run

1. Build the complete application:
   ```bash
   mvn clean package
   cd frontend && npm install && npm run build
   ```

2. Run in production mode:
   ```bash
   java -jar target/NovaSonicPlayground-1.0-SNAPSHOT.jar
   ```

## Configuration

### Model Configuration

All parameters are configurable through the web interface:

#### Model Parameters
- MaxTokens: Controls response length (default: 1024)
- TopP: Controls sampling diversity (default: 0.9)
- Temperature: Controls response randomness (default: 0.7)
- System Prompt: Default prompts for supported languages are added to the application.

#### Language Settings
The language is configured used voice Id. supported voice ids are described here https://docs.aws.amazon.com/nova/latest/userguide/available-voices.html
- Supported Languages:
  - English (US)
  - English (GB)
  - French
  - Italian
  - German
  - Spanish


### Troubleshooting

Common issues and solutions:
Nova sonic has connection timelimit of 8 minutes.
1. Connection Issues
   ```
   Error: WebSocket connection failed
   Solution: Check if backend server is running on port 8008
   ```

2. Audio Capture Problems
   ```
   Error: No audio input detected
   Solution: Check browser permissions and microphone settings
   ```

3. Transcription Delays
   ```
   Issue: High latency in transcription
   Solution: Check network connection and reduce MaxTokens if needed
   ```

## Environment Setup

### Environment Variables
```bash
# Required AWS Configuration
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=us-east-1
```

### Proxy Configuration
The frontend development server is configured to proxy requests to the backend:
```json
{
  "proxy": "http://localhost:8008"
}
```
This configuration is already included in package.json and handles:
- API requests
- WebSocket connections
- CORS requirements

## API Documentation

### REST Endpoints

- `GET /api/transcription/config` - Get default configuration
- `GET /api/transcription/prompt/{language}` - Get system prompt for specified language

### WebSocket Protocol

- Endpoint: `ws://localhost:8008/ws/audio`
- Binary messages: Audio data chunks
- Text messages: Control commands and transcription results

Message Types:
- `status`: Connection and processing status
- `transcription`: Real-time transcription updates
- `audio`: Audio response data
- `error`: Error messages

## Logging

- Application logs: `logs/novasonic-playground.log`
- Rolling file configuration: Daily rollover
- Log levels configurable in `src/main/resources/logback.xml`

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This library is licensed under the MIT-0 License. See the LICENSE file.

