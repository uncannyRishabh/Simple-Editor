package com.uncanny.simpleapplication.Views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.uncanny.simpleapplication.R;

/**
@author Rishabh Raj Gupta 12/2/2022
 */
public class CropView extends View{
    private final String TAG = "CropView";
    private Paint paint,linePaint,sPaint;
    private RectF rect,sRect;
    private float left,top,right,bottom;
    private float LEFT, TOP, RIGHT, BOTTOM;
    private float currX,currY,prevX, prevY;
    private Bitmap bitmap;
    private boolean linetop=false, lineleft=false, lineright=false, linebottom=false;
    private final int strokePadding=3;

    public CropView(Context context) {
        super(context);
        init();
    }

    public CropView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        paint = new Paint();
        rect = new RectF();
        linePaint = new Paint();
        sPaint = new Paint();
        sRect = new RectF();

        sPaint.setStyle(Paint.Style.FILL);
        sPaint.setColor(Color.parseColor("#80000000"));


        linePaint.setStrokeWidth(6);
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.purple_500));
        linePaint.setStyle(Paint.Style.STROKE);

        paint.setStrokeWidth(3f);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);

    }

    public int[] getDeltaCoords(){
        int[] coords;
        if(TOP!=top || LEFT !=left || RIGHT!=right || BOTTOM!=bottom){
            coords = new int[4];
            coords[0] = Math.max((int) (top - TOP), 0);
            coords[1] = Math.max((int) (left - LEFT), 0);
            coords[2] = Math.min((int) (bottom - TOP),(int) bottom);
            coords[3] = Math.min((int) (right - LEFT),(int) right);
        }
        else {
            coords = null;
        }
        return coords;
    }

    public void setViewBounds(int width, int height){
        rect.set(1,1,width,height);
        invalidate();
    }

    public void setViewBounds(RectF imageBounds) {
        left   = imageBounds.left ;
        top    = imageBounds.top ;
        right  = imageBounds.right ;
        bottom = imageBounds.bottom ;

        LEFT   = imageBounds.left ;
        TOP = imageBounds.top ;
        RIGHT = imageBounds.right ;
        BOTTOM = imageBounds.bottom ;

        invalidate();
    }

    public void setBitmap(Bitmap bitmap){
        
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        currX = event.getX();
        currY = event.getY();
        switch (action) {
            case MotionEvent.ACTION_MOVE: {

                if (currY > top - 30 && currY < top + 30) {
                    linetop = true;
                    if (currY - prevY > 0) { //TOP++
                        if (currY < bottom - 40)
                            top = currY;
                    } else { //TOP--
                        if (currY >= TOP)
                            top = currY;
                    }
                }

                if (currX > left - 30 && currX < left + 30) {
                    lineleft = true;
                    if (currX - prevX > 0) { //LEFT++
                        if (currX < right - 40)
                            left = currX;
                    } else { //LEFT--
                        if (currX >= LEFT)
                            left = currX;
                    }
                }

                if (currY > bottom - 30 && currY < bottom + 30) {
                    linebottom = true;
                    if (currY - prevY > 0) { //BOTTOM++
                        if (currY <= BOTTOM)
                            bottom = currY;
                    } else { //BOTTOM--
                        if (currY >= TOP)
                            bottom = currY;
                    }
                }

                if (currX > right - 30 && currX < right + 30) {
                    lineright = true;
                    if (currX - prevX > 0) { //RIGHT++
                        if (currX <= RIGHT)
                            right = currX;
                    } else { //RIGHT--
                        if (currX >= LEFT)
                            right = currX;
                    }
                }

                prevX = currX;
                prevY = currY;
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                break;
            }
            case MotionEvent.ACTION_UP: {
                linetop = false;
                lineleft = false;
                lineright = false;
                linebottom = false;
                break;
            }
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        rect.set(left+3,
                top+3,
                right-3,
                bottom-3);

        sRect.set(LEFT,
                TOP,
                RIGHT,
                BOTTOM);

        canvas.clipRect(rect, Region.Op.DIFFERENCE);
        canvas.drawRect(sRect,sPaint);
        canvas.drawRect(rect, paint);


        //top
        if(linetop) canvas.drawLine(left+strokePadding,top+strokePadding
                     , right-strokePadding,top+strokePadding, linePaint);
        //left
        if(lineleft) canvas.drawLine(left+strokePadding,top+strokePadding
                     , left+strokePadding,bottom-strokePadding, linePaint);
        //right
        if(lineright) canvas.drawLine(right-strokePadding,top+strokePadding
                     , right-strokePadding,bottom-strokePadding, linePaint);
        //bottom
        if(linebottom) canvas.drawLine(left+strokePadding,bottom-strokePadding
                     , right-strokePadding,bottom-strokePadding, linePaint);
    }

}
