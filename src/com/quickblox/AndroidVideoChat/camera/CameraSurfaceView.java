package com.quickblox.AndroidVideoChat.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: grenka
 * Date: 01.11.12
 * Time: 20:49
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {


    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private Draw drawOnTop;
    private boolean isFinished;
    private Context ctx;
    private ImageView pictureFromCameraImageView;

    public CameraSurfaceView(Context ctx, AttributeSet attrSet) {
        super(ctx, attrSet);
        this.ctx = ctx;
        isFinished = false;
        drawOnTop = new Draw(ctx);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public Draw getDrawOnTop() {
        return drawOnTop;
    }

    public void setPictureFromCameraImageView(ImageView pictureFromCameraImageView) {
        this.pictureFromCameraImageView = pictureFromCameraImageView;
    }

    public interface OnFrameChangeListener {
        public void onFrameChange(byte[] mas);
    }

    public OnFrameChangeListener onFrameChangeListener;

    public OnFrameChangeListener getOnFrameChangeListener() {
        return onFrameChangeListener;
    }

    public void setOnFrameChangeListener(OnFrameChangeListener onFrameChangeListener) {
        this.onFrameChangeListener = onFrameChangeListener;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
        try {
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(90);

            // CameraSurfaceView callback used whenever new viewfinder frame is available
            camera.setPreviewCallback(new Camera.PreviewCallback() {
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if ((drawOnTop == null) || isFinished)
                        return;

                    if (drawOnTop.bitmap == null) {
                        // Initialize the draw-on-top companion
                        Camera.Parameters params = camera.getParameters();
                        drawOnTop.imageWidth = params.getPreviewSize().width;

                        drawOnTop.imageHeight = params.getPreviewSize().height;
                        drawOnTop.bitmap = Bitmap.createBitmap(drawOnTop.imageWidth,
                                drawOnTop.imageHeight, Bitmap.Config.RGB_565);
                        drawOnTop.rgbData = new int[drawOnTop.imageWidth * drawOnTop.imageHeight];
                        drawOnTop.yuvData = new byte[data.length];

                    }
                    // Pass YUV data to draw-on-top companion
                    System.arraycopy(data, 0, drawOnTop.yuvData, 0, data.length);
                    drawOnTop.invalidate();
                    if (onFrameChangeListener != null)
                        onFrameChangeListener.onFrameChange(encodeToBytes(drawOnTop.bitmap));
                }
            });

        } catch (IOException exception) {
            camera.release();
            camera = null;
        }
    }


    private byte[] encodeToBytes(Bitmap bmp) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    public void applyPicture(byte[] data) {

        int width = drawOnTop.imageWidth;
        int height = drawOnTop.imageHeight;
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(drawOnTop.bitmap, 0, 0,
                width, height, matrix, true);
        pictureFromCameraImageView.setImageBitmap(rotatedBitmap);
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isFinished = true;
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(320, 240);
        parameters.setPreviewFrameRate(15);
        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_NIGHT);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        camera.setParameters(parameters);
        camera.startPreview();


    }

    public class Draw extends View {

        Bitmap bitmap;
        byte[] yuvData;
        int[] rgbData;
        int imageWidth, imageHeight;

        public Draw(Context context) {
            super(context);
            bitmap = null;
            yuvData = null;
            rgbData = null;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (bitmap != null) {
                decodeYUV420RGB(rgbData, yuvData, imageWidth, imageHeight);
                bitmap.setPixels(rgbData, 0, imageWidth, 0, 0, imageWidth, imageHeight);
            }
        }

        private void decodeYUV420RGB(int[] rgb, byte[] yuv420sp, int width, int height) {
            // Convert YUV to RGB
            final int frameSize = width * height;
            for (int j = 0, yp = 0; j < height; j++) {
                int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
                for (int i = 0; i < width; i++, yp++) {
                    int y = (0xff & ((int) yuv420sp[yp])) - 16;
                    if (y < 0) y = 0;
                    if ((i & 1) == 0) {
                        v = (0xff & yuv420sp[uvp++]) - 128;
                        u = (0xff & yuv420sp[uvp++]) - 128;
                    }

                    int y1192 = 1192 * y;
                    int r = (y1192 + 1634 * v);
                    int g = (y1192 - 833 * v - 400 * u);
                    int b = (y1192 + 2066 * u);

                    if (r < 0) r = 0;
                    else if (r > 262143) r = 262143;
                    if (g < 0) g = 0;
                    else if (g > 262143) g = 262143;
                    if (b < 0) b = 0;
                    else if (b > 262143) b = 262143;

                    rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
                }
            }
        }
    }

}
