package com.quickblox.AndroidVideoChat.websockets;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.clwillingham.socket.io.IOSocket;
import com.clwillingham.socket.io.MessageCallback;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: mardusdima
 * Date: 7/4/13
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatService extends Service {

    public final static String START_CHAT = "com.example.WebSocketsSimpleChat.START_CHAT";
    public final static String STOP_CHAT = "com.example.WebSocketsSimpleChat.STOP_CHAT";
    public final static String SEND_MESSAGE = "com.example.WebSocketsSimpleChat.SEND_MESSAGE";
    public final static String SERVER_URL = "http://54.234.173.172:8000";


    public final static String CONNECTED = "connected";
    public final static String USER_JOINED = "userJoined";
    public final static String MESSAGE_SEND = "messageSent";
    public final static String MESSAGE_RECEIVED = "messageReceived";

    public final static String USER_SPLIT = "userSplit";
    private static final String LOGTAG = ChatService.class.getName();


    private static IOSocket socket;
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
        socket = new IOSocket(SERVER_URL, new MessageCallback() {
            @Override
            public void on(String s, JSONObject... jsonObjects) {

            }

            @Override
            public void onMessage(String s) {

            }

            @Override
            public void onMessage(JSONObject jsonObject) {
                Log.v(LOGTAG, "on Message:" + jsonObject);
                String event = null;
                try {
                    event = jsonObject.getString("event");
                    ChatMessage message = new ChatMessage();
                    if (event == null) return;
                    message.setTime(jsonObject.getString("time"));
                    message.setFrom(jsonObject.getString("name"));
                    message.setType(event);
                    if (event.equals(MESSAGE_RECEIVED) || event.equals(MESSAGE_SEND)) {
                        // message.setText(Base64.ejsonObject.getString("text"));
                        message.setBinaryBody(Base64.decode(jsonObject.getString("text"), Base64.DEFAULT));
                    }
                    messages.add(message);
                    if (onMessageReceive != null) onMessageReceive.onMessage(message);
                } catch (JSONException e) {
                    Log.e(LOGTAG, "error", e);
                }


            }

            @Override
            public void onConnect() {
                Log.v(LOGTAG, "connect");
                stopReconnectTask();
            }

            @Override
            public void onDisconnect() {
                Log.v(LOGTAG, "disconnect");
                if (chatActivated)
                    startReconnectTask();
            }

            @Override
            public void onConnectFailure() {
                Log.v(LOGTAG, "connection failure");
                if (chatActivated)
                    startReconnectTask();

            }
        });
        super.onCreate();    //To change body of overridden methods use File | Settings | File Templates.
    }


    private final IBinder myBinder = new MyLocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    public class MyLocalBinder extends Binder {
        public ChatService getService() {
            return ChatService.this;
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
            } catch (IOException e) {
                Log.e(LOGTAG, "socket_error", e);
                if (!socket.isConnected()) {
                    startReconnectTask();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

//    public void sendMessage(String message) {
//        try {
//            socket.send(message);
//        } catch (IOException e) {
//            Log.e(LOGTAG, "socket_error", e);
//            if (!socket.isConnected()) {
//                startReconnectTask();
//            }
//        }
//    }

    public void sendMessage(final byte[] message) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket.send(Base64.encodeToString(message, Base64.DEFAULT));
                } catch (IOException e) {
                    Log.e(LOGTAG, "socket_error", e);
                    if (!socket.isConnected()) {
                        startReconnectTask();
                    }
                }
            }
        }).start();


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

    public OnMessageReceive getOnMessageReceive() {
        return onMessageReceive;
    }

    public void setOnMessageReceive(OnMessageReceive onMessageReceive) {
        this.onMessageReceive = onMessageReceive;
    }

    OnMessageReceive onMessageReceive;

    public interface OnMessageReceive {
        void onMessage(ChatMessage message);
    }
}


