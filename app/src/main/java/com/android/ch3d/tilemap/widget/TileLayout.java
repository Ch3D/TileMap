package com.android.ch3d.tilemap.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class TileLayout extends ViewGroup {

	public static final int GRID_SIZE = 5;

	private int mCurrentChildMaxWidth = 0;

	int xPos = 0;

	int yPos = 0;

	private float mTouchX = -1;

	private float mTouchY = -1;

	public TileLayout(final Context context) {
		super(context);
		init();
	}

	public TileLayout(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public TileLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public TileLayout(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	private void init() {
		setWillNotDraw(false);
		setScrollContainer(true);
		setVerticalScrollBarEnabled(true);
		setHorizontalScrollBarEnabled(true);
	}

	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		measureChildren(widthMeasureSpec, heightMeasureSpec);

		int w = 0;
		int h = 0;

		int count = getChildCount();
		for(int i = 0; i < count; i++) {
			View child = getChildAt(i);
			if(child.getVisibility() != GONE) {
				mCurrentChildMaxWidth = Math.max(mCurrentChildMaxWidth, child.getMeasuredWidth());
				w = Math.max(w, child.getMeasuredWidth());
				h = Math.max(h, child.getMeasuredHeight());
			}
		}

		h = Math.max(h, getSuggestedMinimumHeight());
		w = Math.max(w, getSuggestedMinimumWidth());

		w = resolveSize(w, widthMeasureSpec);
		h = resolveSize(h, heightMeasureSpec);

		setMeasuredDimension(GRID_SIZE * mCurrentChildMaxWidth, GRID_SIZE * mCurrentChildMaxWidth);
	}

	@Override
	protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
		int count = getChildCount();
		int currentRow = 0;
		int l1 = 0;
		for(int i = 0; i < count; i++) {
			View child = getChildAt(i);
			if(child.getVisibility() != GONE) {
				// child.layout((i % GRID_SIZE) * ITEM_SIZE, currentRow * ITEM_SIZE, child.getMeasuredWidth(), child.getMeasuredHeight());
				child.layout(l1, currentRow * mCurrentChildMaxWidth, child.getMeasuredWidth() + l1,
				             child.getMeasuredHeight() + (currentRow * mCurrentChildMaxWidth));
				l1 += mCurrentChildMaxWidth;
			}
			if(i == GRID_SIZE - 1) {
				currentRow++;
				l1 = 0;
			}
		}
	}

	@Override
	protected int computeHorizontalScrollExtent() {
		return 48;
	}

	@Override
	protected int computeHorizontalScrollOffset() {
		return 10;
	}

	@Override
	protected int computeHorizontalScrollRange() {
		return getWidth();
	}

	@Override
	protected int computeVerticalScrollExtent() {
		return getHeight() / 2;
	}

	@Override
	protected int computeVerticalScrollOffset() {
		return getHeight() / 2;
	}

	@Override
	protected int computeVerticalScrollRange() {
		return getHeight();
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mTouchX = event.getX();
				mTouchY = event.getY();
				break;

			case MotionEvent.ACTION_MOVE:
				if(mTouchX == -1 || mTouchY == -1) {
					break;
				}

				final float difX = mTouchX - event.getX();
				final float difY = mTouchY - event.getY();

				xPos += difX;
				yPos += difY;

				final int newX = Math.max(0, Math.min(xPos, getMeasuredWidth()));
				final int newY = Math.max(0, Math.min(yPos, getMeasuredHeight()));

				if(getScrollX() != newX || getScrollY() != newY) {
					scrollTo(newX, newY);
					mTouchX = event.getX();
					mTouchY = event.getY();
				} else {
					xPos = newX;
					yPos = newY;
				}
				break;

			case MotionEvent.ACTION_UP:
				mTouchX = -1;
				mTouchY = -1;
				break;
		}
		return true;
	}
}
