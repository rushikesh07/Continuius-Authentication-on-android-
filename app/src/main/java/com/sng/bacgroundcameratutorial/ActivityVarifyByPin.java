package com.sng.bacgroundcameratutorial;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import services.CameraService;

public class ActivityVarifyByPin extends AppCompatActivity {
    EditText et_pin,et_comfirm_pin;
    Button btn_set_pin;
    TextView tv_Cpin,tv_pin;
    int AutorizationAttempt=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_pin_lock);
        et_pin=(EditText)findViewById(R.id.et_pin);
        et_comfirm_pin=(EditText)findViewById(R.id.et_comfirm_pin);
        et_comfirm_pin.setVisibility(View.GONE);
        tv_Cpin=(TextView)findViewById(R.id.tv_Cpin);
        tv_pin=(TextView)findViewById(R.id.tv_pin);
        tv_Cpin.setVisibility(View.GONE);
        tv_pin.setText("Enter your pin ");
        btn_set_pin=(Button)findViewById(R.id.btn_set_pin);
        btn_set_pin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pin=et_pin.getText().toString();
                if(!pin.trim().equals(""))
                {
                    if(pin.trim().equals(""+MyLib.getPinPref(ActivityVarifyByPin.this)))
                    {
                        /*Intent intent=new Intent(ActivityVarifyByPin.this,ActivityHome.class);
                        startActivity(intent);*/
                        setResult(200);
                        finish();
                    }
                    else {
                        AutorizationAttempt++;
                        et_pin.setText("");
                        if(AutorizationAttempt<3)
                        Toast.makeText(ActivityVarifyByPin.this,"Wrong Pin..", Toast.LENGTH_SHORT).show();
                        else
                        finishAffinity();
                    }
                }
                else {
                    Toast.makeText(ActivityVarifyByPin.this,"Please enter pin", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
