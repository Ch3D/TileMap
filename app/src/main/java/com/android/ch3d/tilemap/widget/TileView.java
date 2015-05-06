package com.android.ch3d.tilemap.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
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
public class TileView extends View {

    private static final String TAG = TileView.class.getSimpleName();
    private static final int DEFAULT_TILE_WIDTH = 256;

    private static final int DEFAULT_TILE_HEIGHT = DEFAULT_TILE_WIDTH;

    private static final int DEFAULT_COLUMNS_COUNT = 10;

    private static final int DEFAULT_ROWS_COUNT = DEFAULT_COLUMNS_COUNT;

    private int mTileWidth;

    private int mTileHeight;

    private int mColumnsCount;

    private int mRowsCount;

    private int mXPos = 0;

    private int mYPos = 0;

    private float mTouchX = -1;

    private float mTouchY = -1;

    private int mDisplayHeight;

    private int mDisplayWidth;

    private TilesManager mTilesManager;

    private int mLastLeftIndexX;

    private int mLastRightIndexX;

    private int mLastTopIndexY;

    private int mLastBottomIndexY;

    private int mToolbarHeight;

    private int mStatusBarHeight;

    private int mNavBarHeight;

    public TileView(final Context context) {
        this(context, null);
    }

    public TileView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TileView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TileView);

        if (ta != null) {
            mTileWidth = ta.getDimensionPixelSize(R.styleable.TileView_tileWidth, DEFAULT_TILE_WIDTH);
            mTileHeight = ta.getDimensionPixelSize(R.styleable.TileView_tileHeight, DEFAULT_TILE_HEIGHT);
            mColumnsCount = ta.getDimensionPixelSize(R.styleable.TileView_columnsCount, DEFAULT_COLUMNS_COUNT);
            mRowsCount = ta.getDimensionPixelSize(R.styleable.TileView_rowsCount, DEFAULT_ROWS_COUNT);
            ta.recycle();
        }

        init();
    }

    public void addView(final View child) {
        addView(child, new ViewGroup.LayoutParams(mTileWidth, mTileHeight));
    }

    private void addView(View child, ViewGroup.LayoutParams layoutParams) {

    }

    private int getChildIndex(final int i, final int j) {
        return (j * mColumnsCount) + i;
    }

    private void init() {
        setWillNotDraw(false);
        setScrollContainer(true);
        setVerticalScrollBarEnabled(true);
        setHorizontalScrollBarEnabled(true);

        mToolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_height);
        mStatusBarHeight = 0;
        mNavBarHeight = Utils.getNavigationBarHeight(getContext());

        final DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        mDisplayHeight = displayMetrics.heightPixels;
        mDisplayWidth = displayMetrics.widthPixels;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mTilesManager.drawBitmap(0,0, canvas);
        mTilesManager.drawBitmap(0,1, canvas);
        mTilesManager.drawBitmap(0,2, canvas);
        mTilesManager.drawBitmap(0,3, canvas);
        mTilesManager.drawBitmap(1,0, canvas);
        mTilesManager.drawBitmap(1,1, canvas);
        mTilesManager.drawBitmap(1,2, canvas);
        mTilesManager.drawBitmap(1,3, canvas);
    }

//    @Override
//    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
//        int count = mRowsCount * mColumnsCount;
//        int currentRow = 0;
//        int column = 0;
//        for (int i = 0; i < count; i++) {
//            View child = getChildAt(i);
//            if (child != null && child.getVisibility() != GONE) {
//                child.layout(column, currentRow * mTileHeight, child.getMeasuredWidth() + column,
//                        child.getMeasuredHeight() + (currentRow * mTileHeight));
//                column += mTileWidth;
//            }
//            if ((i % mColumnsCount) == mColumnsCount - 1) {
//                currentRow++;
//                column = 0;
//            }
//        }
//    }

    private View getChildAt(final int i) {
        return null;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        setMeasuredDimension(mTileWidth * mColumnsCount, mTileHeight * mRowsCount);

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child == null || child.getVisibility() == GONE) {
                continue;
            }

            child.measure(mTileWidth, mTileHeight);
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchX = event.getX();
                mTouchY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                if (mTouchX == -1 || mTouchY == -1) {
                    break;
                }

                final float difX = mTouchX - event.getX();
                final float difY = mTouchY - event.getY();

                mXPos += difX;
                mYPos += difY;

                final int newX = Math.max(0, Math.min(mXPos, getMeasuredWidth() - mDisplayWidth));
                final int newY = Math.max(0, Math.min(mYPos, getMeasuredHeight() - mDisplayHeight + mToolbarHeight + mStatusBarHeight
                        + mNavBarHeight));

                if (getScrollX() != newX || getScrollY() != newY) {
                    scrollTo(newX, newY);
                    mTouchX = event.getX();
                    mTouchY = event.getY();
                    invalidate();
                }
                mXPos = newX;
                mYPos = newY;
                break;

            case MotionEvent.ACTION_UP:
                final int right = mXPos + mDisplayWidth;
                final int bottom = mYPos + mDisplayHeight;

                updateVisibleTiles(mXPos, mYPos, right, bottom);
                mTouchX = -1;
                mTouchY = -1;
                break;
        }
        return true;
    }

    public void renderTiles() {
        updateVisibleTiles(mXPos, mYPos, mDisplayWidth, mDisplayHeight);
    }

    public void setTilesManager(final TilesManager tilesManager) {
        mTilesManager = tilesManager;
    }

    private void updateVisibleTiles(final int left, final int top, final int right, final int bottom) {
        final int leftIndexX = left / mTileWidth;
        final int rightIndexX = right / mTileWidth;
        final int topIndexY = top / mTileHeight;
        final int bottomIndexY = bottom / mTileHeight;

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

        for (int i = leftIndexX; i < (rightIndexX + 2); i++) {
            for (int j = topIndexY; j < (bottomIndexY + 2); j++) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Trying to load tile for [" + i + ", " + j + "]");
                }
                final int index = getChildIndex(i, j);
                if (index < getChildCount()) {
                    ImageView childAt = (ImageView) getChildAt(index);
                    if (childAt != null) {
                        // mTilesManager.loadTile(i, j, childAt);
                    }
                }
            }
        }

        mLastLeftIndexX = leftIndexX;
        mLastRightIndexX = rightIndexX;
        mLastTopIndexY = topIndexY;
        mLastBottomIndexY = bottomIndexY;
    }

    private int getChildCount() {
        return mRowsCount * mColumnsCount;
    }

}
