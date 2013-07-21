package com.choiboi.imagecroppingexample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
    public void onStartCropButton(View v) {
        Intent intent = new Intent(this, CropActivity.class);
        startActivity(intent);
    }
}
