package com.quickblox.AndroidVideoChat.websockets;

/**
 * Created with IntelliJ IDEA.
 * User: mardusdima
 * Date: 7/26/13
 * Time: 4:01 PM
 * To change this template use File | Settings | File Templates.
 */
public interface  ChatService {

    public final static String START_CHAT = "com.example.WebSocketsSimpleChat.START_CHAT";
    public final static String START_SERVER = "com.example.WebSocketsSimpleChat.START_SERVER";
    public final static String STOP_CHAT = "com.example.WebSocketsSimpleChat.STOP_CHAT";
    public final static String SEND_MESSAGE = "com.example.WebSocketsSimpleChat.SEND_MESSAGE";

    void setOnMessageReceive(OnBinaryMessageReceive onMessageReceive);

    void sendMessage(byte[] dataBytes);
}
