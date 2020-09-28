package com.sng.bacgroundcameratutorial;

import android.graphics.Canvas;
import android.content.Context;
import android.view.View;

public class Layer extends View
{
    private int a;
    private int b;
    private int g;
    private int r;

    public Layer(Context context){
        super(context);
    }

    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawARGB(this.a, this.r, this.g, this.b);
    }

    public void setColor(int a, int r, int g, int b){
        this.a = a;
        this.r = r;
        this.g = g;
        this.b = b;
        invalidate();
    }
}
