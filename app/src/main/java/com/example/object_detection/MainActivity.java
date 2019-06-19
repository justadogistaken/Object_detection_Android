package com.example.object_detection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.start_detection);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = PreviewCameraActivity.startActivity(MainActivity.this);
                startActivity(intent);
            }
        });


        SurfaceView surfaceView2 = (SurfaceView) findViewById(R.id.mipi_preview_content);

            //mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
//            mSurfaceHolder.addCallback(mSurfaceHolderCallBack);
//            mSurfaceHolderUpper = surfaceView2.getHolder();
//            surfaceView2.setZOrderOnTop(true);
//            //surfaceView2.setZOrderMediaOverlay(true);
//            mSurfaceHolderUpper.setFormat(PixelFormat.TRANSLUCENT);
//            mSurfaceHolderUpper.addCallback(FaceHolderCallback);
    }

    private SurfaceHolder.Callback FaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };


    @Override
    protected void onResume() {
        super.onResume();
    }

    private class DownloadHandler extends Handler{

        public DownloadHandler(Looper looper) {
            super(looper);

        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //Canvas canvas = mSurfaceHolderUpper.lockCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            //canvas.drawLine(23,23,100,100,paint);
            //canvas.drawRect(20,20,89,89,paint);
            //canvas.drawLines(a,paint);
            //mSurfaceHolderUpper.unlockCanvasAndPost(canvas);
        }
    }






}
