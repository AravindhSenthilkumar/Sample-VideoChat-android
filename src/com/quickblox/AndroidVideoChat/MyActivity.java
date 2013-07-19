package com.quickblox.AndroidVideoChat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import android.widget.ImageView;
import com.quickblox.AndroidVideoChat.camera.CameraSurfaceView;
import com.quickblox.AndroidVideoChat.websockets.BinaryChatService;
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
        Intent intent = new Intent(this, BinaryChatService.class);
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);


        pictureFromCameraImageView = (ImageView) findViewById(R.id.pictureFromCameraImageView);
        cameraPreview = (CameraSurfaceView) findViewById(R.id.camera_preview);
        cameraPreview.setPictureFromCameraImageView(pictureFromCameraImageView);

        addContentView(cameraPreview.getDrawOnTop(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        startService(new Intent(ChatService.STOP_CHAT));
    }





    private boolean isBound;
    private BinaryChatService chatService;


    private ServiceConnection myConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BinaryChatService.MyLocalBinder binder = (BinaryChatService.MyLocalBinder) service;
            chatService = binder.getService();
            isBound = true;

            chatService.setOnMessageReceive(new BinaryChatService.OnBinaryMessageReceive() {
                @Override
                public void onMessage(final byte[] data) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(-90);
                    final Bitmap origin = BitmapFactory.decodeByteArray(data, 0, data.length);
                    final Bitmap rotatedBitmap = Bitmap.createBitmap(origin, 0, 0,
                            origin.getWidth(), origin.getHeight(), matrix, true);
                    origin.recycle();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cameraPreview.applyPicture(rotatedBitmap);
                        }
                    });

                }

                @Override
                public void onServerConnected() {
                    cameraPreview.setOnFrameChangeListener(new CameraSurfaceView.OnFrameChangeListener() {
                        @Override
                        public void onFrameChange(byte[] mas) {
                            chatService.sendMessage(mas);
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
        return super.onOptionsItemSelected(item);
    }


}
