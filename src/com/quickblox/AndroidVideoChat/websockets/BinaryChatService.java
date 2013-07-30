package com.quickblox.AndroidVideoChat.websockets;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_10;

import java.net.URI;
import java.util.Timer;

/**
 * Created with IntelliJ IDEA.
 * User: mardusdima
 * Date: 7/4/13
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class BinaryChatService extends Service implements ChatService {


    private static final String LOGTAG = BinaryChatServerService.class.getName();
    private static String ADDR = "ws://%server%:8887";

    private static VideoChatClient client;

    boolean chatActivated = false;

    private Timer reconnectTask;

    @Override
    public void onCreate() {
        Log.d(LOGTAG, "OnCreate");
        super.onCreate();
    }

    Draft draft = new Draft_10();

    private final IBinder myBinder = new MyLocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        String serverAdr = intent.getStringExtra("server");
        if (serverAdr != null) {
            ADDR = ADDR.replace("%server%", serverAdr);
        } else {
            ADDR = ADDR.replace("%server%", "127.0.0.1");
        }
        Log.d(LOGTAG, "server = " + ADDR);
        client = new VideoChatClient(URI.create(ADDR));//,draft);

        return myBinder;
    }

    public void setOnMessageReceive(OnBinaryMessageReceive onMessageReceive) {

        client.setOnMessageReceive(onMessageReceive);

    }

    public class MyLocalBinder extends Binder {
        public BinaryChatService getService() {
            return BinaryChatService.this;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(START_CHAT)) {

            if (!chatActivated) {
                client.connect();
            }
            Log.i(LOGTAG, "start client");
            chatActivated = true;

        } else if (intent.getAction().equals(STOP_CHAT)) {
            Log.i(LOGTAG, "stop chat");
            chatActivated = false;

            client.close();

            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }


    public void sendMessage(final byte[] message) {

        try {
            client.send(message);

        } catch (Exception e) {
            Log.e(LOGTAG, "socket_error", e);

        }

    }


//    synchronized private void startReconnectTask() {
//        Log.i(LOGTAG, "call reconnect");
//        if (reconnectTask == null) {
//            reconnectTask = new Timer();
//            reconnectTask.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    if (chatActivated) {
//                        socket.connect();
//                    }
//                }
//            }, 5000, 5000);
//        }
//    }

//    synchronized private void stopReconnectTask() {
//        if (reconnectTask != null) {
//            reconnectTask.cancel();
//            reconnectTask = null;
//        }
//    }

}


