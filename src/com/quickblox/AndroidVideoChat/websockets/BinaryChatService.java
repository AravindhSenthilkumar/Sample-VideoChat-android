package com.quickblox.AndroidVideoChat.websockets;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.codebutler.android_websockets.WebSocketClient;
import org.apache.http.message.BasicNameValuePair;

import java.net.URI;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: mardusdima
 * Date: 7/4/13
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class BinaryChatService extends Service {

    public final static String START_CHAT = "com.example.WebSocketsSimpleChat.START_CHAT";
    public final static String STOP_CHAT = "com.example.WebSocketsSimpleChat.STOP_CHAT";
    public final static String SEND_MESSAGE = "com.example.WebSocketsSimpleChat.SEND_MESSAGE";
    public final static String SERVER_URL = "http://54.234.173.172:8000";


    public final static String CONNECTED = "connected";
    public final static String USER_JOINED = "userJoined";
    public final static String MESSAGE_SEND = "messageSent";
    public final static String MESSAGE_RECEIVED = "messageReceived";

    public final static String USER_SPLIT = "userSplit";
    private static final String LOGTAG = BinaryChatService.class.getName();


    private static WebSocketClient socket;
    boolean chatActivated = false;

    public ArrayList<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<ChatMessage> messages) {
        this.messages = messages;
    }

    ArrayList<ChatMessage> messages;
    private Timer reconnectTask;

    @Override
    public void onCreate() {
        messages = new ArrayList<ChatMessage>();
        List<BasicNameValuePair> extraHeaders = Arrays.asList(
                new BasicNameValuePair("Cookie", "ssl=false")
        );


        socket = new WebSocketClient(URI.create("ws://192.168.0.116:8000"), new WebSocketClient.Listener() {
            long lastTime;
            long count = 0;

            @Override
            public void onConnect() {
                Log.d(LOGTAG, "Connected!");
                lastTime = System.currentTimeMillis();
                if (onMessageReceive != null) onMessageReceive.onServerConnected();
            }

            @Override
            public void onMessage(String message) {
                Log.d(LOGTAG, String.format("Got string message! %s", message));
            }

            @Override
            public void onMessage(final byte[] data) {
                //Log.d(LOGTAG, String.format("Got binary message!"));

                count++;

                if (onMessageReceive != null) {
                    onMessageReceive.onMessage(data);
                }

                if (System.currentTimeMillis() - lastTime > 2000) {
                    Log.w(LOGTAG, "mps=" + (count / 2) + " data size =" + data.length);
                    count = 0;
                    lastTime = System.currentTimeMillis();
                }

            }

            @Override
            public void onDisconnect(int code, String reason) {
                Log.d(LOGTAG, String.format("Disconnected! Code: %d Reason: %s", code, reason));
            }

            @Override
            public void onError(Exception error) {
                Log.e(LOGTAG, "Error!", error);
            }

        }, extraHeaders);

        super.onCreate();
    }


    private final IBinder myBinder = new MyLocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    public class MyLocalBinder extends Binder {
        public BinaryChatService getService() {
            return BinaryChatService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(START_CHAT)) {
            Log.i(LOGTAG, "start chat");
            if (!chatActivated)
                socket.connect();
            chatActivated = true;
        } else if (intent.getAction().equals(STOP_CHAT)) {
            Log.i(LOGTAG, "stop chat");
            chatActivated = false;
            socket.disconnect();
            stopSelf();
        } else if (intent.getAction().equals(SEND_MESSAGE)) {
            Log.i(LOGTAG, "send message");
            String message = intent.getStringExtra("message");
            try {
                socket.send(message);
            } catch (Exception e) {
                Log.e(LOGTAG, "socket_error", e);
                if (!socket.isConnected()) {
                    startReconnectTask();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    public void sendMessage(final byte[] message) {

        try {
            socket.send(message);
        } catch (Exception e) {
            Log.e(LOGTAG, "socket_error", e);
            if (!socket.isConnected()) {
                startReconnectTask();
            }
        }

    }

    synchronized private void startReconnectTask() {
        Log.i(LOGTAG, "call reconnect");
        if (reconnectTask == null) {
            reconnectTask = new Timer();
            reconnectTask.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (chatActivated) {
                        socket.connect();
                    }
                }
            }, 5000, 5000);
        }
    }

    synchronized private void stopReconnectTask() {
        if (reconnectTask != null) {
            reconnectTask.cancel();
            reconnectTask = null;
        }
    }

    public OnBinaryMessageReceive getOnMessageReceive() {
        return onMessageReceive;
    }

    public void setOnMessageReceive(OnBinaryMessageReceive onMessageReceive) {
        this.onMessageReceive = onMessageReceive;
    }

    OnBinaryMessageReceive onMessageReceive;

    public interface OnBinaryMessageReceive {
        void onMessage(byte[] message);

        void onServerConnected();
    }
}


