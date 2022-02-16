package com.uncanny.simpleapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.uncanny.simpleapplication.Utils.ImageSaverThread;
import com.uncanny.simpleapplication.Views.AutoFitPreviewView;

import java.util.Vector;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class CameraActivity extends AppCompatActivity {
    private final String TAG = "SIMPLE APPLICATION";
    private CameraManager camManager = null;
    private CameraDevice camDevice = null;
    private CameraCaptureSession camSession = null;
    private CameraCharacteristics characteristics = null;
    private CaptureRequest.Builder captureRequest = null;
    private ImageReader imgReader;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private final String CameraID = "1";
    private Handler mHandler = new Handler();
    private Vector<Surface> surfaceList = new Vector<>();
    private Size imageSize,previewSize;

    private AutoFitPreviewView tvPreview;
    private SurfaceTexture stPreview;
    private MaterialButton captureBtn;
    private boolean resumed = false, surface = false;

    private static final int REQUEST_PERMISSIONS = 200;
    private static final String[] PERMISSION_STRING = {Manifest.permission.WRITE_EXTERNAL_STORAGE
            ,Manifest.permission.READ_EXTERNAL_STORAGE
            , Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        tvPreview = findViewById(R.id.preview);
        tvPreview.setSurfaceTextureListener(surfaceTextureListener);
        captureBtn = findViewById(R.id.capture);
        captureBtn.setOnClickListener(view -> captureImage());
    }

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            Log.e(TAG, "onSurfaceTex");
            surface = true;
            stPreview = surfaceTexture;
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            surface = false;
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private void openCamera() {
        if (!resumed || !surface)
            return;

        Log.e(TAG, "openCamera: ");

        getCameraCharacteristics();
        imageSize = new Size(1080,1440);

        previewSize = imageSize;

        tvPreview.measure(previewSize.getWidth(), previewSize.getHeight());
        tvPreview.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());

        imgReader = ImageReader.newInstance(imageSize.getHeight(), imageSize.getWidth(), ImageFormat.JPEG, 5);
        imgReader.setOnImageAvailableListener(snapshotImageCallback, null);

        stPreview.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

        surfaceList.clear();
        surfaceList.add(new Surface(stPreview));

        surfaceList.add(imgReader.getSurface());
        Log.e(TAG, "openCamera: ImageReader preview size " + previewSize.getWidth() + "x" + previewSize.getHeight());
        Log.e(TAG, "openCamera: ImageReader capture size " + imageSize.getWidth() + "x" + imageSize.getHeight());

        try {
            if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestRuntimePermission();
                return;
            }
            camManager.openCamera(CameraID, imageCaptureCallback ,mHandler);
        } catch(Exception e) {
            Log.e(TAG, "openCamera: open failed: " + e.getMessage());
        }

    }

    private void captureImage() {
        try {
            captureRequest = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequest.set(CaptureRequest.JPEG_QUALITY,(byte) 100);
            captureRequest.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation()); //for front facing cam only
            Log.e(TAG, "captureImage: jpegrotation : "+getJpegOrientation());
            captureRequest.addTarget(surfaceList.get(1));
            camSession.capture(captureRequest.build(), snapshotCallback, mHandler);
        } catch(Exception e) {
            Log.e(TAG, "captureImage: "+e.toString());
        }
    }

    private void closeCamera() {
        if (null != camDevice) {
            camDevice.close();
            camDevice = null;
        }
        if (null != imgReader) {
            imgReader.close();
            imgReader = null;
        }

    }

    private CameraCharacteristics getCameraCharacteristics() {
        camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = camManager.getCameraCharacteristics("0");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return characteristics;
    }

    private int getJpegOrientation() {
        int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();

        CameraCharacteristics c = getCameraCharacteristics();
        int sensorOrientation =  c.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        return (sensorOrientation + deviceOrientation + 180) % 360;
    }

    private void requestRuntimePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(CameraActivity.this
                    , PERMISSION_STRING
                    , REQUEST_PERMISSIONS);
        }
    }

    /**
     * CALLBACKS
     */

    ImageReader.OnImageAvailableListener snapshotImageCallback = imageReader -> {
        Log.e(TAG, "onImageAvailable: received snapshot image data");
        Completable.fromRunnable(new ImageSaverThread(imageReader.acquireLatestImage(),
                CameraID,getContentResolver()))
                .subscribeOn(Schedulers.io())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        Intent i1 = new Intent(CameraActivity.this, EditActivity.class);
                        startActivity(i1);
                    }
                });
    };

    CameraDevice.StateCallback imageCaptureCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            camDevice = cameraDevice;
            try {
                camDevice.createCaptureSession(surfaceList,stateCallback, null);
                Log.e(TAG, "onOpened: tvPreview.SetAspectRatio : h : "+ previewSize.getHeight() + " w : "+ previewSize.getWidth());

            } catch (Exception e) {
                Log.e(TAG, "onOpened: session create failed: " + e.getMessage());
                camDevice = null;
                finish();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.e(TAG, "onDisconnected: camera disconnected");
            closeCamera();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.e(TAG, "onError: camera failed: " + i);
            camDevice = null;
            finish();
        }
    };

    CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            camSession = cameraCaptureSession;

            try {
                captureRequest = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequest.addTarget(surfaceList.get(0));

                camSession.setRepeatingRequest(captureRequest.build(), previewCallback, null);
            } catch (Exception e) {
                Log.e(TAG, "IMAGE CAPTURE CALLBACK: onConfigured: create preview failed: " + e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.e(TAG, "IMAGE CAPTURE CALLBACK: onConfigureFailed: configure failed");
        }
    };

    CameraCaptureSession.CaptureCallback previewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

        }
        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.e(TAG, "onCaptureFailed: lost preview");
        }
    };

    CameraCaptureSession.CaptureCallback snapshotCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);

        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.d(TAG, "onCaptureProgressed() returned:  capture progressed");
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.e(TAG, "onCaptureFailed: lost snapshot");
        }
    };

    /**
        LIFECYCLE METHODS
     **/

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        resumed = true;
        openCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        surface = false;
        resumed = false;
    }
}