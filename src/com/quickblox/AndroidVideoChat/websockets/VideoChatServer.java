package com.quickblox.AndroidVideoChat.websockets;

/**
 * Created with IntelliJ IDEA.
 * User: mardusdima
 * Date: 7/26/13
 * Time: 1:08 PM
 * To change this template use File | Settings | File Templates.
 */

import android.util.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 */
public class VideoChatServer extends WebSocketServer {
    static String TAG = VideoChatServer.class.getName();
    private int count = 0;
    private long lastTime = 0;


    public OnBinaryMessageReceive getOnMessageReceive() {
        return onMessageReceive;
    }

    public void setOnMessageReceive(OnBinaryMessageReceive onMessageReceive) {
        this.onMessageReceive = onMessageReceive;
    }

    OnBinaryMessageReceive onMessageReceive;

    public VideoChatServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    public VideoChatServer(InetSocketAddress address) {
        super(address);
    }

    public VideoChatServer(InetSocketAddress inetSocketAddress, List<Draft> drafts) {
        super(inetSocketAddress, drafts);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        this.sendToAll("new connection: " + handshake.getResourceDescriptor());
        Log.d(TAG, conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
        if (onMessageReceive != null) onMessageReceive.onServerConnected();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        this.sendToAll(conn + " has left the room!");
        Log.d(TAG, conn + " has left the room!");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // this.sendToAll(message);
        byte[] bytes = message.getBytes();
        count++;

        if (onMessageReceive != null) {
            if (bytes.length > 0)
                onMessageReceive.onMessage(bytes);
        }

        if (System.currentTimeMillis() - lastTime > 2000) {
            Log.w(TAG, "mps=" + (count / 2) + " data size =" + bytes.length);
            count = 0;
            lastTime = System.currentTimeMillis();
        }
        onMessageReceive.onMessage(bytes);
    }


    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        byte[] data = message.array();
        long timestamp = bytesToLong(Arrays.copyOfRange(data, data.length - 9, data.length - 1));
        //Log.d(TAG,"time is " + timestamp+ " cut time is "+System.currentTimeMillis());
        long diff;
        if ((diff = System.currentTimeMillis() - timestamp) > 500) {
            Log.d(TAG,"time is up so skip " + diff);
            return;
        }

        byte[] bytes = message.array();
        count++;

        if (onMessageReceive != null) {
            if (bytes.length > 0)
                onMessageReceive.onMessage(bytes);
        }

        if (System.currentTimeMillis() - lastTime > 1000) {
            Log.w(TAG, "mps=" + (count) + " data size =" + bytes.length);
            count = 0;
            lastTime = System.currentTimeMillis();
        }
        onMessageReceive.onMessage(bytes);

    }


    public byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }


    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.e(TAG, "error", ex);
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific websocket
        }
    }

    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text The String to send across the network.
     * @throws InterruptedException When socket related I/O errors occur.
     */
    public void sendToAll(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }

    public void sendToAll(byte[] data) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(data);
            }
        }
    }
}
