package com.android.ch3d.tilemap.util.cache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import java.io.File;

/**
 * Created by Ch3D on 23.04.2015.
 */
public interface ImageCache {
	void addBitmapToCache(String data, BitmapDrawable value);

	void clearCache();

	Bitmap getBitmapFromDiskCache(String data);

	BitmapDrawable getBitmapFromMemCache(String data);

	Bitmap getBitmapFromReusableSet(BitmapFactory.Options options);

	File getDiskCacheDir();
}
