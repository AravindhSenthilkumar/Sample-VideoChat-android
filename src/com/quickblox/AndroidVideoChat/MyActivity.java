package com.quickblox.AndroidVideoChat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import com.quickblox.AndroidVideoChat.camera.CameraSurfaceView;
import com.quickblox.AndroidVideoChat.websockets.BinaryChatService;
import com.quickblox.AndroidVideoChat.websockets.ChatService;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class MyActivity extends FragmentActivity {


    private static final String TAG = MyActivity.class.getName();
    private CameraSurfaceView cameraPreview;
    private ImageView videoFrame1;
    private ImageView videoFrame2;
    private ImageView videoFrame3;
    private ImageView videoFrame4;

    private AudioTrack speaker;
    private AudioRecord recorder;

    public static byte PHONE_ID = 0;


    //Audio Configuration.
    private int sampleRate = 8000;
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private boolean status = true;

    AudioTrack audioTrack;
    private PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        onCreateDialogSingleChoice().show();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        videoFrame1 = (ImageView) findViewById(R.id.chatFrame1);
        videoFrame2 = (ImageView) findViewById(R.id.chatFrame2);
        videoFrame3 = (ImageView) findViewById(R.id.chatFrame3);
        videoFrame4 = (ImageView) findViewById(R.id.chatFrame4);
        cameraPreview = (CameraSurfaceView) findViewById(R.id.camera_preview);


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
            Log.d(TAG, "Service connected");
            BinaryChatService.MyLocalBinder binder = (BinaryChatService.MyLocalBinder) service;
            chatService = binder.getService();
            isBound = true;
            chatService.setOnMessageReceive(onMessageReceive);
            Intent start = new Intent(MyActivity.this, BinaryChatService.class);
            start.setAction(BinaryChatService.START_CHAT);
            startService(start);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            chatService.setOnMessageReceive(null);
        }

    };

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


    BinaryChatService.OnBinaryMessageReceive onMessageReceive = new BinaryChatService.OnBinaryMessageReceive() {
        @Override
        public void onMessage(final byte[] data) {
            final byte flag = data[data.length - 1];
            long timestamp = bytesToLong(Arrays.copyOfRange(data, data.length - 9, data.length - 1));

            if (System.currentTimeMillis() - timestamp > 500) {
                Log.w("BinaryChatService", "time is up so skip");
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] originalBytes = Arrays.copyOfRange(data, 0, data.length - 1);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    final Bitmap origin = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.length);
                    final Bitmap rotatedBitmap = Bitmap.createBitmap(origin, 0, 0,
                            origin.getWidth(), origin.getHeight(), matrix, true);
                    origin.recycle();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (flag) {
                                case 0:
                                    videoFrame1.setImageBitmap(rotatedBitmap);
                                    break;
                                case 1:
                                    videoFrame2.setImageBitmap(rotatedBitmap);
                                    break;
                                case 2:
                                    videoFrame3.setImageBitmap(rotatedBitmap);
                                    break;
                                case 3:
                                    videoFrame4.setImageBitmap(rotatedBitmap);
                                    break;
                            }
                        }
                    });
                }
            }).start();


        }

        @Override
        public void onServerConnected() {
            cameraPreview.setOnFrameChangeListener(new CameraSurfaceView.OnFrameChangeListener() {
                @Override
                public void onFrameChange(byte[] mas) {
                    byte[] dataBytes = Arrays.copyOf(mas, mas.length + 9);
                    dataBytes[dataBytes.length - 1] = PHONE_ID;
                    byte[] timestamp = longToBytes(System.currentTimeMillis());
                    for (int i = 0; i < 8; i++) {
                        dataBytes[dataBytes.length - 9 + i] = timestamp[i];
                    }
                    chatService.sendMessage(dataBytes);
                }
            });
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


    public Dialog onCreateDialogSingleChoice() {

        //Initialize the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Source of the data in the DIalog
        CharSequence[] array = {"1 frame", "2 frame", "3 frame", "4 frame"};

        // Set the dialog title
        builder.setTitle("Select frame")
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setSingleChoiceItems(array, 1, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PHONE_ID = (byte) which;
                    }
                })

                        // Set the action buttons
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(MyActivity.this, BinaryChatService.class);
                        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });

        return builder.create();
    }


}
