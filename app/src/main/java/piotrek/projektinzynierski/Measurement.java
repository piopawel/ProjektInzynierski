package piotrek.projektinzynierski;
        import android.Manifest;
        import android.content.Context;
        import android.content.pm.PackageManager;
        import android.graphics.Bitmap;
        import android.graphics.SurfaceTexture;
        import android.hardware.camera2.CameraAccessException;
        import android.hardware.camera2.CameraCaptureSession;
        import android.hardware.camera2.CameraCharacteristics;
        import android.hardware.camera2.CameraDevice;
        import android.hardware.camera2.CameraManager;
        import android.hardware.camera2.CaptureRequest;
        import android.support.v4.content.ContextCompat;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.view.Surface;
        import android.view.TextureView;
        import android.view.View;
        import android.view.WindowManager;
        import android.widget.Button;
        import android.widget.ImageView;
        import android.widget.RadioButton;
        import android.widget.SeekBar;
        import android.widget.TextView;
        import android.widget.Toast;
        import java.util.Arrays;

/*
    Class that is responsible for setting the camera and preview up.
    It holds the UI required to use the application in full meaning:
        - The display of camera
        - Elements used to set parameters of flash
        - The bounding circles of the found pupil
 */
public class Measurement extends AppCompatActivity {

    private CameraDevice cameraDevice;
    private String cameraId;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession captureSession;
    private Button flashButton;
    private SeekBar secondsBar;
    private TextView secondsLabel;
    private TextureView textureView;
    private double numberOfSeconds;
    private ImageView drawnImage;
    private Pupil pupil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_measurement);
        secondsLabel = (TextView) findViewById(R.id.seconds);
        textureView = (TextureView) findViewById(R.id.texture);

        flashButton = (Button) findViewById(R.id.button_flash);
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioButton oneFlashRadio = (RadioButton) findViewById(R.id.oneFlash);
                RadioButton seriesFlashRadio = (RadioButton) findViewById(R.id.seriesFlash);
                if(oneFlashRadio.isChecked()){
                    FlashlightManager.oneFlash(captureRequestBuilder, captureSession, numberOfSeconds);
                } else if (seriesFlashRadio.isChecked()){
                    FlashlightManager.seriesFlash(captureRequestBuilder, captureSession);
                }
            }
        });
        secondsBar = (SeekBar) findViewById(R.id.seekBar);
        numberOfSeconds = (double) secondsBar.getProgress() / 10;
        secondsBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                numberOfSeconds = (double) secondsBar.getProgress() / 10;
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

        drawnImage = (ImageView) findViewById(R.id.imageView2);
        pupil = new Pupil();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(textureView.isAvailable()) {
            setupCamera();
            connectCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
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

    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            startPreview();
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
        }
    };

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera();
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
            drawnImage.setImageBitmap(pupil.drawPupil(bmp));
        }
    };

    private void setupCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraCounter : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraCounter);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
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
                cameraManager.openCamera(cameraId, cameraDeviceStateCallback, null);
            } else {
                Toast.makeText(this, "Application requires to access the camera", Toast.LENGTH_SHORT).show();
            }
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

    private void startPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(1280, 960);
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(cameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
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
}
