/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ch3d.tilemap.util.cache;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.util.DiskLruCache;
import com.android.ch3d.tilemap.util.ImageResizer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ch3D on 24.04.2015.
 */
public class ImageCacheAdvanced extends ImageCacheBase {

	public static final boolean DEBUG = BuildConfig.DEBUG;

	static ImageCacheAdvanced getInstance(FragmentManager fragmentManager, ImageCacheParams cacheParams) {
		final RetainFragment mRetainFragment = findOrCreateRetainFragment(fragmentManager);
		ImageCacheAdvanced imageCache = (ImageCacheAdvanced) mRetainFragment.getObject();
		if(imageCache == null) {
			imageCache = new ImageCacheAdvanced(cacheParams);
			mRetainFragment.setObject(imageCache);
		}
		return imageCache;
	}

	private static final String TAG = "ImageCacheSimple";

	private static final int DISK_CACHE_INDEX = 0;

	private DiskLruCache mDiskLruCache;

	private final Object mDiskCacheLock = new Object();

	private boolean mDiskCacheStarting = true;

	private Set<SoftReference<Bitmap>> mReusableBitmaps;

	private ImageCacheAdvanced(ImageCacheParams cacheParams) {
		init(cacheParams);
	}

	@Override
	public void addBitmapToCache(String data, BitmapDrawable value) {
		if(data == null || value == null) {
			return;
		}

		if(mMemoryCache != null) {
			mMemoryCache.put(data, value);
		}

		synchronized(mDiskCacheLock) {
			if(mDiskLruCache != null) {
				final String key = hashKeyForDisk(data);
				OutputStream out = null;
				try {
					DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
					if(snapshot == null) {
						final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
						if(editor != null) {
							out = editor.newOutputStream(DISK_CACHE_INDEX);
							value.getBitmap().compress(
									mCacheParams.compressFormat, mCacheParams.compressQuality, out);
							editor.commit();
							out.close();
						}
					} else {
						snapshot.getInputStream(DISK_CACHE_INDEX).close();
					}
				} catch(final IOException e) {
					Log.e(TAG, "addBitmapToCache - " + e);
				} catch(Exception e) {
					Log.e(TAG, "addBitmapToCache - " + e);
				} finally {
					try {
						if(out != null) {
							out.close();
						}
					} catch(IOException e) {
					}
				}
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
			mDiskCacheStarting = true;
			if(mDiskLruCache != null && !mDiskLruCache.isClosed()) {
				try {
					mDiskLruCache.delete();
					if(DEBUG) {
						Log.d(TAG, "Disk cache cleared");
					}
				} catch(IOException e) {
					Log.e(TAG, "clearCache - " + e);
				}
				mDiskLruCache = null;
				initDiskCache();
			}
		}
	}

	@Override
	public void close() {
		synchronized(mDiskCacheLock) {
			if(mDiskLruCache != null) {
				try {
					if(!mDiskLruCache.isClosed()) {
						mDiskLruCache.close();
						mDiskLruCache = null;
						if(DEBUG) {
							Log.d(TAG, "Disk cache closed");
						}
					}
				} catch(IOException e) {
					Log.e(TAG, "close - " + e);
				}
			}
		}
	}

	@Override
	public void flush() {
		synchronized(mDiskCacheLock) {
			if(mDiskLruCache != null) {
				try {
					mDiskLruCache.flush();
					if(DEBUG) {
						Log.d(TAG, "Disk cache flushed");
					}
				} catch(IOException e) {
					Log.e(TAG, "flush - " + e);
				}
			}
		}
	}

	@Override
	public Bitmap getBitmapFromDiskCache(String data) {
		final String key = hashKeyForDisk(data);
		Bitmap bitmap = null;

		synchronized(mDiskCacheLock) {
			while(mDiskCacheStarting) {
				try {
					mDiskCacheLock.wait();
				} catch(InterruptedException e) {
				}
			}
			if(mDiskLruCache != null) {
				InputStream inputStream = null;
				try {
					final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
					if(snapshot != null) {
						if(DEBUG) {
							Log.d(TAG, "Disk cache hit");
						}
						inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
						if(inputStream != null) {
							FileDescriptor fd = ((FileInputStream) inputStream).getFD();

							// provide MAX_VALUE to skip sampling
							bitmap = ImageResizer.decodeSampledBitmapFromDescriptor(
									fd, Integer.MAX_VALUE, Integer.MAX_VALUE, this);
						}
					}
				} catch(final IOException e) {
					Log.e(TAG, "getBitmapFromDiskCache - " + e);
				} finally {
					try {
						if(inputStream != null) {
							inputStream.close();
						}
					} catch(IOException e) {
					}
				}
			}
			return bitmap;
		}
	}

	private void init(ImageCacheParams cacheParams) {
		mCacheParams = cacheParams;

		if(mCacheParams.memoryCacheEnabled) {
			if(DEBUG) {
				Log.d(TAG, "Memory cache created (size = " + mCacheParams.memCacheSize + ")");
			}
			mReusableBitmaps = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
			mMemoryCache = new LruCache<String, BitmapDrawable>(mCacheParams.memCacheSize) {
				@Override
				protected void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue) {
					mReusableBitmaps.add(new SoftReference<Bitmap>(oldValue.getBitmap()));
				}

				@Override
				protected int sizeOf(String key, BitmapDrawable value) {
					final int bitmapSize = getBitmapSize(value) / 1024;
					return bitmapSize == 0 ? 1 : bitmapSize;
				}
			};
		}
		if(cacheParams.initDiskCacheOnCreate) {
			initDiskCache();
		}
	}

	@Override
	public void initDiskCache() {
		synchronized(mDiskCacheLock) {
			if(mDiskLruCache == null || mDiskLruCache.isClosed()) {
				File diskCacheDir = mCacheParams.diskCacheDir;
				if(mCacheParams.diskCacheEnabled && diskCacheDir != null) {
					if(!diskCacheDir.exists()) {
						diskCacheDir.mkdirs();
					}
					if(getUsableSpace(diskCacheDir) > mCacheParams.diskCacheSize) {
						try {
							mDiskLruCache = DiskLruCache.open(diskCacheDir, BuildConfig.VERSION_CODE, 1, mCacheParams.diskCacheSize);
							if(DEBUG) {
								Log.d(TAG, "Disk cache initialized");
							}
						} catch(final IOException e) {
							mCacheParams.diskCacheDir = null;
							Log.e(TAG, "initDiskCache - " + e);
						}
					}
				}
			}
			mDiskCacheStarting = false;
			mDiskCacheLock.notifyAll();
		}
	}

}
