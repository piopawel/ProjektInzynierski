package piotrek.projektinzynierski;
        import android.Manifest;
        import android.content.Context;
        import android.content.pm.PackageManager;
        import android.graphics.SurfaceTexture;
        import android.hardware.camera2.CameraAccessException;
        import android.hardware.camera2.CameraCaptureSession;
        import android.hardware.camera2.CameraCharacteristics;
        import android.hardware.camera2.CameraDevice;
        import android.hardware.camera2.CameraManager;
        import android.hardware.camera2.CameraMetadata;
        import android.hardware.camera2.CaptureRequest;
        import android.hardware.camera2.params.StreamConfigurationMap;
        import android.media.MediaRecorder;
        import android.os.Build;
        import android.os.Environment;
        import android.os.Handler;
        import android.os.HandlerThread;
        import android.os.SystemClock;
        import android.support.v4.content.ContextCompat;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Size;
        import android.util.SparseIntArray;
        import android.view.Surface;
        import android.view.TextureView;
        import android.view.View;
        import android.widget.Button;
        import android.widget.Chronometer;
        import android.widget.ImageButton;
        import android.widget.Toast;

        import java.io.File;
        import java.io.IOException;
        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Collections;
        import java.util.Comparator;
        import java.util.Date;
        import java.util.List;

public class Measurement extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;
    private TextureView textureView;
    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    private CameraDevice cameraDevice;

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;
    private String cameraId;
    private Size previewSize;
    private Size videoSize;
    private MediaRecorder mediaRecorder;
    private int totalRotation;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession captureSession;
    private Button recordButton;
    private Button flashButton;
    private boolean isMeasurementOn = false;
    private boolean isFlashOn = false;
    private Chronometer chronometer;
    private File videoFolder;
    private String videoFileName;
/*    boolean hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);*/
    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            // to tylko raz się ma zrobić
            if(isMeasurementOn){
                try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                mediaRecorder.start();
            } else {
                startPreview();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            //setupCamera(textureView.getWidth(), textureView.getHeight());
            //connectCamera();
        }
    };

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() /
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);
        createVideoFolder();
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        mediaRecorder = new MediaRecorder();

        textureView = (TextureView) findViewById(R.id.texture);
        recordButton = (Button) findViewById(R.id.button_start_measurement);
        flashButton = (Button) findViewById(R.id.button_flash);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMeasurementOn) {
                    /*mediaRecorder.stop();
                    isMeasurementOn = false;
                    recordImageButton.setImageResource(R.mipmap.btn_video_online);
                    mediaRecorder.stop();
                    mediaRecorder.reset();*/

                    chronometer.stop();
                    finish();
                    // connectCamera();
                    //startPreview();
                } else {
                    checkWriteStoragePermission();
                }
            }
        });
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if (isFlashOn) {
                        turnOffFlashLight();
                        isFlashOn = false;
                    } else {
                        turnOnFlashLight();
                        isFlashOn = true;
                    }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();

        if(textureView.isAvailable()) {
            setupCamera(textureView.getWidth(), textureView.getHeight());
            connectCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Application will not run without camera services", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isMeasurementOn = true;
                /*recordImageButton.setImageResource(R.mipmap.btn_video_busy);*/
                try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(this,
                        "Permission successfully granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "App needs to save video to run", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocas) {
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if(hasFocas) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraCounter : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraCounter);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if(swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                videoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);
                cameraId = cameraCounter;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {/*
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(cameraId, cameraDeviceStateCallback, backgroundHandler);
                } else {
                    if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(this,
                                "Video app required access to camera", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }

            } else {*/
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(cameraId, cameraDeviceStateCallback, backgroundHandler);
                } else {
                        Toast.makeText(this,
                                "Video app required access to camera", Toast.LENGTH_SHORT).show();

                }

            /*}*/
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    //zrobic start measurement z wszystkich kawałków
    private void startRecord() {
        try {
            setupMediaRecorder();
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface = mediaRecorder.getSurface();
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(recordSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface,recordSurface),
                    new CameraCaptureSession.StateCallback(){
                        @Override
                        public void onConfigured(CameraCaptureSession session){
                            captureSession = session;
                            try {
                                captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession session){

                        }
                    }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void turnOnFlashLight()
    {
        try
        {
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void turnOffFlashLight()
    {
        try
        {
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(cameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(captureRequestBuilder.build(),
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Toast.makeText(getApplicationContext(),
                                    "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if(cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        backgroundHandlerThread = new HandlerThread("Camera2VideoImage");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundHandlerThread.quitSafely();
        try {
            backgroundHandlerThread.join();
            backgroundHandlerThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrienatation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrienatation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for(Size option : choices) {
            if(option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    private void createVideoFolder() {
        File movieFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        videoFolder = new File(movieFile, "ProjektInzynierski");
        if(!videoFolder.exists()) {
            videoFolder.mkdirs();
        }
    }

    private File createVideoFileName() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "Badanie" + timestamp + "_";
        File videoFile = File.createTempFile(prepend, ".mp4", videoFolder);
        videoFileName = videoFile.getAbsolutePath();
        return videoFile;
    }

    private void checkWriteStoragePermission() {/*
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                isMeasurementOn = true;
                recordImageButton.setImageResource(R.mipmap.btn_video_busy);
                try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                mediaRecorder.start();
            } else {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "app needs to be able to save videos", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
            }
        } else {*/
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            isMeasurementOn = true;
            /*recordImageButton.setImageResource(R.mipmap.btn_video_busy);*/
            try {
                createVideoFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }

            startRecord();
            mediaRecorder.start();
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.setVisibility(View.VISIBLE);
            chronometer.start();
        } else {
                Toast.makeText(this, "app needs to be able to save videos", Toast.LENGTH_SHORT).show();
        }

        /*}*/
    }

    private void setupMediaRecorder() throws IOException {
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(videoFileName);
        mediaRecorder.setVideoEncodingBitRate(1000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setOrientationHint(totalRotation);
        mediaRecorder.prepare();
    }

/*    private void checkFlash(){
        if (!hasFlash) {
            Toast.makeText(Measurement.this, "The device must support flash!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }*/


}
/*

public class Measurement extends AppCompatActivity {

    */
/**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     *//*

    private static final boolean AUTO_HIDE = true;

    */
/**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     *//*

    private static final int AUTO_HIDE_DELAY_MILLIS = 3;

    */
/**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     *//*

    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        createVideosFolder();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement);
        textureView = (TextureView) findViewById(R.id.texture);
        mediaRecorder = new MediaRecorder();
        */
/*assert textureView != null;//te asserty raczej niepotrzebne*//*

        textureView.setSurfaceTextureListener(textureListener);
        startMeasurementButton = (Button) findViewById(R.id.button_start_measurement);
                startMeasurementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               */
/* if(isMeasurementOn) {
                    isMeasurementOn = false;
                    startMeasurementButton.setImageResource(R.mipmap.btn_video_online);
                    mediaRecorder.stop();
                    mediaRecorder.reset();
                    createCameraPreview();//trzeba zobaczyć co on ma w tym rpeview
                }else{
                    isMeasurementOn = true;
                    startMeasurementButton.setImageResource(R.mipmap.btn_video_busy);
                }*//*

                mediaRecorder.stop();
                mediaRecorder.reset();
                startMeasurement();
            }
        });

        */
/*assert startMeasurementButton != null;//te asserty raczej niepotrzebne*//*

        startMeasurementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMeasurement();
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        startBackgroundThread();
        if(textureView.isAvailable()){
            openCamera();
            */
/*setupCamera(textureView.getWidth(), textureView.getHeight());*//*

        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        if(cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
        stopBackgroundThread();
        super.onPause();
    }


    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    */
/**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
*/
/*     *//*
*/
/*
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }*//*


    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Toast.makeText(getApplicationContext(), "TextureView is available", Toast.LENGTH_SHORT).show();
            openCamera();*/
/*Camera(width, height);*//*

            //openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrienatation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrienatation + deviceOrientation + 360) % 360;
    }

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }
    //chyba nie potrzebuje tego rotation
    // open camera nie rozni sie prawie niczym poza facing
    */
/*int rotatedWidth;
    int rotatedHeight;
    private void setupCamera(int width, int height){
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        try{
            for (String cameraCounter : manager.getCameraIdList()){
                characteristics = manager.getCameraCharacteristics(cameraId);
                if(characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK)
                    cameraId = cameraCounter;
            }
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            int deviceOrientation =  getWindowManager().getDefaultDisplay().getRotation();
            int totalRotation = sensorToDeviceRotation(characteristics, deviceOrientation);
            boolean swapRotation = totalRotation==90 || totalRotation == 270;
            if (swapRotation) {
                rotatedWidth = height;
                rotatedHeight = width;
            }
            //czy 0?
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[Integer.getInteger(cameraId)];
            return;
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }*//*


    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        */
/*Log.e(TAG, "is camera open");*//*

        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            // uzyskuje info o tym jakie mają być kolory itp
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
            totalRotation = sensorToDeviceRotation(characteristics, deviceOrientation);
            //assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            // Bez tego nie pozwoli na otworzenie kamery(to openCamera tu jest inną funkcją, jakąś domyyślną) musi być jawnie sprawdzone
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Measurement.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        */
/*Log.e(TAG, "openCamera X");*//*

    }


    // Funkcja obsłuhgująca preview
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            */
/*Log.e(TAG, "onOpened");*//*

            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
            cameraDevice = null;
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };


    */
/*
    When starting the preview, set up the MediaRecorder to accept video format.
    Then, set up a CaptureRequest.Builder using createCaptureRequest(CameraDevice.TEMPLATE_RECORD) on your CameraDevice instance.
    Then, implement a CameraCaptureSession.StateCallback,
     using the method createCaptureSession(surfaces, new CameraCaptureSession.StateCallback(){})
      on your CameraDevice instance, where surfaces is a list consisting of the surface view of your TextureView
       and the surface of your MediaRecorder instance.
     Use start() and stop() methods on your MediaRecorder instance to actually start and stop the recording.
    Lastly, set up and clean up your camera device in onResume() and onPause().*//*

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            //assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            //Set the default size of the image buffer - buffer to ogólne pojęcie dla przechowaywania(?)
            Surface surface = new Surface(texture);
            //capture request daje wszystkie ustawienia potrzebne do pochwycenia pojedynczego obrazu z kamery
            // może to się zamini na to Media Recorede
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

           */
/* The active capture session determines the set of potential output
            Surfaces for the camera device for each capture request. A given request may use all or only
            some of the outputs. Once the CameraCaptureSession is created, requests can be submitted with capture,
                    captureBurst, setRepeatingRequest, or setRepeatingBurst.

                    For drawing to a SurfaceView: Once the SurfaceView's Surface is created,
                    set the size of the Surface with setFixedSize(int, int) to be one of the sizes returned
                     by getOutputSizes(SurfaceHolder.class) and then obtain the Surface by calling getSurface().
                     If the size is not set by the application,
                    it will be rounded to the nearest supported size less than 1080p, by the camera device.


                    For accessing through an OpenGL texture via a SurfaceTexture:
                    Set the size of the SurfaceTexture with setDefaultBufferSize(int, int)
                    to be one of the sizes returned by getOutputSizes(SurfaceTexture.class)
                    before creating a Surface from the SurfaceTexture with Surface(SurfaceTexture).
                    If the size is not set by the application,
                    it will be set to be the smallest supported size less than 1080p, by the camera device.


                    For recording with MediaCodec: Call createInputSurface() after configuring the media codec
                    to use one of the sizes returned by getOutputSizes(MediaCodec.class)

For recording with MediaRecorder: Call getSurface() after configuring the media recorder to use one of the sizes returned by
getOutputSizes(MediaRecorder.class), or configuring it to use one of the supported CamcorderProfiles.*//*

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(Measurement.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if(null == cameraDevice) {
           */
/* Log.e(TAG, "updatePreview error, return");*//*

        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    protected void startMeasurement() {
        try{
            createFileName();
            setupMediaRecorder();
        }catch (IOException e){
            e.printStackTrace();
        }
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            //Tu jest mniejsze niż maksymalne
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface previewSurface = new Surface(texture);
            Surface videoSurface = mediaRecorder.getSurface();
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(videoSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, videoSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {

                        }
                    }, null);
        }catch(Exception e){
            e.printStackTrace();
        }
        if(null == cameraDevice) {
            */
/*Log.e(TAG, "cameraDevice is null");*//*

            return;
        }
    }







}
*/


/*
    z tego tutoriala zdjeciowego
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(AndroidCameraApi.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };
    */

/*
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }


    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
*
 * Touch listener to use for in-layout UI controls to delay hiding the
 * system UI. This is to prevent the jarring behavior of controls going away
 * while interacting with activity UI.
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };*/
