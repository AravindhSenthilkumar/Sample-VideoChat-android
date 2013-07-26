package com.quickblox.AndroidVideoChat.websockets;

/**
 * Created with IntelliJ IDEA.
 * User: mardusdima
 * Date: 7/26/13
 * Time: 2:21 PM
 * To change this template use File | Settings | File Templates.
 */
public interface OnBinaryMessageReceive {
    void onMessage(byte[] message);

    void onServerConnected();
}