package piotrek.projektinzynierski;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Vector;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

/**
 * Created by Piotrek on 2016-11-25.
 */

/**
 * The class represents a single object of a pupil. It is created once, at the beginning of the program.
 */

public class Pupil {

    private double diameter;
    private long startTime;
    private long time;
    private boolean isFlashOn;

    public Pupil(){
        this.startTime = System.currentTimeMillis();
    }

    /**
        Function used to
        - find the pupil in the picture,
        - set its properties necessary for the experiment
        - draw its boundrary on the screen
        Additionally it draws an ellipse to show where the eye of the subject should be located

        With OpenCV library, the algorithm creates a {@link Mat}out of a single bitmap,
        then creates another to draw on it, and a third one that is a cropped copy of the first one to enhance performance.
        It is processed with a gaussian blur and adaptive thresholding.
        Subsequently contours of the pupil are tried to be found. If so, the pupil parameters are set, and a circle is drawn.
     */
    protected Bitmap drawPupil(Bitmap bitmap){

        Vector<MatOfPoint> contours = new Vector<>();
        Mat mat = new Mat();
        Rect roiRect = new Rect(0, bitmap.getHeight()/3, bitmap.getWidth(), bitmap.getHeight()/3);
        Utils.bitmapToMat(bitmap, mat);
        Mat drawingMat = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC4);
        Mat thresholdedMat = new Mat(mat, roiRect);
        Imgproc.cvtColor(thresholdedMat, thresholdedMat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(thresholdedMat, thresholdedMat, new org.opencv.core.Size(7, 7), 1);
        Imgproc.adaptiveThreshold(thresholdedMat, thresholdedMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 13, 2);
        Imgproc.findContours(thresholdedMat.clone(), contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        Imgproc.drawContours(thresholdedMat, contours, -1, new Scalar(255, 255, 255), -1);
        for (int i = 0; i < contours.size(); i++){
            double area = Imgproc.contourArea(contours.get(i));
            Rect rect = Imgproc.boundingRect(contours.get(i));
            double radius;
            org.opencv.core.Point circleCenter;
            if (rect.height > rect.width) {
                radius = rect.height / 2;
                circleCenter = new org.opencv.core.Point(rect.x + rect.width / 2, rect.y + radius + drawingMat.height() / 3);
            }
            else {
                radius = rect.width / 2;
                circleCenter = new org.opencv.core.Point(rect.x + radius, rect.y + rect.height / 2 + drawingMat.height() / 3);
            }
            double widthToHeightValue = abs(1-((double)rect.width / (double)rect.height));
            double areaValue = abs(1 - (area / (Math.PI * pow(radius, 2))));

            if(area > 200 && widthToHeightValue <=0.4 && areaValue <= 0.4){
                diameter = radius *2;
                time = (System.currentTimeMillis() - startTime) / 10;
                isFlashOn = FlashlightManager.isFlashOn;
                Imgproc.circle(drawingMat, circleCenter, (int) radius, new Scalar(127, 255, 0), 1);
                //To draw the bounding circle of the pupil on thresholdedMat use the line below
                //Imgproc.circle(thresholdedMat, circleCenter, (int) radius, new Scalar(127,0,0), 2);
            }
        }
        org.opencv.core.Point ellipseCenter = new org.opencv.core.Point(drawingMat.width() / 2, drawingMat.height() / 2);
        Imgproc.ellipse(drawingMat, ellipseCenter, new org.opencv.core.Size(180, 76), 0, 0, 360, new Scalar(0,127,255), 3);
        Utils.matToBitmap(drawingMat, bitmap);
        return bitmap;

        //To show the thresholdedMat use this part instead of the above.
        /*org.opencv.core.Point ellipseCenter = new org.opencv.core.Point(thresholdedMat.width()/2, thresholdedMat.height()/2);
        Imgproc.ellipse(thresholdedMat, ellipseCenter, new org.opencv.core.Size(130, 56), 0, 0, 360, new Scalar(127,0,0), 3);
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight()/3, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(thresholdedMat, croppedBitmap);
        return croppedBitmap;*/
    }


}
