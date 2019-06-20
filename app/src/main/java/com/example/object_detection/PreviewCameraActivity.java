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
import android.graphics.Rect;
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
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
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

    private RenderScript mRenderScript;
    private ScriptIntrinsicYuvToRGB mYuvToRGB;
    private Type.Builder mYuvType, mRgbaType;
    private Allocation mIn, mOut;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview_content);
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mSurfaceHolder = ((SurfaceView) findViewById(R.id.usb_preview_content)).getHolder();
        mSurfaceHolder.addCallback(mSurfaceHolderCallBack);
        mImageView = (ImageView) findViewById(R.id.image);
        mRenderScript = RenderScript.create(this);
        mYuvToRGB = ScriptIntrinsicYuvToRGB.create(mRenderScript, Element.U8_4(mRenderScript));
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
            mSurfaceHolder.setFixedSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
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
                    ImageFormat.YUV_420_888,
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

                    byte[] data = getDataFromImage(image,COLOR_FormatNV21);
                    if(mYuvType == null){
                        mYuvType = new Type.Builder(mRenderScript, Element.U8(mRenderScript))
                                .setX(data.length);
                        mIn = Allocation.createTyped(mRenderScript, mYuvType.create(), Allocation.USAGE_SCRIPT);

                        mRgbaType = new Type.Builder(mRenderScript, Element.RGBA_8888(mRenderScript))
                                .setX(mPreviewSize.getWidth()).setY(mPreviewSize.getHeight());
                        mOut = Allocation.createTyped(mRenderScript, mRgbaType.create(), Allocation.USAGE_SCRIPT);
                    }

                    mIn.copyFrom(data);
                    mYuvToRGB.setInput(mIn);
                    mYuvToRGB.forEach(mOut);

                    Bitmap bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
                    mOut.copyTo(bitmap);
                    Matrix matrix = new Matrix();
                    matrix.setScale(0.5f,0.5f);
                    matrix.postRotate(90);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                    YuvImage yuvImage = new YuvImage(getDataFromImage(image,COLOR_FormatNV21)
//                            ,ImageFormat.NV21, mPreviewSize.getWidth()
//                            , mPreviewSize.getHeight(),null);
//                    yuvImage.compressToJpeg(new Rect(0,0,width,height),50,outputStream);
//                    Log.i("picturesize","ss"+outputStream.toByteArray().length);
//                    Bitmap bitmap = BitmapFactory.decodeByteArray(outputStream.toByteArray(),
//                            0,outputStream.toByteArray().length);
//                    Matrix matrix = new Matrix();
//                    bitmap.recycle();
                    Bitmap newBitmap = Bitmap.createBitmap(bitmap
                            ,0
                            ,0
                            ,bitmap.getWidth()
                            ,bitmap.getHeight()
                            ,matrix
                            ,true);

//                    Bitmap newBitmap = Bitmap.createBitmap(bitmap,0,0
//                            ,mPreviewSize.getWidth()
//                            ,mPreviewSize.getHeight(),matrix,true);
//                    newBitmap.compress(Bitmap.CompressFormat.JPEG,50,outputStream);
                    newBitmap.compress(Bitmap.CompressFormat.JPEG,100, outputStream);
                    outputStream.toByteArray();
                    mImageView.setImageBitmap(newBitmap);
                    //newBitmap.recycle();
                    bitmap.recycle();
                    Log.i("widthheight",newBitmap.getHeight()+"anc"+newBitmap.getWidth());

                    //newBitmap.compress(ImageFormat.JPEG)
                    //matrix.postTranslate(0,(bitmap.getWidth()-bitmap.getHeight())/2);
//                    byte[] res = new byte[planes[0].getBuffer().remaining()];
//                    planes[0].getBuffer().get(res);
//
//                    Bitmap bitmap = BitmapFactory.decodeByteArray(res
//                            ,0,res.length);
//
//                    //Canvas canvas = new Canvas(newBitmap);
//                    Paint paint = new Paint();
//                    Matrix matrix = new Matrix();
//                    //matrix.setScale(1,-1);
//                    //matrix.postTranslate(bitmap.getWidth(),0);
//                    //matrix.postRotate(90,bitmap.getWidth()/2,bitmap.getHeight()/2);
//                    matrix.postRotate(90);
//                    Bitmap newBitmap = Bitmap.createBitmap(bitmap,0,0
//                            ,mPreviewSize.getWidth(),mPreviewSize.getHeight(),matrix,true);
//                    //matrix.postTranslate(0,(bitmap.getWidth()-bitmap.getHeight())/2);
//                    //canvas.drawBitmap(bitmap,matrix,paint);
//                    Log.i("widthheight",mPreviewSize.getHeight()+"anc"+mPreviewSize.getWidth());

//                    mImageView.setImageBitmap(newBitmap);
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    newBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
//
//                    //newBitmap.recycle();
//                    bitmap.recycle();
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




    private static final int COLOR_FormatI420 = 1;
    private static final int COLOR_FormatNV21 = 2;

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    private static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }
}
