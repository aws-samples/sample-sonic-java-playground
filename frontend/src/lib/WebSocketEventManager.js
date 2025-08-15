import AudioPlayer from './AudioPlayer';

class WebSocketEventManager {
    static instance = null;

    static getInstance(wsUrl) {
        if (!WebSocketEventManager.instance) {
            WebSocketEventManager.instance = new WebSocketEventManager(wsUrl);
        }
        return WebSocketEventManager.instance;
    }

    constructor(wsUrl) {
        if (WebSocketEventManager.instance) {
            return WebSocketEventManager.instance;
        }

        this.wsUrl = wsUrl;
        this.socket = null;
        this.audioPlayer = new AudioPlayer();
        this.isConnected = false;
        this.isProcessing = false;
        this.onTranscriptionUpdate = null;
        this.onStatusChange = null;
        this.onError = null;
        this.isInitialized = false;

        WebSocketEventManager.instance = this;
    }

    async connect(config = {}) {
        if (this.isInitialized) {
            console.log('WebSocket already initialized, reusing existing connection');
            return;
        }

        if (this.socket) {
            this.socket.close();
        }

        try {
            console.log('Connecting to WebSocket:', this.wsUrl);
            // Add configuration parameters to the URL
            const configParams = new URLSearchParams({
                maxTokens: config.maxTokens || 1024,
                topP: config.topP || 0.9,
                topT: config.topT || 0.7,
                systemPrompt: config.systemPrompt || '',
                language: config.language || 'en-US',
                useFeminineVoice: config.useFeminineVoice || false
            }).toString();
            
            const wsUrlWithConfig = `${this.wsUrl}?${configParams}`;
            this.socket = new WebSocket(wsUrlWithConfig);
            this.setupSocketListeners();
            this.isInitialized = true;
        } catch (error) {
            console.error('Error connecting to WebSocket:', error);
            this.onError?.('Failed to connect to server. Please check if the server is running on port 8008.');
            throw error;
        }
    }

    setupSocketListeners() {
        this.socket.onopen = () => {
            console.log('WebSocket Connected, waiting for backend initialization...');
            this.isConnected = false;
            this.onStatusChange?.({ status: 'connecting' });
            this.audioPlayer.start();
        };

        this.socket.onmessage = async (event) => {
            try {
                const data = JSON.parse(event.data);
                await this.handleMessage(data);
            } catch (error) {
                console.error('Error handling WebSocket message:', error);
                this.onError?.('Error processing server response');
            }
        };

        this.socket.onclose = () => {
            console.log('WebSocket disconnected');
            this.isConnected = false;
            this.onStatusChange?.({ status: 'disconnected' });
        };

        this.socket.onerror = (error) => {
            console.error('WebSocket error:', error);
            this.isConnected = false;
            this.onStatusChange?.({ status: 'disconnected' });
            this.onError?.('Connection error. Please ensure the server is running on port 8008.');
        };
    }

     base64ToFloat32Array(base64String) {
        const binaryString = window.atob(base64String);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }

        const int16Array = new Int16Array(bytes.buffer);
        const float32Array = new Float32Array(int16Array.length);
        for (let i = 0; i < int16Array.length; i++) {
            float32Array[i] = int16Array[i] / 32768.0;
        }

        return float32Array;
    }

    async handleMessage(data) {
        console.log("Received WebSocket message:", {
            type: data.type,
            status: data.status,
            role: data.role,
            timestamp: new Date().toISOString()
        });
        switch (data.type) {
            case 'transcription':
                console.log('Transcription: ', data.text, data.role);
                this.onTranscriptionUpdate?.({
                    text: data.text,
                    role: data.role || 'USER'
                });
                break;

            case 'status':
                if (data.status === 'ready' || data.status === 'connected') {
                    console.log('Backend ready and connected');
                    this.isConnected = true;
                    this.onStatusChange?.({ status: 'connected' });
                } else if (data.status === 'processing') {
                    this.isProcessing = true;
                    this.onStatusChange?.(data);
                } else if (data.status === 'completed') {
                    this.isProcessing = false;
                    this.onStatusChange?.(data);
                } else {
                    this.onStatusChange?.(data);
                }
                break;

            case 'audio':
                if (data.data) {
                    console.log('Received audio data from Nova Sonic (24000 Hz sample rate)');
                    await this.audioPlayer.playAudio(this.base64ToFloat32Array(data.data));
                }
                break;

            case 'error':
                this.onError?.(data.message);
                this.isProcessing = false;
                break;

            default:
                console.warn('Unknown message type:', data.type);
        }
    }

    sendAudioChunk(audioData) {
        if (this.socket?.readyState === WebSocket.OPEN) {
            this.socket.send(audioData);
        }
    }

    stop() {
        if (this.socket?.readyState === WebSocket.OPEN) {
            this.socket.send('stop');
        }
    }

    disconnect() {
        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }
        // this.audioPlayer.stop();
        this.isConnected = false;
        this.isProcessing = false;
        this.isInitialized = false;
        WebSocketEventManager.instance = null;
    }

    async resetNovaSonicSession() {
        if (this.socket?.readyState === WebSocket.OPEN) {
            // Send a message to close Nova Sonic session but keep WebSocket open
            this.socket.send('reset_session');
            // Clear the initialized flag so we can reinitialize with new config
            this.isInitialized = false;
        }
    }
}

export default WebSocketEventManager;
