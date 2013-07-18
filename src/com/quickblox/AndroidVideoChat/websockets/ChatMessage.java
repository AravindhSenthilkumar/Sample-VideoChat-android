package com.quickblox.AndroidVideoChat.websockets;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: mardusdima
 * Date: 7/5/13
 * Time: 1:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatMessage implements Serializable {


    private String type;
    private String time;
    private String from;
    //private String text;
    private byte[] binaryBody;

//    public String getText() {
//        return text;
//    }
//
//    public void setText(String text) {
//        this.text = text;
//    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public byte[] getBinaryBody() {
        return binaryBody;
    }

    public void setBinaryBody(byte[] binaryBody) {
        this.binaryBody = binaryBody;
    }
}
