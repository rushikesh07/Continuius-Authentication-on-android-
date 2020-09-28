package services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.sng.bacgroundcameratutorial.FaceNet;
import com.sng.bacgroundcameratutorial.Layer;
import com.sng.bacgroundcameratutorial.MyLib;
import com.sng.bacgroundcameratutorial.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import repository.mSqiteHelper;

public class CameraService extends Service implements Camera.FaceDetectionListener {
    Camera mCamera;
    Layer layer;
    private boolean cameraFront = false;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    WindowManager wm;
    Bitmap storeImgBitmap;
    WindowManager.LayoutParams layoutParams;
    WindowManager.LayoutParams layerLayoutParam;
    int windowFlag;
    TextureView textureView;
    String TAG="My Tag";
    boolean detected=false;
    boolean insideAsynk=false;
    mSqiteHelper mSqiteHelper;
    int maxNumberofAttempts=5;
    int numberofattempts=0;
    public boolean verifybypin=false;
    public boolean waitFaceRecognition=false;
    View pinVerificationWindow;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        layer=new Layer(this);
        mSqiteHelper=new mSqiteHelper(getApplicationContext());
        layer.setColor(100,0,0,200);
        windowFlag =Build.VERSION.SDK_INT >= 26 ? 2038 : 2002;
        layerLayoutParam = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT ,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        initComonent();

        storeImgBitmap=getFaceImageBitmap();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Detecting Face")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(false);

            Notification notification = builder.build();
            startForeground(1, notification);

        } else {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Detecting Face")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(false);

            Notification notification = builder.build();
            startForeground(1, notification);
        }
        Handler handler=new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                createTextureView();
                wm.addView(layer,layerLayoutParam);
            }
        },2000);
        return START_STICKY;
    }
    private  void initComonent()
    {
        mCamera=Camera.open(findFrontFacingCamera());
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = getOptimalPreviewSize(sizes, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
        parameters.setPreviewSize(optimalSize.width,optimalSize.height);
        mCamera.setFaceDetectionListener(this);
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
    }
    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
    public void startFaceDetection() {
        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();

        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0) {
            // camera supports face detection, so can start it:
            mCamera.startFaceDetection();
        }
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera)
    {

        if (mSqiteHelper.isAppPresent(getCurrentForegroundApp()) == 1) {
            if(waitFaceRecognition==false) {
                if(textureView.getVisibility()==View.GONE)
                    textureView.setVisibility(View.VISIBLE);
                if (faces.length <= 0 || faces == null || faces.length > 1) {
                    detected = false;
                    if (layer.getWindowToken() == null)
                        wm.addView(layer, layerLayoutParam);
                }
                if (faces.length == 1 && detected == false) {
                    detected = true;
                    if (textureView.getWindowToken() != null)
                        new AsynkFaceRecognition().execute();
                }
            }
        } else {
            if (layer.getWindowToken() != null)
                wm.removeView(layer);
            if (pinVerificationWindow!=null&&pinVerificationWindow.getWindowToken() != null)
                wm.removeView(pinVerificationWindow);
            if(textureView.getVisibility()==View.VISIBLE)
                textureView.setVisibility(View.GONE);
            waitFaceRecognition=false;

        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(layer != null&&layer.getWindowId()!=null)
        {
            wm.removeView(layer);
            layer = null;
        }
        if(textureView!=null&&textureView.getWindowToken()!=null)
        {
            textureView.setSurfaceTextureListener(null);
            wm.removeView(textureView);
        }
        if(mCamera!=null)
        {
            mCamera.stopPreview();
            mCamera.setFaceDetectionListener(null);
            mCamera=null;
        }
        stopForeground(true);
        stopSelf();
    }

    private void createTextureView()
    {
        textureView=new TextureView(this);
        textureView.setSurfaceTextureListener(new mTextureListener());
        Display display=wm.getDefaultDisplay();
        //WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(display.getWidth()/4 , display.getHeight()/4 , windowFlag, 40, -3);
        layoutParams = new WindowManager.LayoutParams(
                display.getWidth()/4 ,
                display.getHeight()/4,
                windowFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        //Specify the view position
        layoutParams.gravity = Gravity.TOP;       //Initially view will be added to top-left corner
        layoutParams.x = (display.getWidth()-display.getWidth()/4)-10;
        layoutParams.y = 0;
        textureView.setOnTouchListener(new View.OnTouchListener()
        {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        initialX = layoutParams.x;
                        initialY = layoutParams.y;

                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        layoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);

                        wm.updateViewLayout(textureView, layoutParams);
                        return true;
                }
                return false;
            }
        });
        wm.addView(textureView,layoutParams);
    }


    private Bitmap getFaceImageBitmap()
    {
        Bitmap b=null;
        try {
            ContextWrapper cw = new ContextWrapper(getApplicationContext());
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            // Create imageDir
            File f=new File(directory, MyLib.getFacePref(getApplicationContext())+".jpg");
            b = BitmapFactory.decodeStream(new FileInputStream(f));

        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return b;
    }



    class AsynkFaceRecognition extends AsyncTask<Void,Void,Void>
    {

        Bitmap textureBMP;
        double score=20;
        SparseArray<Face> vision_faces;
        @Override
        protected void onPreExecute() {

            textureBMP=textureView.getBitmap();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            FaceDetector faceDetector = new
                    FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                    .build();
            if(!faceDetector.isOperational()){
                //new AlertDialog.Builder(v.getContext()).setMessage("Could not set up the face detector!").show();
                return null;
            }

            try {
                Frame frame = new Frame.Builder().setBitmap(textureBMP).build();
                vision_faces = faceDetector.detect(frame);
                if (vision_faces != null && vision_faces.size() == 1) {
                    Face visionface = vision_faces.valueAt(0);
                    if (visionface != null) {

                        Bitmap curentBMP = Bitmap.createBitmap(textureBMP, (int) Math.ceil(visionface.getPosition().x), (int) visionface.getPosition().y, (int) Math.floor(visionface.getWidth()), (int) visionface.getHeight());
                        FaceNet facenet = null;
                        facenet = new FaceNet(getAssets());
                        score = facenet.getSimilarityScore(storeImgBitmap, curentBMP);
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }



            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.e("score=",""+score);
            if(detected==true) {
                //if no face detected
                if (vision_faces == null) {
                    if (layer.getWindowToken() == null)
                        wm.addView(layer, layerLayoutParam);
                }
                if (Math.ceil(score) <= 15) {
                    if (layer.getWindowToken() != null)
                        wm.removeView(layer);
                    numberofattempts=1;

                } else {
                    if (layer.getWindowToken() == null)
                        wm.addView(layer, layerLayoutParam);
                    if(numberofattempts==maxNumberofAttempts)
                    {
                        waitFaceRecognition=true;
                        if(MyLib.getPinPref(getApplicationContext())!=0)
                            AskPinVerification();
                    }
                    else
                        numberofattempts++;
                }
                detected=false;
            }

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

        }
    }
    private String getCurrentForegroundApp()
    {
        String topPackageName = "NA";
        Log.e("Task List", "Current App in foreground is: " + topPackageName);
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.RunningTaskInfo foregroundTaskInfo = am.getRunningTasks(1).get(0);
            topPackageName = foregroundTaskInfo.topActivity.getPackageName();
        }else{
            UsageStatsManager usage = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> stats = usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000*1000, time);
            if (stats != null) {
                SortedMap<Long, UsageStats> runningTask = new TreeMap<Long,UsageStats>();
                for (UsageStats usageStats : stats) {
                    runningTask.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (runningTask.isEmpty()) {
                    topPackageName="None";
                }
                else
                    topPackageName =  runningTask.get(runningTask.lastKey()).getPackageName();
            }
        }
        //is_appopenfirstTime(topPackageName);
        Log.e("Task List", "Current App in foreground is: " + topPackageName);
        return topPackageName;
    }

    EditText et_pin;
    Button btn_set_pin;

    public void AskPinVerification()
    {

        pinVerificationWindow = LayoutInflater.from(this).inflate(R.layout.activity_verifybypin, null);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.CENTER;

        textureView.setVisibility(View.GONE);
        wm.addView(pinVerificationWindow, params);
        et_pin=(EditText)pinVerificationWindow.findViewById(R.id.et_pin);
        initCalculator();
        btn_set_pin=(Button) pinVerificationWindow.findViewById(R.id.btn_set_pin);
        btn_set_pin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pin=et_pin.getText().toString();
                if(!pin.trim().equals(""))
                {
                    if(pin.trim().equals(""+MyLib.getPinPref(getApplicationContext())))
                    {
                        textureView.setVisibility(View.VISIBLE);
                        if (layer.getWindowToken() != null)
                            wm.removeView(layer);
                        if (pinVerificationWindow.getWindowToken() != null)
                            wm.removeView(pinVerificationWindow);

                        numberofattempts=1;
                        waitFaceRecognition=false;


                    }
                    else {
                        Toast.makeText(getApplicationContext(),"Wrong Pin..", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(),"Please enter pin", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void initCalculator()
    {
        Button btn_1=(Button)pinVerificationWindow.findViewById(R.id.btn_1);
        Button btn_2=(Button)pinVerificationWindow.findViewById(R.id.btn_2);
        Button btn_3=(Button)pinVerificationWindow.findViewById(R.id.btn_3);
        Button btn_4=(Button)pinVerificationWindow.findViewById(R.id.btn_4);
        Button btn_5=(Button)pinVerificationWindow.findViewById(R.id.btn_5);
        Button btn_6=(Button)pinVerificationWindow.findViewById(R.id.btn_6);
        Button btn_7=(Button)pinVerificationWindow.findViewById(R.id.btn_7);
        Button btn_8=(Button)pinVerificationWindow.findViewById(R.id.btn_8);
        Button btn_9=(Button)pinVerificationWindow.findViewById(R.id.btn_9);
        Button btn_0=(Button)pinVerificationWindow.findViewById(R.id.btn_0);
        Button btn_clear=(Button)pinVerificationWindow.findViewById(R.id.btn_clear);
        btn_0.setOnClickListener(new CalculaorListener());
        btn_1.setOnClickListener(new CalculaorListener());
        btn_2.setOnClickListener(new CalculaorListener());
        btn_3.setOnClickListener(new CalculaorListener());
        btn_4.setOnClickListener(new CalculaorListener());
        btn_5.setOnClickListener(new CalculaorListener());
        btn_6.setOnClickListener(new CalculaorListener());
        btn_7.setOnClickListener(new CalculaorListener());
        btn_8.setOnClickListener(new CalculaorListener());
        btn_9.setOnClickListener(new CalculaorListener());
        btn_clear.setOnClickListener(new CalculaorListener());

    }
    class CalculaorListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            switch (view.getId())
            {
                case R.id.btn_0:
                    et_pin.setText(et_pin.getText().toString()+"0");
                    break;
                case R.id.btn_1:
                    et_pin.setText(et_pin.getText().toString()+"1");
                    break;
                case R.id.btn_2:
                    et_pin.setText(et_pin.getText().toString()+"2");
                    break;
                case R.id.btn_3:
                    et_pin.setText(et_pin.getText().toString()+"3");
                    break;
                case R.id.btn_4:
                    et_pin.setText(et_pin.getText().toString()+"4");
                    break;
                case R.id.btn_5:
                    et_pin.setText(et_pin.getText().toString()+"5");
                    break;
                case R.id.btn_6:
                    et_pin.setText(et_pin.getText().toString()+"6");
                    break;
                case R.id.btn_7:
                    et_pin.setText(et_pin.getText().toString()+"7");
                    break;
                case R.id.btn_8:
                    et_pin.setText(et_pin.getText().toString()+"8");
                    break;
                case R.id.btn_9:
                    et_pin.setText(et_pin.getText().toString()+"9");
                    break;
                case R.id.btn_clear:
                    et_pin.setText("");
                    break;
            }
        }
    }
    class mTextureListener implements TextureView.SurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewTexture(textureView.getSurfaceTexture());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mCamera.startPreview();
            startFaceDetection();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    }
}
