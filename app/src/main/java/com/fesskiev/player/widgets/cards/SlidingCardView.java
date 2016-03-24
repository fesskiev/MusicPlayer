package com.fesskiev.player.widgets.cards;


import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.fesskiev.player.R;

public class SlidingCardView extends FrameLayout implements View.OnClickListener {

    public interface OnSlidingCardListener {
        void onDeleteClick();

        void onEditClick();

        void onClick();
    }

    private static final int MIN_DISTANCE = 100;

    private GestureDetector detector;
    private OnSlidingCardListener listener;
    private View slidingContainer;
    private float x1;
    private float x2;
    private boolean isOpen;

    public SlidingCardView(Context context) {
        super(context);
        init(context);
    }

    public SlidingCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SlidingCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.card_sliding_layout, this, true);

        view.findViewById(R.id.editButton).setOnClickListener(this);
        view.findViewById(R.id.deleteButton).setOnClickListener(this);

        slidingContainer = view.findViewById(R.id.slidingContainer);

        detector = new GestureDetector(getContext(), new GestureListener());
    }


    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (listener != null) {
                listener.onClick();
            }
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                return true;
            case MotionEvent.ACTION_CANCEL:
                x2 = event.getX();
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    if (x2 > x1) {
                        isOpen = false;
                        animateSlidingContainer();
                    } else {
                        isOpen = true;
                        animateSlidingContainer();
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.editButton:
                if (listener != null && isOpen) {
                    listener.onEditClick();
                }
                break;
            case R.id.deleteButton:
                if (listener != null && isOpen) {
                    listener.onDeleteClick();
                }
                break;
        }
    }

    private void animateSlidingContainer() {
        int marginInPixels = (int) getResources().getDimension(R.dimen.card_view_margin_start);
        float value = isOpen ? -slidingContainer.getWidth() / 3 : marginInPixels;
        slidingContainer.
                animate().
                x(value).
                setDuration(500);
    }

    public void setOnSlidingCardListener(OnSlidingCardListener listener) {
        this.listener = listener;
    }
}