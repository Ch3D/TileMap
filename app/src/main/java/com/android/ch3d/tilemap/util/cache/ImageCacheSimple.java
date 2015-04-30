package com.android.ch3d.tilemap.util.cache;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.util.Utils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;

import static com.android.ch3d.tilemap.util.ImageUtils.decodeSampledBitmapFromDescriptor;

/**
 * Created by Ch3D on 27.04.2015.
 */
public class ImageCacheSimple extends ImageCacheBase {

	public static ImageCacheSimple getInstance(FragmentManager fragmentManager, ImageCacheParams cacheParams, final int defaultImageSize) {
		final ImageCacheBase.RetainFragment mRetainFragment = findOrCreateRetainFragment(fragmentManager);
		ImageCacheSimple imageCache = (ImageCacheSimple) mRetainFragment.getObject();
		if(imageCache == null) {
			imageCache = new ImageCacheSimple(cacheParams, defaultImageSize);
			mRetainFragment.setObject(imageCache);
		}
		return imageCache;
	}

	private final Object mDiskCacheLock = new Object();

	private static final String TAG = ImageCacheSimple.class.getSimpleName();

	private static final boolean DEBUG = BuildConfig.DEBUG;

	private final int mImgSize;

	private ImageCacheSimple(ImageCacheBase.ImageCacheParams cacheParams, int imgSize) {
		mImgSize = imgSize;
		init(cacheParams);
	}

	@Override
	public void addBitmapToCache(final String data, final BitmapDrawable value) {
		if(mMemoryCache != null) {
			mMemoryCache.put(data, value);
		}

		synchronized(mDiskCacheLock) {
			final String key = hashKeyForDisk(data);
			final File file = getFileForKey(key);
			file.getParentFile().mkdirs();
			OutputStream out = null;
			try {
				out = new FileOutputStream(file);
				value.getBitmap().compress(mCacheParams.compressFormat, mCacheParams.compressQuality, out);
				out.close();
			} catch(final IOException e) {
				Log.e(TAG, "addBitmapToCache", e);
			} catch(Exception e) {
				Log.e(TAG, "addBitmapToCache", e);
			} finally {
				Utils.close(out);
			}
		}
	}

	@Override
	public void clearCache() {
		if(mMemoryCache != null) {
			mMemoryCache.evictAll();
			if(DEBUG) {
				Log.d(TAG, "Memory cache cleared");
			}
		}

		synchronized(mDiskCacheLock) {
			mCacheParams.diskCacheDir.delete();
		}
	}

	@Override
	public Bitmap getBitmapFromDiskCache(final String data) {
		final String key = hashKeyForDisk(data);
		Bitmap bitmap = null;

		synchronized(mDiskCacheLock) {
			InputStream inputStream = null;
			try {
				if(DEBUG) {
					Log.d(TAG, "Disk cache hit");
				}

				final File fileForKey = getFileForKey(key);
				if(!fileForKey.exists()) {
					return null;
				}

				inputStream = new FileInputStream(fileForKey);
				if(inputStream != null) {
					FileDescriptor fd = ((FileInputStream) inputStream).getFD();
					// provide MAX_VALUE to skip sampling
					bitmap = decodeSampledBitmapFromDescriptor(fd, mImgSize, mImgSize, this);
				}
			} catch(final IOException e) {
				Log.e(TAG, "getBitmapFromDiskCache - " + e);
			} finally {
				Utils.close(inputStream);
			}
			return bitmap;
		}
	}

	private File getFileForKey(final String key) {
		return new File(mCacheParams.diskCacheDir + File.separator + key);
	}

	private void init(final ImageCacheBase.ImageCacheParams cacheParams) {
		mCacheParams = cacheParams;

		mCacheParams.diskCacheDir.mkdirs();

		if(mCacheParams.memoryCacheEnabled) {
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "Memory cache created (size = " + mCacheParams.memCacheSize + ")");
			}
			if(Utils.hasHoneycomb()) {
				mReusableBitmaps = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
			}
			mMemoryCache = new LruCache<String, BitmapDrawable>(mCacheParams.memCacheSize) {
				@Override
				protected void entryRemoved(boolean evicted, String key,
				                            BitmapDrawable oldValue, BitmapDrawable newValue) {
					mReusableBitmaps.add(new SoftReference<Bitmap>(oldValue.getBitmap()));
				}

				@Override
				protected int sizeOf(String key, BitmapDrawable value) {
					final int bitmapSize = getBitmapSize(value) / 1024;
					return bitmapSize == 0 ? 1 : bitmapSize;
				}
			};
		}
	}
}