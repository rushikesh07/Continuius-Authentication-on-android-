package com.sng.bacgroundcameratutorial;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ActivitySplash extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        //ImageView img_splash=(ImageView)findViewById(R.id.img_splash);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i=new Intent(ActivitySplash.this,ActivityLauncher.class);
                startActivity(i);
                finish();
            }
        },3000);
    }
}
