package com.android.ch3d.tilemap.model;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.android.ch3d.tilemap.provider.TilesProvider;
import com.android.ch3d.tilemap.util.ImageDownloader;
import com.android.ch3d.tilemap.util.ImageWorker;
import com.android.ch3d.tilemap.util.cache.ImageCacheBase;
import com.android.ch3d.tilemap.util.cache.ImageCacheFactory;

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

	public void closeCache() {
		mImageDownloader.closeCache();
	}

	public void flushCache() {
		mImageDownloader.flushCache();
	}

	private void initImageCache(FragmentActivity context) {
		ImageCacheBase.ImageCacheParams cacheParams =
				new ImageCacheBase.ImageCacheParams(context, IMAGE_CACHE_DIR);
		cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

		final DisplayMetrics displayMetrics = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		final int height = displayMetrics.heightPixels;
		final int width = displayMetrics.widthPixels;

		final int longest = (height > width ? height : width) / 2;

		mImageDownloader = new ImageDownloader(context, longest);
		mImageDownloader.addImageCache(ImageCacheFactory.getInstance(ImageCacheBase.CACHE_TYPE_SIMPLE,
		                                                             context.getSupportFragmentManager(),
		                                                             cacheParams));
		mImageDownloader.setImageFadeIn(true);

	}

	public void loadTile(int x, int y, ImageView imageView) {
		mImageDownloader.loadImage(mTilesProvider.getTile(x, y).getImgUrl(), imageView);
	}

	public void setExitTasksEarly(final boolean exitTasksEarly) {
		mImageDownloader.setExitTasksEarly(exitTasksEarly);
	}
}