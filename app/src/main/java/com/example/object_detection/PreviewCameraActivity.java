package com.example.object_detection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PreviewCameraActivity extends AppCompatActivity {
    //Camera config part;
    private final String cameraId = Integer.toString(CameraCharacteristics.LENS_FACING_FRONT);
    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    SurfaceHolder mSurfaceHolder, mSurfaceHolderUpper;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private Handler mCameraHandler;
    private HandlerThread mCameraThread;
    private ImageReader mPreviewImageReader;
    private byte[] mYuvBytes;
    private boolean mIsShutter;
    private ImageView mImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview_content);
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mSurfaceHolder = ((SurfaceView) findViewById(R.id.usb_preview_content)).getHolder();
        mSurfaceHolder.addCallback(mSurfaceHolderCallBack);
        mImageView = (ImageView) findViewById(R.id.image);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initHandlerThread();
    }









    public static Intent startActivity(Context context){
        return new Intent(context, PreviewCameraActivity.class);
    }

    private void initHandlerThread(){
        mCameraThread = new HandlerThread("CameraSurfaceViewThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i("camera","camera_opened");
            mCameraDevice = camera;
            startPreview();

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.i("camera","camera_closed");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.i("camera","camera_Error");
        }
    };

    private SurfaceHolder.Callback mSurfaceHolderCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            setupCamera(holder.getSurfaceFrame().width(),holder.getSurfaceFrame().height());
            //CameraManager mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                if (ActivityCompat.checkSelfPermission(PreviewCameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.

                    ActivityCompat.requestPermissions(PreviewCameraActivity.this, new String[]{Manifest.permission.CAMERA},1);
                    return;
                }
                mCameraManager.openCamera(cameraId, stateCallback, null);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    private void setupCamera(int width, int height){
        try {
            final StreamConfigurationMap map = mCameraManager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class),width,height);
            mPreviewImageReader = ImageReader.newInstance(
                    mPreviewSize.getWidth(),
                    mPreviewSize.getHeight(),
                    ImageFormat.JPEG,
                    1
            );
            mPreviewImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    if (image == null) {
                        return;
                    }

                    int width = image.getWidth();
                    int height = image.getHeight();

                    // YUV_420_888
                    Image.Plane[] planes = image.getPlanes();
                    byte[] res = new byte[planes[0].getBuffer().remaining()];
                    planes[0].getBuffer().get(res);

                    Bitmap bitmap = BitmapFactory.decodeByteArray(res
                            ,0,res.length);

                    //Canvas canvas = new Canvas(newBitmap);
                    Paint paint = new Paint();
                    Matrix matrix = new Matrix();
                    //matrix.setScale(1,-1);
                    //matrix.postTranslate(bitmap.getWidth(),0);
                    //matrix.postRotate(90,bitmap.getWidth()/2,bitmap.getHeight()/2);
                    matrix.postRotate(90);
                    Bitmap newBitmap = Bitmap.createBitmap(bitmap,0,0
                            ,mPreviewSize.getWidth(),mPreviewSize.getHeight(),matrix,true);
                    //matrix.postTranslate(0,(bitmap.getWidth()-bitmap.getHeight())/2);
                    //canvas.drawBitmap(bitmap,matrix,paint);
                    Log.i("widthheight",mPreviewSize.getHeight()+"anc"+mPreviewSize.getWidth());
                    Log.i("widthheight",newBitmap.getHeight()+"anc"+newBitmap.getWidth());
                    mImageView.setImageBitmap(newBitmap);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    newBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);

                    //newBitmap.recycle();
                    bitmap.recycle();
                    // 一定不能忘记close
                    image.close();
                }
            },null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    private void startPreview(){
        try {
            Surface surface = mSurfaceHolder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            if (surface!=null){
                Log.i("SURFACE","SURFACE不为空");
                mCaptureRequestBuilder.addTarget(surface);
                mCaptureRequestBuilder.addTarget(mPreviewImageReader.getSurface());
            }else {
                Log.i("SURFACE","SURFACE为空");
            }

            mCameraDevice.createCaptureSession(Arrays.asList(surface,mPreviewImageReader.getSurface())
                    , new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCaptureRequest = mCaptureRequestBuilder.build();
                    mCameraCaptureSession = session;
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest,
                                null,mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            },mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mCameraCaptureSession != null)
            mCameraCaptureSession = null;
        if(mCameraDevice != null)
            mCameraDevice = null;
    }
}
