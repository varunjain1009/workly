package com.workly.helpprovider.data.network;
import java.util.concurrent.TimeUnit;

import android.util.Log;
import com.google.gson.Gson;
import com.workly.helpprovider.data.model.ChatMessage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Properties;

@Singleton
public class WebSocketManager {
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
        client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build();
        gson = new Gson();
    }

    public void connect(String userId, MessageListener listener) {
        this.messageListener = listener;
        Request request = new Request.Builder().url(wsUrl + "?userId=" + userId).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    ChatMessage message = gson.fromJson(text, ChatMessage.class);
                    if (messageListener != null)
                        messageListener.onMessageReceived(message);
                } catch (Exception e) {
                    Log.e("WS", "Error", e);
                }
            }
        });
    }

    public void sendMessage(ChatMessage message) {
        if (webSocket != null)
            webSocket.send(gson.toJson(message));
    }

    public void disconnect() {
        if (webSocket != null)
            webSocket.close(1000, "Bye");
    }
}
