package piotrek.projektinzynierski;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.content.Intent;
import org.opencv.android.OpenCVLoader;

/*
    Basic activity, which shows the help and leads to Measurement activity.
 */

public class MainMenu extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_menu);
        if(!OpenCVLoader.initDebug()){
            finish();
        }
    }

    public void goToMeasurement(View view){
        Intent intent = new Intent(this, Measurement.class);
        startActivity(intent);
    }
}
