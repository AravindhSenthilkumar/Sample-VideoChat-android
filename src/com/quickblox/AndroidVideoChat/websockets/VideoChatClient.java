package com.quickblox.AndroidVideoChat.websockets;

import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: mardusdima
 * Date: 7/26/13
 * Time: 12:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class VideoChatClient extends WebSocketClient {
    private static final String LOGTAG = VideoChatClient.class.getName();
    long lastTime;
    long count = 0;

    public VideoChatClient(URI serverURI) {
        super(serverURI);
    }

    public OnBinaryMessageReceive getOnMessageReceive() {
        return onMessageReceive;
    }

    public void setOnMessageReceive(OnBinaryMessageReceive onMessageReceive) {
        this.onMessageReceive = onMessageReceive;
    }

    OnBinaryMessageReceive onMessageReceive;

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        Log.d(LOGTAG, "Connected!");
        lastTime = System.currentTimeMillis();
        if (onMessageReceive != null) onMessageReceive.onServerConnected();
    }

    @Override
    public void onMessage(String message) {
        Log.d(LOGTAG, String.format("Got string message! %s", message));
    }

    @Override
    public void onMessage(ByteBuffer data) {
        //Log.d(LOGTAG, String.format("Got binary message!"));

        count++;

        if (onMessageReceive != null) {
            byte[] bytes = data.array();
            if (bytes.length > 0)
                onMessageReceive.onMessage(bytes);

        }

        if (System.currentTimeMillis() - lastTime > 1000) {
            Log.w(LOGTAG, "mps=" + count  + " data size =" + data.array().length);
            count = 0;
            lastTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean b) {
        Log.d(LOGTAG, String.format("Disconnected! Code: %d Reason: %s", code, reason));
    }

    @Override
    public void onError(Exception error) {
        Log.e(LOGTAG, "Error!", error);
    }


};
