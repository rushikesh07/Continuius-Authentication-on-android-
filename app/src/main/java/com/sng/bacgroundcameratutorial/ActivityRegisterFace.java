package com.sng.bacgroundcameratutorial;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.transition.Visibility;
import android.util.Log;
import android.util.SparseArray;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ActivityRegisterFace extends AppCompatActivity implements Camera.FaceDetectionListener, TextureView.SurfaceTextureListener {
    ImageView iv_preview;
    Button btn_save,btn_retry;
    TextureView textureView;
    Camera mCamera;
    String TAG="My Tag";
    Bitmap detectedFaceBMP,confirmFaceBMP;
    boolean keepDetectface=true;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
       // getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        setContentView(R.layout.activity_register);
        init();

        //startFaceDetection();
    }

    void init()
    {
        iv_preview=(ImageView)findViewById(R.id.iv_preview);
        btn_save=(Button)findViewById(R.id.btn_save);
        btn_retry=(Button)findViewById(R.id.btn_retry);
        textureView=(TextureView)findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);
        setButtonsVisibility(View.INVISIBLE);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(detectedFaceBMP!=null)
                    new AsynkFaceDetector().execute();
            }
        });
        btn_retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setButtonsVisibility(View.INVISIBLE);
                detectedFaceBMP=null;
                keepDetectface=true;

            }
        });
    }

    void initCamera()
    {
        mCamera=Camera.open(MyLib.findFrontFacingCamera());
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = MyLib.getOptimalPreviewSize(sizes, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
        parameters.setPreviewSize(optimalSize.width,optimalSize.height);
        mCamera.setFaceDetectionListener(this);
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
        try {
            mCamera.setPreviewTexture(textureView.getSurfaceTexture());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        if(keepDetectface) {
            if (faces != null && faces.length == 1) {
                keepDetectface = false;
                new AsynkFaceDetector().execute();
            }
        }
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

    private String saveToInternalStorage(Bitmap bitmapImage){
        int id=MyLib.getRandomNumberInRange();
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,id+".jpg");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            MyLib.SaveFacePref(id,ActivityRegisterFace.this);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }
    void setButtonsVisibility(int visible)
    {
        btn_save.setVisibility(visible);
        btn_retry.setVisibility(visible);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        //startFaceDetection();

            initCamera();
            startFaceDetection();

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }
    class AsynkFaceDetector extends AsyncTask<Void,Void,Void>
    {
        FaceDetector faceDetector;
        Bitmap bitmap;
        SparseArray<Face> vision_faces=null;
        ProgressDialog pBuilder;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pBuilder=new ProgressDialog(ActivityRegisterFace.this);
            if(detectedFaceBMP==null)
            pBuilder.setMessage("Detecting Face...");
            if(detectedFaceBMP!=null)
            pBuilder.setMessage("Verifying Face...");
            faceDetector = new FaceDetector.Builder(getApplicationContext())
                    .setTrackingEnabled(false)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .build();
            if(!faceDetector.isOperational()){
                new AlertDialog.Builder(ActivityRegisterFace.this).setMessage("Could not set up the face detector!").show();
                return;
            }
            bitmap=textureView.getBitmap();
            if(vision_faces!=null)
            vision_faces.clear();
            pBuilder.show();

        }

        @Override
        protected Void doInBackground(Void... voids) {

            Frame frame = new Frame.Builder().setBitmap(bitmap).build();

            vision_faces = faceDetector.detect(frame);

            if(vision_faces!=null&&vision_faces.size()==1)
            {
                Face visionface = vision_faces.valueAt(0);
                if(visionface!=null) {
                    if(detectedFaceBMP==null) {
                        detectedFaceBMP = Bitmap.createBitmap(bitmap, (int) Math.ceil(visionface.getPosition().x), (int) visionface.getPosition().y, (int) Math.floor(visionface.getWidth()), (int) visionface.getHeight());

                    }
                    else
                    {
                        confirmFaceBMP = Bitmap.createBitmap(bitmap, (int) Math.ceil(visionface.getPosition().x), (int) visionface.getPosition().y, (int) Math.floor(visionface.getWidth()), (int) visionface.getHeight());
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            if(detectedFaceBMP!=null) {
                iv_preview.setImageBitmap(detectedFaceBMP);
                setButtonsVisibility(View.VISIBLE);
            }
            if(confirmFaceBMP!=null)
            {
                FaceNet facenet = null;
                try {
                    facenet = new FaceNet(getAssets());
                    double score = facenet.getSimilarityScore(detectedFaceBMP,confirmFaceBMP);
                    if(Math.ceil(score)<=15)
                    {
                        Toast.makeText(ActivityRegisterFace.this,"Welcome",Toast.LENGTH_SHORT).show();
                        saveToInternalStorage(detectedFaceBMP);
                        Intent intent=new Intent(ActivityRegisterFace.this,ActivityHome.class);
                        startActivity(intent);
                        finish();

                    }
                    else
                    {

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            pBuilder.dismiss();
            textureView.refreshDrawableState();
            super.onPostExecute(aVoid);
        }
    }
}
