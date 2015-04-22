package com.android.ch3d.tilemap.manager;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.android.ch3d.tilemap.model.Tile;
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

		// Fetch screen height and width, to use as our max size when loading images as this
		// activity runs full screen
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		final int height = displayMetrics.heightPixels;
		final int width = displayMetrics.widthPixels;

		// For this sample we'll use half of the longest width to resize our images. As the
		// image scaling ensures the image is larger than this, we should be left with a
		// resolution that is appropriate for both portrait and landscape. For best image quality
		// we shouldn't divide by 2, but this will use more memory and require a larger memory
		// cache.
		final int longest = (height > width ? height : width) / 2;

		// The ImageFetcher takes care of loading images into our ImageView children asynchronously
		mImageFetcher = new ImageFetcher(context, longest);
		mImageFetcher.addImageCache(context.getSupportFragmentManager(), cacheParams);
		mImageFetcher.setImageFadeIn(true);

	}

	public void loadTile(Tile tile, ImageView imageView) {
		mImageFetcher.loadImage(tile.getImgUrl(), imageView);
	}

	public void loadTile(Tile tile) {
		loadTile(tile, null);
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