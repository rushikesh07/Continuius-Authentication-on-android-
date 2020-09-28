package com.sng.bacgroundcameratutorial;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ActivitySetPinLock extends AppCompatActivity {
    EditText et_pin,et_comfirm_pin;
    Button btn_set_pin;
    int resultcode=404;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_pin_lock);
        et_pin=(EditText)findViewById(R.id.et_pin);
        et_comfirm_pin=(EditText)findViewById(R.id.et_comfirm_pin);
        btn_set_pin=(Button)findViewById(R.id.btn_set_pin);
        btn_set_pin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pin=et_pin.getText().toString();
                String cPin=et_comfirm_pin.getText().toString();
                if(!pin.trim().equals("")&&!cPin.trim().equals(""))
                {
                    if(pin.trim().equals(cPin.trim()))
                    {
                        MyLib.savePinPref(Integer.parseInt(pin),ActivitySetPinLock.this);
                        Toast.makeText(ActivitySetPinLock.this,"Pin set successfully.", Toast.LENGTH_SHORT).show();
                        resultcode=200;
                        setResult(resultcode);
                        finish();
                    }
                    else {
                        Toast.makeText(ActivitySetPinLock.this,"Confirm pin doesn't match with pin", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(ActivitySetPinLock.this,"Please enter both pin", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

    }
}
