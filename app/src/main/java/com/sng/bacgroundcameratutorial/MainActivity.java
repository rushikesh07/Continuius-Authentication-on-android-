package com.sng.bacgroundcameratutorial;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import services.CameraService;

public class MainActivity extends AppCompatActivity {
    int Overlay_Request_code=101;
    int REQUEST_PERMISSION=102;
    Button btn_start,btn_stop;
    Intent cameraService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        btn_start=(Button)findViewById(R.id.btn_start);
        btn_stop=(Button)findViewById(R.id.btn_stop);
        cameraService = new Intent(this
                .getApplicationContext(), CameraService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.v("App", "Build Version Greater than or equal to M: " + Build.VERSION_CODES.M);
            checkDrawOverlayPermission();
        } else {
            Log.v("App", "OS Version Less than M");
            //No need for Permission as less then M OS.
        }
        if ((CheckPermission(this, Manifest.permission.CAMERA)) &&
                (CheckPermission(this, Manifest.permission.READ_PHONE_STATE)) &&
                (CheckPermission(this, Manifest.permission.NFC))) {
            PermHandling();
        } else {
            RequestPermission(MainActivity.this, Manifest.permission.CAMERA, REQUEST_PERMISSION);
            RequestPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE, REQUEST_PERMISSION);
            RequestPermission(MainActivity.this, Manifest.permission.NFC, REQUEST_PERMISSION);

            //NewPermHandling();
        }

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getApplicationContext().startForegroundService(cameraService);
                } else {
                    getApplicationContext().startService(cameraService);
                }
            }
        });
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //getApplicationContext().stopService(cameraService);
                finish();
            }
        });
    }
    private void PermHandling() {
        //My app internal parts....
        //Here my stuff works...

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkDrawOverlayPermission() {
        Log.v("App", "Package Name: " + getApplicationContext().getPackageName());

        // Check if we already  have permission to draw over other apps
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            Log.v("App", "Requesting Permission" + Settings.canDrawOverlays(getApplicationContext()));
            // if not construct intent to request permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getApplicationContext().getPackageName()));
            // request permission via start activity for result
            startActivityForResult(intent, Overlay_Request_code); //It will call onActivityResult Function After you press Yes/No and go Back after giving permission
        } else {
            Log.v("App", "We already have permission for it.");
            // disablePullNotificationTouch();
            // Do your stuff, we got permission captain
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v("App", "OnActivity Result.");
        //check if received result code
        //  is equal our requested code for draw permission
        if (requestCode == Overlay_Request_code) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // Permission Granted by Overlay
                    // Do your Stuff
                }
            }
        }
    }
    public boolean CheckPermission(Context context, String Permission) {
        if (ContextCompat.checkSelfPermission(context,
                Permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    @Override
    public void onRequestPermissionsResult(int permissionRequestCode, String[] permissions, int[] grantResults) {
        if (permissionRequestCode != REQUEST_PERMISSION) {
            return;
        }

        if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            PermHandling();
        } else {
            // Ask the user to grant the permission
        }
    }
    public void RequestPermission(Activity thisActivity, String Permission, int Code) {
        if (ContextCompat.checkSelfPermission(thisActivity,
                Permission) !=
                PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                    Permission)) {} else {
                ActivityCompat.requestPermissions(thisActivity,
                        new String[] {
                                Permission
                        },
                        Code);
            }
        }
    }

}
