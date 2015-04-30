package com.android.ch3d.tilemap.util.cache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

/**
 * Created by Ch3D on 23.04.2015.
 */
public interface ImageCache {
	/**
	 * Adds a bitmap to both memory and disk cache.
	 *
	 * @param data  Unique identifier for the bitmap to store
	 * @param value The bitmap drawable to store
	 */
	void addBitmapToCache(String data, BitmapDrawable value);

	/**
	 * Clears both the memory and disk cache associated with this ImageCache object. Note that
	 * this includes disk access so this should not be executed on the main/UI thread.
	 */
	void clearCache();

	/**
	 * Get from disk cache.
	 *
	 * @param data Unique identifier for which item to get
	 * @return The bitmap if found in cache, null otherwise
	 */
	Bitmap getBitmapFromDiskCache(String data);

	/**
	 * Get from memory cache.
	 *
	 * @param data Unique identifier for which item to get
	 * @return The bitmap drawable if found in cache, null otherwise
	 */
	BitmapDrawable getBitmapFromMemCache(String data);

	/**
	 * @param options - BitmapFactory.Options with out* options populated
	 * @return Bitmap that case be used for inBitmap
	 */
	Bitmap getBitmapFromReusableSet(BitmapFactory.Options options);

	ImageCacheBase.ImageCacheParams getParams();
}
