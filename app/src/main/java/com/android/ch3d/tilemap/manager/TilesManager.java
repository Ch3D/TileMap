package com.android.ch3d.tilemap.manager;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.android.ch3d.tilemap.provider.TilesProvider;
import com.android.ch3d.tilemap.util.ImageCache;
import com.android.ch3d.tilemap.util.ImageFetcher;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class TilesManager {

	private static final String IMAGE_CACHE_DIR = "images";

	private ImageFetcher mImageFetcher;

	private final Context mContext;

	private final TilesProvider mTilesProvider;

	public TilesManager(FragmentActivity context, TilesProvider tilesProvider) {
		mContext = context;
		mTilesProvider = tilesProvider;
		initImageCache(context);
	}

	private void initImageCache(FragmentActivity context) {
		ImageCache.ImageCacheParams cacheParams =
				new ImageCache.ImageCacheParams(context, IMAGE_CACHE_DIR);
		cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

		final DisplayMetrics displayMetrics = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		final int height = displayMetrics.heightPixels;
		final int width = displayMetrics.widthPixels;

		final int longest = (height > width ? height : width) / 2;

		mImageFetcher = new ImageFetcher(context, longest);
		mImageFetcher.addImageCache(context.getSupportFragmentManager(), cacheParams);
		mImageFetcher.setImageFadeIn(true);

	}

	public void loadTile(int x, int y, ImageView imageView) {
		mImageFetcher.loadImage(mTilesProvider.getTile(x, y).getImgUrl(), imageView);
	}

	public void loadTile(int x, int y) {
		loadTile(x, y, null);
	}

	public void setExitTasksEarly(final boolean exitTasksEarly) {
		mImageFetcher.setExitTasksEarly(exitTasksEarly);
	}

	public void flushCache() {
		mImageFetcher.flushCache();
	}

	public void closeCache() {
		mImageFetcher.closeCache();
	}
}