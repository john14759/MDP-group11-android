package com.example.mdp_group11.boundary;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.Nullable;

import com.example.mdp_group11.R;

public class ObstacleBoxView extends androidx.appcompat.widget.AppCompatTextView {

    public ObstacleBoxView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ObstacleView,
                0, 0);
        try {
            setTextSize(getResources().getDimensionPixelSize(R.dimen.large_text_size));
            setText("No Obstacles Left");
            setTextColor(Color.BLACK);
            setGravity(Gravity.CENTER);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}

