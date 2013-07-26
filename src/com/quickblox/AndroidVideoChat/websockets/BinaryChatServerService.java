package com.quickblox.AndroidVideoChat.websockets;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: mardusdima
 * Date: 7/4/13
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class BinaryChatServerService extends Service implements ChatService{

    private static final String LOGTAG = BinaryChatServerService.class.getName();


    private static VideoChatServer server;
    boolean chatActivated = false;



    @Override
    public void onCreate() {
        Log.d(LOGTAG, "OnCreate");
        super.onCreate();
    }


    private final IBinder myBinder = new MyLocalBinder();

    @Override
    public IBinder onBind(Intent intent) {

        server = new VideoChatServer(new InetSocketAddress(8887));
        return myBinder;
    }

    public void setOnMessageReceive(OnBinaryMessageReceive onMessageReceive) {
        server.setOnMessageReceive(onMessageReceive);
    }

    public class MyLocalBinder extends Binder {
        public BinaryChatServerService getService() {
            return BinaryChatServerService.this;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(START_SERVER)) {

            if (!chatActivated) {
                server.start();
            }
            Log.i(LOGTAG, "start server");
            chatActivated = true;
        } else if (intent.getAction().equals(STOP_CHAT)) {
            Log.i(LOGTAG, "stop chat");
            chatActivated = false;

            try {
                server.stop();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }


    public void sendMessage(final byte[] message) {

        try {

            server.sendToAll(message);

        } catch (Exception e) {
            Log.e(LOGTAG, "socket_error", e);

        }

    }


}


