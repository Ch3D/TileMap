package com.android.ch3d.tilemap.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.R;
import com.android.ch3d.tilemap.manager.TilesManager;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class TileLayout extends ViewGroup {

	public interface OnVisibleRectangleListener {
		void onVisibleRectangleChanged(Rect mRect);
	}

	public static final int GRID_SIZE = 10;

	private static final String TAG = TileLayout.class.getSimpleName();

	private int mItemSize = -1;

	private int mXPos = 0;

	private int mYPos = 0;

	private float mTouchX = -1;

	private float mTouchY = -1;

	private OnVisibleRectangleListener mListener;

	private int mDisplayHeight;

	private int mDisplayWidth;

	private TilesManager mTilesManager;

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

	@Override
	public void addView(final View child) {
		super.addView(child, new LayoutParams(256, 256));
	}

	private int getChildIndex(final int i, final int j) {return (j * GRID_SIZE) + i;}

	private void init() {
		setWillNotDraw(false);
		setScrollContainer(true);
		setVerticalScrollBarEnabled(true);
		setHorizontalScrollBarEnabled(true);

		mItemSize = getResources().getDimensionPixelSize(R.dimen.item_size);

		final DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
		mDisplayHeight = displayMetrics.heightPixels;
		mDisplayWidth = displayMetrics.widthPixels;
	}

	public void initSize(final int count) {
		for(int i = 0; i < count; i++) {
			final ImageView child = new ImageView(getContext());
			addView(child);
			child.setBackgroundColor(Color.RED);
		}
	}

	public void initTiles() {
		updateVisibleTiles(0, 0, mDisplayWidth, mDisplayHeight);
	}

	@Override
	protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
		int count = getChildCount();
		int currentRow = 0;
		int l1 = 0;
		for(int i = 0; i < count; i++) {
			View child = getChildAt(i);
			if(child.getVisibility() != GONE) {
				child.layout(l1, currentRow * mItemSize, child.getMeasuredWidth() + l1,
				             child.getMeasuredHeight() + (currentRow * mItemSize));
				l1 += mItemSize;
			}
			if((i % GRID_SIZE) == GRID_SIZE - 1) {
				currentRow++;
				l1 = 0;
			}
		}
	}

	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		measureChildren(widthMeasureSpec, heightMeasureSpec);

		setMeasuredDimension(GRID_SIZE * mItemSize, GRID_SIZE * mItemSize);
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

				mXPos += difX;
				mYPos += difY;

				final int newX = Math.max(0, Math.min(mXPos, getMeasuredWidth() - mDisplayWidth));
				final int newY = Math.max(0, Math.min(mYPos, getMeasuredHeight() - mDisplayHeight));

				if(getScrollX() != newX || getScrollY() != newY) {
					scrollTo(newX, newY);
					mTouchX = event.getX();
					mTouchY = event.getY();
				}
				mXPos = newX;
				mYPos = newY;
				break;

			case MotionEvent.ACTION_UP:
				if(mListener != null) {
					final int right = mXPos + mDisplayWidth;
					final int bottom = mYPos + mDisplayHeight;

					mListener.onVisibleRectangleChanged(new Rect(mXPos, mYPos, right, bottom));

					updateVisibleTiles(mXPos, mYPos, right, bottom);
				}
				mTouchX = -1;
				mTouchY = -1;
				break;
		}
		return true;
	}

	public void setTilesManager(final TilesManager tilesManager) {
		mTilesManager = tilesManager;
	}

	public void setVisibleRectangleListener(OnVisibleRectangleListener listener) {
		mListener = listener;
	}

	private void updateVisibleTiles(final int left, final int top, final int right, final int bottom) {
		final int leftIndexX = left / mItemSize;
		final int rightIndexX = right / mItemSize;
		final int topIndexY = top / mItemSize;
		final int bottomIndexY = bottom / mItemSize;

		if(BuildConfig.DEBUG) {
			Log.d(TAG, "Visible tiles x = [" + leftIndexX + ", " + rightIndexX + "]");
			Log.d(TAG, "Visible tiles y = [" + topIndexY + ", " + bottomIndexY + "]");
		}

		for(int i = leftIndexX; i < (rightIndexX + 1); i++) {
			for(int j = topIndexY; j < (bottomIndexY + 1); j++) {
				if(BuildConfig.DEBUG) {
					Log.d(TAG, "Trying to load tile for [" + i + ", " + j + "]");
				}
				final int index = getChildIndex(i, j);
				if(index < getChildCount()) {
					mTilesManager.loadTile(i, j, (ImageView) getChildAt(index));
				}
			}
		}
	}

}
