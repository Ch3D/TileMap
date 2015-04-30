package com.android.ch3d.tilemap.model;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.android.ch3d.tilemap.R;
import com.android.ch3d.tilemap.provider.TilesProvider;
import com.android.ch3d.tilemap.util.ImageWorker;
import com.android.ch3d.tilemap.util.cache.ImageCacheBase;
import com.android.ch3d.tilemap.util.cache.ImageCacheSimple;
import com.android.ch3d.tilemap.util.downloader.ImageDownloader;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class TilesManager {

	private static final String IMAGE_CACHE_DIR = "images";

	private ImageWorker mImageDownloader;

	private final Context mContext;

	private final TilesProvider mTilesProvider;

	public TilesManager(FragmentActivity context, TilesProvider tilesProvider) {
		mContext = context;
		mTilesProvider = tilesProvider;
		initImageCache(context);
	}

	private void initImageCache(FragmentActivity context) {
		ImageCacheBase.ImageCacheParams cacheParams = new ImageCacheBase.ImageCacheParams(context, IMAGE_CACHE_DIR);
		cacheParams.setMemCacheSizePercent(0.25f);

		final DisplayMetrics displayMetrics = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

		final int defaultImageSize = mContext.getResources().getDimensionPixelSize(R.dimen.item_size);
		mImageDownloader = new ImageDownloader(context, defaultImageSize);
		mImageDownloader.addImageCache(ImageCacheSimple.getInstance(context.getSupportFragmentManager(), cacheParams, defaultImageSize));
	}

	public void loadTile(int x, int y, ImageView imageView) {
		mImageDownloader.loadImage(mTilesProvider.getTile(x, y).getImgUrl(), imageView);
	}

	public void onPause() {
		mImageDownloader.setPaused(true);
	}

	public void onResume() {
		mImageDownloader.setPaused(false);
	}
}