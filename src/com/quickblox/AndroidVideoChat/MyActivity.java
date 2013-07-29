package com.quickblox.AndroidVideoChat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.quickblox.AndroidVideoChat.camera.CameraSurfaceView;
import com.quickblox.AndroidVideoChat.websockets.BinaryChatServerService;
import com.quickblox.AndroidVideoChat.websockets.BinaryChatService;
import com.quickblox.AndroidVideoChat.websockets.ChatService;
import com.quickblox.AndroidVideoChat.websockets.OnBinaryMessageReceive;
import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MyActivity extends FragmentActivity {


    private static final String TAG = MyActivity.class.getName();
    private CameraSurfaceView cameraPreview;
    private ImageView videoFrame;
    private TextView addr;


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


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        videoFrame = (ImageView) findViewById(R.id.chatFrame);
        addr = (TextView) findViewById(R.id.serverAdr);

        cameraPreview = (CameraSurfaceView) findViewById(R.id.camera_preview);


        addContentView(cameraPreview.getDrawOnTop(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        onCreateDialogSingleChoice().show();

    }


    @Override
    public void onStop() {
        super.onStop();
    }


    private boolean isBound;
    private ChatService chatService;


    private ServiceConnection myClientConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "СlientService connected");
            BinaryChatService.MyLocalBinder binder = (BinaryChatService.MyLocalBinder) service;
            chatService = binder.getService();
            isBound = true;
            chatService.setOnMessageReceive(onMessageReceive);
            Intent start = new Intent(MyActivity.this, BinaryChatService.class);
            start.setAction(ChatService.START_CHAT);
            startService(start);
            addr.setText(getIPAddress(true) + "");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            chatService.setOnMessageReceive(null);
            startService(new Intent(BinaryChatService.STOP_CHAT));
        }

    };

    private ServiceConnection myServerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "ServerService connected");
            BinaryChatServerService.MyLocalBinder binder = (BinaryChatServerService.MyLocalBinder) service;
            chatService = binder.getService();
            isBound = true;
            chatService.setOnMessageReceive(onMessageReceive);
            Intent start = new Intent(MyActivity.this, BinaryChatServerService.class);
            start.setAction(BinaryChatServerService.START_SERVER);
            startService(start);
            addr.setText(getIPAddress(true));
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            chatService.setOnMessageReceive(null);
            startService(new Intent(BinaryChatService.STOP_SERVER));
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


    OnBinaryMessageReceive onMessageReceive = new OnBinaryMessageReceive() {
        @Override
        public void onMessage(final byte[] data) {
            final byte flag = data[data.length - 1];
            long timestamp = bytesToLong(Arrays.copyOfRange(data, data.length - 9, data.length - 1));

            if (System.currentTimeMillis() - timestamp > 1000) {
                Log.w("BinaryChatService", "time is up so skip diff="+(System.currentTimeMillis() - timestamp));
                return;
            }


//            byte[] originalBytes = Arrays.copyOfRange(data, 0, data.length - 1);
//            Matrix matrix = new Matrix();
//            matrix.postRotate(90);
//            final Bitmap origin = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.length);
//            if (origin == null) return;
//            final Bitmap rotatedBitmap = Bitmap.createBitmap(origin, 0, 0,
//                    origin.getWidth(), origin.getHeight(), matrix, true);
//            origin.recycle();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   // videoFrame.setImageBitmap(rotatedBitmap);
                }
            });


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
//        if (item.getItemId() == R.id.quit) {
//            startService(new Intent(BinaryChatServerService.STOP_CHAT));
//            finish();
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }


    public Dialog onCreateDialogSingleChoice() {

        //Initialize the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Source of the data in the DIalog

        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setText("192.168.0.");
        // Set the dialog title
        builder.setTitle("Select mode")
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected

                // Set the action buttons
                // Set up the input

                .setView(input)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(MyActivity.this, BinaryChatService.class);
                        intent.putExtra("server", input.getText().toString());
                        bindService(intent, myClientConnection, Context.BIND_AUTO_CREATE);

                    }
                })
                .setNegativeButton("Create server", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(MyActivity.this, BinaryChatServerService.class);
                        bindService(intent, myServerConnection, Context.BIND_AUTO_CREATE);
                    }
                });

        return builder.create();
    }


    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim < 0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "error", ex);
        } // for now eat exceptions
        return "";
    }
}



