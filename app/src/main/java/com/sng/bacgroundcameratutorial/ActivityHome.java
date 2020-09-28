package com.sng.bacgroundcameratutorial;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import adapter.ApplistAdapter;
import models.AppModel;
import repository.*;
import services.CameraService;

import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static androidx.core.app.AppOpsManagerCompat.MODE_ALLOWED;

//import brodcasts.ServiceBroadcast;

public class ActivityHome extends AppCompatActivity {
    List<AppModel> appModels=new ArrayList<>();
    ApplistAdapter applistAdapter;

    //recycle view for listing installed application
    RecyclerView rv_applist;

    //Foreground service to start camera in background while app is not closed
    Intent cameraService;

    //Start and stop service
    Switch switch_service;
    //Request codes used in onActivityresult
    final int CODE_ACTION_USAGE_PERMISSION=101;
    private final int APP_OVERLAY_PERMISSION_REQUEST=102;
    private final int SET_PIN_REQUEST_CODE=103;
    private final int REQUEST_VERIFY_BY_PIN_FOR_SERVICE_ACTION=104;
    private final int REQUEST_VERIFY_PIN_FOR_RESET=105;
    SwichChangeListener swichChangeListener;
    AlertDialog.Builder builder;

    //initialise service to start
    boolean switch_service_value=true;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        cameraService=new Intent(ActivityHome.this, CameraService.class);;
        builder=new AlertDialog.Builder(this);
        swichChangeListener=new SwichChangeListener();


        if(!checkForActionUsagePermission(getApplicationContext()))
        {
            Intent permissionIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(permissionIntent, CODE_ACTION_USAGE_PERMISSION);
        }

        if(!checkForOverlayPermission(getApplicationContext()))
        {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, APP_OVERLAY_PERMISSION_REQUEST);
        }
        //Check for wether Action usage and overlay permission grantedif yes then start camera service
        if(checkForActionUsagePermission(getApplicationContext())&&checkForOverlayPermission(getApplicationContext()))
        {
            mstartService();
        }

        switch_service=(Switch)findViewById(R.id.switch_service);
        rv_applist=(RecyclerView)findViewById(R.id.rv_applist);

        rv_applist.setLayoutManager(new LinearLayoutManager(this));
        rv_applist.addItemDecoration(new DividerItemDecoration(rv_applist.getContext(), DividerItemDecoration.VERTICAL));

        //Asncronous loader to load list of installed applications
        new AsynkLoadAppList().execute();

        switch_service.setOnCheckedChangeListener(swichChangeListener);

    }
    private class AsynkLoadAppList extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Intent mainIntent=new Intent(Intent.ACTION_MAIN,null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> pkgAppList=ActivityHome.this.getPackageManager().queryIntentActivities(mainIntent,0);
            for(int i=0;i<pkgAppList.size();i++)
            {
                ResolveInfo resolveInfo=pkgAppList.get(i);
                //check for application is not system application
                if(!isSystemPackage(resolveInfo)){

                    AppModel appModel=new AppModel();
                    appModel.setApp_package_name(resolveInfo.activityInfo.packageName);
                    PackageManager packageManager=ActivityHome.this.getPackageManager();
                    try {
                        ApplicationInfo applicationInfo=packageManager.getApplicationInfo(resolveInfo.activityInfo.packageName,0);
                        appModel.setApp_name((String)packageManager.getApplicationLabel(applicationInfo));
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    if(!appModel.getApp_package_name().equals("com.sng.bacgroundcameratutorial"))
                        appModels.add(appModel);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Collections.sort(appModels, new Comparator<AppModel>() {

                @Override
                public int compare(AppModel lhs, AppModel rhs) {
                    //here getTitle() method return app name...
                    return lhs.getApp_name().compareTo(rhs.getApp_name());

                }
            });
            applistAdapter=new ApplistAdapter(appModels,ActivityHome.this);

            rv_applist.setAdapter(applistAdapter);
        }
    }


    public boolean isSystemPackage(ResolveInfo resolveInfo){

        return ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }
    private boolean checkForActionUsagePermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
        return mode == MODE_ALLOWED;
    }
    private boolean checkForOverlayPermission(Context context) {
        boolean result=true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
        {
            result=false;
        }
        return result;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==CODE_ACTION_USAGE_PERMISSION)
        {
            if(!checkForActionUsagePermission(getApplicationContext()))
            {
                Intent permissionIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivityForResult(permissionIntent, CODE_ACTION_USAGE_PERMISSION);
            }
            if(checkForActionUsagePermission(getApplicationContext())&&checkForOverlayPermission(getApplicationContext())) {

                mstartService();
            }

        }

        if(requestCode==APP_OVERLAY_PERMISSION_REQUEST)
        {
            if(!checkForOverlayPermission(getApplicationContext()))
            {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, APP_OVERLAY_PERMISSION_REQUEST);
            }
            if(checkForActionUsagePermission(getApplicationContext())&&checkForOverlayPermission(getApplicationContext()))
            {
                mstartService();
            }
        }

        if(requestCode==SET_PIN_REQUEST_CODE)
        {
            if(MyLib.getPinPref(getApplicationContext())==0) {
                Intent intent=new Intent(ActivityHome.this,ActivitySetPinLock.class);
                startActivityForResult(intent,SET_PIN_REQUEST_CODE);
            }
            else {
                mstartService();
            }
        }

        if(requestCode==REQUEST_VERIFY_BY_PIN_FOR_SERVICE_ACTION)
        {
            if (resultCode == 200)
            {
                if (switch_service_value == true)
                {

                    if (!MyLib.isServiceRunning(ActivityHome.this,CameraService.class))
                    {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        {
                            getApplicationContext().startForegroundService(cameraService);
                        } else {
                            getApplicationContext().startService(cameraService);
                        }
                    }
                }

                else
                {
                    if (MyLib.isServiceRunning(ActivityHome.this,CameraService.class))
                        stopService(cameraService);
                }
            }
            else
            {
                switch_service.setOnCheckedChangeListener(null);
                switch_service.setChecked(!switch_service_value);
                switch_service.setOnCheckedChangeListener(swichChangeListener);
            }
        }

        if(requestCode==REQUEST_VERIFY_PIN_FOR_RESET)
        {

            if (resultCode == 200) {
                Intent intent = new Intent(ActivityHome.this, ActivitySetPinLock.class);
                startActivityForResult(intent, SET_PIN_REQUEST_CODE);
            }

        }
    }

    class SwichChangeListener implements CompoundButton.OnCheckedChangeListener{

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            switch_service_value=b;
            if (switch_service_value == true) {
                builder.setMessage("Are you sure you want to start CAS ?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        Intent intent=new Intent(ActivityHome.this,ActivityVarifyByPin.class);
                        startActivityForResult(intent,REQUEST_VERIFY_BY_PIN_FOR_SERVICE_ACTION);
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        switch_service.setOnCheckedChangeListener(null);
                        switch_service.setChecked(false);
                        switch_service.setOnCheckedChangeListener(swichChangeListener);
                    }
                });

            }
            else {
                builder.setMessage("Are you sure you want to stop CAS ?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        Intent intent=new Intent(ActivityHome.this,ActivityVarifyByPin.class);
                        startActivityForResult(intent,REQUEST_VERIFY_BY_PIN_FOR_SERVICE_ACTION);
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        switch_service.setOnCheckedChangeListener(null);
                        switch_service.setChecked(true);
                        switch_service.setOnCheckedChangeListener(swichChangeListener);
                    }
                });

            }
            builder.show();


        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.menu_reset_pin:
                Intent intent=new Intent(ActivityHome.this,ActivityVarifyByPin.class);
                startActivityForResult(intent,REQUEST_VERIFY_PIN_FOR_RESET);
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menus) {
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.menus,menus);

        return true;
    }

    @Override
    protected void onDestroy() {
        //stopService(cameraService);
        super.onDestroy();
    }
    public void mstartService()
    {
        if(MyLib.getPinPref(getApplicationContext())!=0) {
            mSqiteHelper myMSqiteHelper = new mSqiteHelper(getApplicationContext());
            // insert package info of setting application in db
            myMSqiteHelper.insertSettingPackage();
            myMSqiteHelper.insertSamsungSettingPackage();
            //insert cas app package info
            myMSqiteHelper.insertDefaultApp();
            if(!MyLib.isServiceRunning(ActivityHome.this,CameraService.class)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getApplicationContext().startForegroundService(cameraService);
                } else {
                    getApplicationContext().startService(cameraService);
                }
            }
        }
        else
        {
            Intent intent=new Intent(ActivityHome.this,ActivitySetPinLock.class);
            startActivityForResult(intent,SET_PIN_REQUEST_CODE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

}
