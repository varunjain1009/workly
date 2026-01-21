package com.workly.helpseeker.data.network;

import android.util.Log;
import com.google.gson.Gson;
import com.workly.helpseeker.data.model.ChatMessage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Singleton
public class WebSocketManager {
    private static final String TAG = "WebSocketManager";

    private final String wsUrl;
    private WebSocket webSocket;
    private final OkHttpClient client;
    private final Gson gson;
    private MessageListener messageListener;

    public interface MessageListener {
        void onMessageReceived(ChatMessage message);
    }

    @Inject
    public WebSocketManager(Properties properties) {
        this.wsUrl = properties.getProperty("chat.url", "ws://10.0.2.2:8082/ws/chat");
        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        gson = new Gson();
    }

    public void connect(String userId, MessageListener listener) {
        this.messageListener = listener;
        Request request = new Request.Builder()
                .url(wsUrl + "?userId=" + userId)
                .build();
        webSocket = client.newWebSocket(request, new WorklyWebSocketListener());
    }

    public void sendMessage(ChatMessage message) {
        if (webSocket != null) {
            String json = gson.toJson(message);
            webSocket.send(json);
        }
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "User disconnected");
        }
    }

    private class WorklyWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d(TAG, "WebSocket Connected");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.d(TAG, "Message Received: " + text);
            try {
                ChatMessage message = gson.fromJson(text, ChatMessage.class);
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing message", e);
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.e(TAG, "WebSocket Failure", t);
        }
    }
}
