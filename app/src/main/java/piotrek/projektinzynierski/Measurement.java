package piotrek.projektinzynierski;
        import android.Manifest;
        import android.content.Context;
        import android.content.pm.PackageManager;
        import android.graphics.Bitmap;
        import android.graphics.Color;
        import android.graphics.Point;
        import android.graphics.SurfaceTexture;
        import android.hardware.camera2.CameraAccessException;
        import android.hardware.camera2.CameraCaptureSession;
        import android.hardware.camera2.CameraCharacteristics;
        import android.hardware.camera2.CameraDevice;
        import android.hardware.camera2.CameraManager;
        import android.hardware.camera2.CameraMetadata;
        import android.hardware.camera2.CaptureRequest;
        import android.hardware.camera2.TotalCaptureResult;
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
        import android.util.Log;
        import android.util.Size;
        import android.util.SparseIntArray;
        import android.view.Surface;
        import android.view.TextureView;
        import android.view.View;
        import android.widget.Button;
        import android.widget.Chronometer;
        import android.widget.ImageButton;
        import android.widget.ImageView;
        import android.widget.SeekBar;
        import android.widget.TextView;
        import android.widget.Toast;

        import org.opencv.android.Utils;
        import org.opencv.core.Mat;
        import org.opencv.core.MatOfPoint;
        import org.opencv.core.Rect;
        import org.opencv.core.Scalar;
        import org.opencv.imgproc.Imgproc;

        import java.io.File;
        import java.io.FileOutputStream;
        import java.io.IOException;
        import java.io.OutputStream;
        import java.nio.IntBuffer;
        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Collections;
        import java.util.Comparator;
        import java.util.Date;
        import java.util.List;
        import java.util.Vector;

        import static java.lang.Math.abs;
        import static java.lang.Math.pow;

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
            Bitmap bmp = textureView.getBitmap();
            findPupil(bmp);
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
    private SeekBar secondsBar;
    private TextView secondsLabel;
    private boolean isMeasurementOn = false;
    private boolean isFlashOn = false;
    private Chronometer chronometer;
    private File videoFolder;
    private String videoFileName;

    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            // to tylko raz się ma zrobić
            if(isMeasurementOn){
                /*try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
               /* startRecord();
                mediaRecorder.start();*/
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
       /* createVideoFolder();*/
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        mediaRecorder = new MediaRecorder();
        secondsLabel = (TextView) findViewById(R.id.seconds);
        textureView = (TextureView) findViewById(R.id.texture);
        flashButton = (Button) findViewById(R.id.button_flash);

        secondsBar = (SeekBar) findViewById(R.id.seekBar);
        secondsBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double numberOfSeconds = (double) secondsBar.getProgress()/10;
                String text = numberOfSeconds + "s";
                secondsLabel.setText(text);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
       /* recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMeasurementOn) {
                    *//*mediaRecorder.stop();
                    isMeasurementOn = false;
                    recordImageButton.setImageResource(R.mipmap.btn_video_online);
                    mediaRecorder.stop();
                    mediaRecorder.reset();*//*

                    chronometer.stop();
                    finish();
                    // connectCamera();
                    //startPreview();
                } else {
                   *//* checkWriteStoragePermission();*//*
                }
            }
        });*/
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
                /*try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
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

    private void findPupil(Bitmap bitmap){
        //Uwaga wrzuciłem MatOfPoint bo tego wymaga funkcja, a czy tak wyjdzie to zobaczymy.
        Vector<MatOfPoint> contours = new Vector<>();
        Vector<MatOfPoint> contours2 = new Vector<>();
        Vector<MatOfPoint> contours3 = new Vector<>();
        Vector<MatOfPoint> contours4 = new Vector<>();

        Mat mat = new Mat();
        //Mat matThresholded = new Mat();
        Mat colorMat;
        Utils.bitmapToMat(bitmap, mat);
        colorMat = mat.clone();
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        Scalar color = new Scalar(255, 255, 255);
        //Imgproc.Canny(mat, matThresholded, 30, 90);
        //Imgproc.adaptiveThreshold(mat, matThresholded, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 7, 1);
        //The function FindContours() computes contours from binary images.
        // To new Mat() nic nie robi, jak to wywalic? Niby jestoptional
        Imgproc.findContours(mat, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        //co zmieni to że wyrysuje te kontury w tym thresholded. Trzeba zoabczyc
        Imgproc.drawContours(mat, contours, -1, color, -1);
        //Imgproc.Canny(matThresholded, matThresholded, 30, 255);
        //Imgproc.drawContours(colorMat, contours, -1, new Scalar(255,255,255), -1);
        for (int i = 0; i < contours.size(); i++){
            double area = Imgproc.contourArea(contours.get(i));
            Rect rect = Imgproc.boundingRect(contours.get(i));
            int radius = rect.width/2;
            double whValue = abs(1-(rect.width / rect.height));
            double areaValue = abs(1 - (area / Math.PI * pow(radius, 2)));
            if(area > 100){//30 && whValue <= 0.4 && areaValue <=0.4) {
                Imgproc.circle(colorMat, new org.opencv.core.Point(rect.x + radius, rect.y + radius), radius, new Scalar(255,0,0), 2);
            }
        }

        Utils.matToBitmap(colorMat, bitmap);
        //Utils.matToBitmap(matThresholded,bitmap);
        ImageView myImage = (ImageView) findViewById(R.id.imageView2);
        myImage.setImageBitmap(bitmap);
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
        try {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, cameraDeviceStateCallback, backgroundHandler);
            } else {
                Toast.makeText(this, "Application required access to camera", Toast.LENGTH_SHORT).show();
            }
        } catch (CameraAccessException e) {
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
                                        //listener
                                        new CameraCaptureSession.CaptureCallback() {

                                            @Override
                                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                                                           TotalCaptureResult result) {
//tu gdzieś się pojawia ten preview(za 2 razem)

                                          }
                                        } , backgroundHandler);
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


}