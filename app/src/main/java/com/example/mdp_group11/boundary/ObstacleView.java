package com.example.mdp_group11.boundary;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.example.mdp_group11.R;
import com.example.mdp_group11.control.GridControl;
import com.example.mdp_group11.enums.FaceDirection;
import com.example.mdp_group11.enums.GridState;
import com.example.mdp_group11.utils.MyDragShadowBuilder;

public class ObstacleView extends androidx.appcompat.widget.AppCompatTextView {
    public static String TAG = "BluetoothDialogFragment";

    boolean moved;

    private int id;
    private int gridInterval;
    private int x;
    private int y;
    public FaceDirection imageFace=FaceDirection.NONE;
    public boolean setOnMap = false; // true when obstacle has been placed on map
    private float initX;
    private float initY;
    private boolean hasEntered = false;
    private int lastSnapX;
    private int lastSnapY;
    private boolean hasFocused = false;
    private float fullResetX;
    private float fullResetY;
    private int originalParamWidth;
    private int originalParamHeight;

    private GridControl gridControl;

    public ObstacleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ObstacleView,
                0, 0);
        try {
            id = Integer.parseInt(getText().toString());
            setTextSize(getResources().getDimensionPixelSize(R.dimen.large_text_size));
            setTextColor(Color.WHITE);
            setBackgroundResource(R.color.black);
            setGravity(Gravity.CENTER);
        } finally {
            a.recycle();
        }

        gridControl=GridControl.getInstance();

        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                DragShadowBuilder myShadow = new MyDragShadowBuilder(view,gridInterval);

                view.startDragAndDrop(null,
                        myShadow,
                        ObstacleView.this,
                        DRAG_FLAG_OPAQUE);

                return true;
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (hasWindowFocus){
            fullResetX=getX();
            fullResetY=getY();
            hasFocused=true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.RED);
        borderPaint.setStrokeWidth(10);

        switch (imageFace) {
            case NORTH:
                canvas.drawLine(0, 0, gridInterval, 0, borderPaint);
                break;
            case SOUTH:
                canvas.drawLine(0, gridInterval, gridInterval, gridInterval, borderPaint);
                break;
            case WEST:
                canvas.drawLine(0, 0, 0, gridInterval, borderPaint);
                break;
            case EAST:
                canvas.drawLine(gridInterval, 0, gridInterval, gridInterval, borderPaint);
                break;
            case NONE:
                break;
            default:
                break;
        }
    }

    public int[] getMovePotential(float xCoord, float yCoord){
        x = (int) Math.floor(xCoord / gridInterval);
        int screenY = (int) Math.floor(yCoord / gridInterval);
        y = 19 - screenY;
        int[] returnArr={x-1,y};
        return returnArr;
    }

    public void move(float xCoord, float yCoord) {
        if (!setOnMap) {
            initX = getX();
            initY = getY();
        }

        x = (int) Math.floor(xCoord / gridInterval);
        int screenY = (int) Math.floor(yCoord / gridInterval);
        y = 19 - screenY;

        setX(x * gridInterval);
        setY(screenY * gridInterval);

        this.lastSnapX=(int) Math.floor(xCoord / gridInterval);
        this.lastSnapY=19 - screenY;

        setVisibility(VISIBLE);
    }

    public void reset() {
        if (setOnMap) {
            x = -1;
            y = -1;
            imageFace = FaceDirection.NONE;
            setOnMap = false;

            setText(String.valueOf(id));
            setBackgroundResource(R.color.black);

            setX(initX);
            setY(initY);

            invalidate();
        }
        setVisibility(VISIBLE);
    }

    public void reset(boolean s) {
        if (setOnMap || s) {

            x = -1;
            y = -1;
            imageFace = FaceDirection.NONE;
            setOnMap = false;

            setText(String.valueOf(id));
            setBackgroundResource(R.color.black);

            setX(initX);
            setY(initY);

            invalidate();
        }
        setVisibility(VISIBLE);
    }

    public void fullReset(String s) {
        enlarge();
        x = -1;
        y = -1;
        imageFace = FaceDirection.NONE;
        setOnMap = false;

        setText(String.valueOf(id));
        setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        setBackgroundResource(R.color.black);

        if (s.equals("p")) {
            setX((float)630.0);
            setY((float)20.0);
        }
        else if (s.equals("h")){
            setX((float)3.168);
            setY((float)563.168);
        }
        else{
            setX(fullResetX);
            setY(fullResetY);
        }

        if (hasEntered){
            imageClear();
            hasEntered=false;
        }

        invalidate();

        setVisibility(VISIBLE);
    }

    public void setGridInterval(int gridInterval) {
        this.gridInterval = gridInterval;
        ViewGroup.LayoutParams params = getLayoutParams();
    }

    public void shrink(){
        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = this.gridInterval;
        params.width = this.gridInterval;
        setLayoutParams(params);
    }

    public void setOriginalParamHeight(int originalParamHeight) {
        this.originalParamHeight = originalParamHeight;
    }

    public void setOriginalParamWidth(int originalParamWidth) {
        this.originalParamWidth = originalParamWidth;
    }

    public void enlarge(){

        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = originalParamHeight;
        params.width = originalParamWidth;
        setLayoutParams(params);
    }

    public void setImageFace(FaceDirection imageFace) {
        this.imageFace = imageFace;
        invalidate();
    }

    public void setImage(int imageID) {
        setText("");
        String imageResourceID = String.format("obstacle_image_%d", imageID);
        int imageResourceIntID = getResources().getIdentifier(imageResourceID, "drawable", getContext().getPackageName());
        if (imageResourceIntID != 0) setBackgroundResource(imageResourceIntID);
    }

    public int getObstacleId() {
        return id;
    }

    public void setFullResetX(float x){
        fullResetX=x;
    }

    public void setFullResetY(float y){
        fullResetY=y;
    }

    public int getGridX() {
        return x-1;
    }

    public int getGridY() {
        return y;
    }

    public int getInitX(){return (int)initX;}

    public int getInitY(){return (int)initY;}

    public FaceDirection getImageFace(){ return imageFace; }

    public int getLastSnapX(){return lastSnapX;}

    public int getLastSnapY(){return lastSnapY;}

    public void setHasEntered(boolean hasEntered){this.hasEntered=hasEntered;}
    public boolean getHasEntered(){return hasEntered;}

    public void imagePlace(){
        gridControl.setCellState(getLastSnapX()-1,19-getLastSnapY(), GridState.IMAGEBLOCK);
        invalidate();
    }

    public void imageClear(){
        gridControl.setCellState(getLastSnapX()-1,19-getLastSnapY(),GridState.EMTPY);
        invalidate();
    }

    public void updateImage(String s){
        setText(s);
        setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        invalidate();
    }

    public void moveShrink(int x,int y,FaceDirection faceDirection){
        setY((19-y)*gridInterval);
        setX((x+1)*gridInterval);
        imageFace=faceDirection;
        shrink();
        invalidate();
    }
}