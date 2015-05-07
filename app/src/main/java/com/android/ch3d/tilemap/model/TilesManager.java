package com.android.ch3d.tilemap.model;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.R;
import com.android.ch3d.tilemap.provider.TilesProvider;
import com.android.ch3d.tilemap.util.ImageWorker;
import com.android.ch3d.tilemap.util.cache.ImageCacheSimple;
import com.android.ch3d.tilemap.util.downloader.ImageDownloader;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class TilesManager {

    private static final String TAG = TilesManager.class.getSimpleName();

    private ImageWorker mImageDownloader;

    private final Context mContext;

    private final TilesProvider mTilesProvider;

    public TilesManager(FragmentActivity context, TilesProvider tilesProvider) {
        mContext = context;
        mTilesProvider = tilesProvider;
        initImageCache(context);
    }

    private void initImageCache(FragmentActivity context) {
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        final int defaultImageSize = mContext.getResources().getDimensionPixelSize(R.dimen.item_size);
        mImageDownloader = new ImageDownloader(context, defaultImageSize);
        mImageDownloader.addImageCache(ImageCacheSimple.getInstance(context, context.getSupportFragmentManager(), defaultImageSize));
    }

    public void loadTile(int x, int y, ImageView imageView) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Trying to load tile for [" + x + ", " + y + "]");
        }
        mImageDownloader.loadImage(mTilesProvider.getTile(x, y).getImgUrl(), imageView);
    }

    public void onPause() {
        mImageDownloader.setPaused(true);
    }

    public void onResume() {
        mImageDownloader.setPaused(false);
    }
}