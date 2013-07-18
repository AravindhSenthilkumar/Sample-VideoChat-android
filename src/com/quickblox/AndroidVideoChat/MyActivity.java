package com.quickblox.AndroidVideoChat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.media.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import android.widget.ImageView;
import com.quickblox.AndroidVideoChat.camera.CameraSurfaceView;
import com.quickblox.AndroidVideoChat.websockets.ChatMessage;
import com.quickblox.AndroidVideoChat.websockets.ChatService;

public class MyActivity extends FragmentActivity {


    private CameraSurfaceView cameraPreview;
    private ImageView pictureFromCameraImageView;

    private AudioTrack speaker;
    private AudioRecord recorder;


    //Audio Configuration.
    private int sampleRate = 8000;
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private boolean status = true;

    AudioTrack audioTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
//      ServerConnect.runUdpClient();
        Intent intent = new Intent(this, ChatService.class);
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);


        pictureFromCameraImageView = (ImageView) findViewById(R.id.pictureFromCameraImageView);
        cameraPreview = (CameraSurfaceView) findViewById(R.id.camera_preview);
        cameraPreview.setPictureFromCameraImageView(pictureFromCameraImageView);

        addContentView(cameraPreview.getDrawOnTop(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//      startStreaming();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
//      recorder.release();
//      speaker.release();
    }


    public void startStreaming() {
        Thread streamThread = new Thread(new Runnable() {
            @Override
            public void run() {

                int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                byte[] buffer = new byte[minBufSize];
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize);
                recorder.startRecording();


                speaker = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, 2048, AudioTrack.MODE_STREAM);
                speaker.play();

                while (status == true) {
                    minBufSize = recorder.read(buffer, 0, buffer.length);
                    speaker.write(buffer, 0, minBufSize);
                }
            }
        });
        streamThread.start();
    }


    private boolean isBound;
    private ChatService chatService;
    private ServiceConnection myConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ChatService.MyLocalBinder binder = (ChatService.MyLocalBinder) service;
            chatService = binder.getService();
            isBound = true;

            chatService.setOnMessageReceive(new ChatService.OnMessageReceive() {
                @Override
                public void onMessage(final ChatMessage message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String type = message.getType();

                            if (type.equals(ChatService.CONNECTED)) {
                                cameraPreview.setOnFrameChangeListener(new CameraSurfaceView.OnFrameChangeListener() {
                                    @Override
                                    public void onFrameChange(byte[] mas) {
                                        // byte[] bytes = message.getBinaryBody();
                                        // Log.w("MyActivity", "onFrameChange");
                                        // pictureFromCameraImageView.setImageBitmap(BitmapFactory.decodeByteArray(mas, 0, mas.length));
                                        chatService.sendMessage(mas);
                                    }
                                });
                            } else if (type.equals(ChatService.USER_JOINED)) {

                            } else if (type.equals(ChatService.MESSAGE_SEND)) {
                                pictureFromCameraImageView.setImageBitmap(
                                        BitmapFactory.decodeByteArray(message.getBinaryBody(), 0,
                                                message.getBinaryBody().length));
                            } else if (type.equals(ChatService.MESSAGE_RECEIVED)) {
                                pictureFromCameraImageView.setImageBitmap(
                                        BitmapFactory.decodeByteArray(message.getBinaryBody(), 0,
                                                message.getBinaryBody().length));
                            } else if (type.equals(ChatService.USER_SPLIT)) {

                            }
                        }
                    });

                }
            });
            startService(new Intent(ChatService.START_CHAT));


        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            chatService.setOnMessageReceive(null);
        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.quit) {
            startService(new Intent(ChatService.STOP_CHAT));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);    //To change body of overridden methods use File | Settings | File Templates.
    }


}
