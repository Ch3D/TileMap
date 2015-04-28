package com.android.ch3d.tilemap.util.downloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.ch3d.tilemap.BuildConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.android.ch3d.tilemap.util.Utils.close;
import static com.android.ch3d.tilemap.util.cache.ImageCacheBase.hashKeyForDisk;

/**
 * Created by Ch3D on 24.04.2015.
 */
public class ImageDownloaderSimple extends ImageDownloaderBase {
	private static final String TAG = ImageDownloaderAdvanced.class.getSimpleName();

	public ImageDownloaderSimple(Context context, int imageWidth, int imageHeight) {
		super(context, imageWidth, imageHeight);
		init(context);
	}

	public ImageDownloaderSimple(Context context, int imageSize) {
		super(context, imageSize);
		init(context);
	}

	@Override
	protected void closeCacheInternal() {
		super.closeCacheInternal();
		synchronized(mDiskCacheLock) {
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "HTTP cache closed");
			}
		}
	}

	@Override
	protected void flushCacheInternal() {
		super.flushCacheInternal();
		synchronized(mDiskCacheLock) {
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "HTTP cache flushed");
			}
		}
	}

	private File getFileForKey(final String key) {
		return new File(getImageCache().getParams().diskCacheDir + File.separator + key);
	}

	@Override
	protected void init(Context context) {
		checkConnection(context);
	}

	@Override
	protected Bitmap processBitmap(String data) {
		if(BuildConfig.DEBUG) {
			Log.d(TAG, "processing bitmap = " + data);
		}
		synchronized(mDiskCacheLock) {
			final String key = hashKeyForDisk(data);
			FileOutputStream outputStream = null;
			FileInputStream inputStream = null;
			try {
				final File file = getFileForKey(key);
				file.getParentFile().mkdirs();
				outputStream = new FileOutputStream(file);
				if(downloadUrlToStream(data, outputStream)) {
					inputStream = new FileInputStream(file);
					return decodeSampledBitmapFromDescriptor(inputStream.getFD(), mImageWidth, mImageHeight, getImageCache());
				}
			} catch(FileNotFoundException e) {
				Log.e(TAG, "processBitmap", e);
			} catch(IOException e) {
				Log.e(TAG, "processBitmap", e);
			} finally {
				close(outputStream);
				close(inputStream);
			}
			return null;
		}
	}
}
