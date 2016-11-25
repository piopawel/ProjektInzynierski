package piotrek.projektinzynierski;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.CountDownTimer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Piotrek on 2016-11-25.
 */
/*
    This class provides the methods to control the flashlight in a way required for the project.
 */
public class FlashlightManager {

    protected static boolean isFlashOn = false;

    /*
        Turns the flash on and off a few times in desired time, as set in CountDownTimer and timer.schedule
     */
    protected static void seriesFlash(final CaptureRequest.Builder captureRequestBuilder, final CameraCaptureSession captureSession){
        new CountDownTimer(7500, 1500) {
            public void onTick(long millisUntilFinished) {
                TimerTask flashlightOff = new TimerTask() {
                    @Override
                    public void run() {
                        turnFlashLightOff(captureRequestBuilder, captureSession);
                        isFlashOn = false;
                    }
                };
                Timer timer = new Timer();
                turnFlashLightOn(captureRequestBuilder, captureSession);
                isFlashOn = true;
                timer.schedule(flashlightOff, 500);
            }
            public void onFinish() {
            }
        }.start();
    }

    /*
        Turns the flash on for the time specified by the variable numberOfSeconds.
        The numberOfSeconds is set for default of 2 seconds.
        The value of numberOfSeconds can be changed by scrolling the SeekBar secondsBar in the Measurement.
     */
    protected static void oneFlash(final CaptureRequest.Builder captureRequestBuilder,final CameraCaptureSession captureSession, double numberOfSeconds){
        TimerTask flashlightOff = new TimerTask() {
            @Override
            public void run() {
                turnFlashLightOff(captureRequestBuilder, captureSession);
                isFlashOn = false;
            }
        };
        Timer timer = new Timer();
        turnFlashLightOn(captureRequestBuilder, captureSession);
        isFlashOn = true;
        timer.schedule(flashlightOff, (long) (numberOfSeconds * 1000));
    }

    private static void turnFlashLightOn(CaptureRequest.Builder captureRequestBuilder, CameraCaptureSession captureSession) {
        try {
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void turnFlashLightOff(CaptureRequest.Builder captureRequestBuilder, CameraCaptureSession captureSession) {
        try {
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
