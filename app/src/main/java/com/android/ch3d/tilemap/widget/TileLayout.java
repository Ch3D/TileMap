package com.android.ch3d.tilemap.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.R;
import com.android.ch3d.tilemap.model.TilesManager;
import com.android.ch3d.tilemap.util.Utils;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class TileLayout extends ViewGroup {

    private static final String TAG = TileLayout.class.getSimpleName();
    public static final int PRELOAD_OFFSET = 2;

    public int DEFAULT_VALUE = -1;

    private int mItemSize = DEFAULT_VALUE;

    private int mXPos = 0;

    private int mYPos = 0;

    private float mTouchX = DEFAULT_VALUE;

    private float mTouchY = DEFAULT_VALUE;

    private int mDisplayHeight;

    private int mDisplayWidth;

    private TilesManager mTilesManager;

    private int mGridSize = 0;

    private int mLastLeftIndexX;

    private int mLastRightIndexX;

    private int mLastTopIndexY;

    private int mLastBottomIndexY;

    private int mToolbarHeight;

    private int mStatusBarHeight;

    private int mNavBarHeight;

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

    @Override
    public void addView(final View child) {
        super.addView(child, new LayoutParams(mItemSize, mItemSize));
    }

    private int getChildIndex(final int i, final int j) {
        return (j * mGridSize) + i;
    }

    private void init() {
        setWillNotDraw(false);
        setScrollContainer(true);
        setVerticalScrollBarEnabled(true);
        setHorizontalScrollBarEnabled(true);

        mItemSize = getResources().getDimensionPixelSize(R.dimen.item_size);
        mToolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_height);
        mStatusBarHeight = 0;
        mNavBarHeight = Utils.getNavigationBarHeight(getContext());

        final DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        mDisplayHeight = displayMetrics.heightPixels;
        mDisplayWidth = displayMetrics.widthPixels;
    }

    public void initSize(final int count) {
        for (int i = 0; i < count; i++) {
            addView(new ImageView(getContext()));
        }
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
        int count = getChildCount();
        int currentRow = 0;
        int column = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                child.layout(column, currentRow * mItemSize, child.getMeasuredWidth() + column,
                        child.getMeasuredHeight() + (currentRow * mItemSize));
                column += mItemSize;
            }
            if ((i % mGridSize) == mGridSize - 1) {
                currentRow++;
                column = 0;
            }
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mGridSize * mItemSize, mGridSize * mItemSize);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchX = event.getX();
                mTouchY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                if (mTouchX == DEFAULT_VALUE || mTouchY == DEFAULT_VALUE) {
                    break;
                }

                final float difX = mTouchX - event.getX();
                final float difY = mTouchY - event.getY();

                mXPos += difX;
                mYPos += difY;

                final int newX = Math.max(0, Math.min(mXPos, getMeasuredWidth() - mDisplayWidth));
                final int newY = Math.max(0, Math.min(mYPos, getMeasuredHeight() - mDisplayHeight +
                        mToolbarHeight + mStatusBarHeight + mNavBarHeight));

                if (getScrollX() != newX || getScrollY() != newY) {
                    scrollTo(newX, newY);
                    mTouchX = event.getX();
                    mTouchY = event.getY();
                }
                mXPos = newX;
                mYPos = newY;
                break;

            case MotionEvent.ACTION_UP:
                final int right = mXPos + mDisplayWidth;
                final int bottom = mYPos + mDisplayHeight;

                updateVisibleTiles(mXPos, mYPos, right, bottom);
                mTouchX = DEFAULT_VALUE;
                mTouchY = DEFAULT_VALUE;
                break;
        }
        return true;
    }

    public void renderTiles() {
        updateVisibleTiles(mXPos, mYPos, mDisplayWidth, mDisplayHeight);
    }

    public void setGridSize(final int gridSize) {
        mGridSize = gridSize;
    }

    public void setTilesManager(final TilesManager tilesManager) {
        mTilesManager = tilesManager;
    }

    private void updateVisibleTiles(final int left, final int top, final int right, final int bottom) {
        final int leftIndexX = left / mItemSize;
        final int rightIndexX = right / mItemSize;
        final int topIndexY = top / mItemSize;
        final int bottomIndexY = bottom / mItemSize;

        if (mLastBottomIndexY == bottomIndexY && mLastLeftIndexX == leftIndexX &&
                mLastRightIndexX == rightIndexX && mLastTopIndexY == topIndexY) {
            // skip
            Log.d(TAG, "Skip tiles update");
            return;
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Visible tiles x = [" + leftIndexX + ", " + rightIndexX + "]");
            Log.d(TAG, "Visible tiles y = [" + topIndexY + ", " + bottomIndexY + "]");
        }

        for (int i = 0; i < mGridSize; i++) {
            for (int j = 0; j < mGridSize; j++) {
                final int index = getChildIndex(i, j);
                final ImageView child = (ImageView) getChildAt(index);

                final boolean isVisible = (i >= leftIndexX) && (i < rightIndexX + PRELOAD_OFFSET) &&
                        (j >= topIndexY) && (j < bottomIndexY + PRELOAD_OFFSET);

                child.setVisibility(isVisible ? VISIBLE : INVISIBLE);
                if (isVisible) {
                    mTilesManager.loadTile(i, j, child);
                }
            }
        }

        mLastLeftIndexX = leftIndexX;
        mLastRightIndexX = rightIndexX;
        mLastTopIndexY = topIndexY;
        mLastBottomIndexY = bottomIndexY;
    }

}
