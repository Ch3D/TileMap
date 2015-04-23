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
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.util.DiskLruCache;
import com.android.ch3d.tilemap.util.ImageResizer;
import com.android.ch3d.tilemap.util.RecyclingBitmapDrawable;
import com.android.ch3d.tilemap.util.Utils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class handles disk and memory caching of bitmaps in conjunction with the
 * {@link com.android.ch3d.tilemap.util.ImageWorker} class and its subclasses. Use
 * {@link ImageCacheSimple#getInstance(android.support.v4.app.FragmentManager, ImageCacheParams)} to get an instance of this
 * class, although usually a cache should be added directly to an {@link com.android.ch3d.tilemap.util.ImageWorker} by calling
 * {@link com.android.ch3d.tilemap.util.ImageWorker#addImageCache(ImageCache)}.
 */
public class ImageCacheSimple extends ImageCacheBase {

	/**
	 * Return an {@link ImageCacheSimple} instance. A {@link RetainFragment} is used to retain the
	 * ImageCache object across configuration changes such as a change in device orientation.
	 *
	 * @param fragmentManager The fragment manager to use when dealing with the retained fragment.
	 * @param cacheParams     The cache parameters to use if the ImageCache needs instantiation.
	 * @return An existing retained ImageCache object or a new one if one did not exist
	 */
	public static ImageCacheSimple getInstance(FragmentManager fragmentManager, ImageCacheParams cacheParams) {

		// Search for, or create an instance of the non-UI RetainFragment
		final RetainFragment mRetainFragment = findOrCreateRetainFragment(fragmentManager);

		// See if we already have an ImageCache stored in RetainFragment
		ImageCacheSimple imageCache = (ImageCacheSimple) mRetainFragment.getObject();

		// No existing ImageCache, create one and store it in RetainFragment
		if(imageCache == null) {
			imageCache = new ImageCacheSimple(cacheParams);
			mRetainFragment.setObject(imageCache);
		}

		return imageCache;
	}

	private static final String TAG = "ImageCache";

	private static final int DISK_CACHE_INDEX = 0;

	private DiskLruCache mDiskLruCache;

	private LruCache<String, BitmapDrawable> mMemoryCache;

	private ImageCacheParams mCacheParams;

	private final Object mDiskCacheLock = new Object();

	private boolean mDiskCacheStarting = true;

	private Set<SoftReference<Bitmap>> mReusableBitmaps;

	/**
	 * Create a new ImageCache object using the specified parameters. This should not be
	 * called directly by other classes, instead use
	 * {@link ImageCacheSimple#getInstance(android.support.v4.app.FragmentManager, ImageCacheParams)} to fetch an ImageCache
	 * instance.
	 *
	 * @param cacheParams The cache parameters to use to initialize the cache
	 */
	private ImageCacheSimple(ImageCacheParams cacheParams) {
		init(cacheParams);
	}

	@Override
	public void addBitmapToCache(String data, BitmapDrawable value) {
		//BEGIN_INCLUDE(add_bitmap_to_cache)
		if(data == null || value == null) {
			return;
		}

		// Add to memory cache
		if(mMemoryCache != null) {
			if(RecyclingBitmapDrawable.class.isInstance(value)) {
				// The removed entry is a recycling drawable, so notify it
				// that it has been added into the memory cache
				((RecyclingBitmapDrawable) value).setIsCached(true);
			}
			mMemoryCache.put(data, value);
		}

		synchronized(mDiskCacheLock) {
			// Add to disk cache
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
		//END_INCLUDE(add_bitmap_to_cache)
	}

	@Override
	public void clearCache() {
		if(mMemoryCache != null) {
			mMemoryCache.evictAll();
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "Memory cache cleared");
			}
		}

		synchronized(mDiskCacheLock) {
			mDiskCacheStarting = true;
			if(mDiskLruCache != null && !mDiskLruCache.isClosed()) {
				try {
					mDiskLruCache.delete();
					if(BuildConfig.DEBUG) {
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
						if(BuildConfig.DEBUG) {
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
					if(BuildConfig.DEBUG) {
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
		//BEGIN_INCLUDE(get_bitmap_from_disk_cache)
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
						if(BuildConfig.DEBUG) {
							Log.d(TAG, "Disk cache hit");
						}
						inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
						if(inputStream != null) {
							FileDescriptor fd = ((FileInputStream) inputStream).getFD();

							// Decode bitmap, but we don't want to sample so give
							// MAX_VALUE as the target dimensions
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
		//END_INCLUDE(get_bitmap_from_disk_cache)
	}

	@Override
	public BitmapDrawable getBitmapFromMemCache(String data) {
		//BEGIN_INCLUDE(get_bitmap_from_mem_cache)
		BitmapDrawable memValue = null;

		if(mMemoryCache != null) {
			memValue = mMemoryCache.get(data);
		}

		if(BuildConfig.DEBUG && memValue != null) {
			Log.d(TAG, "Memory cache hit");
		}

		return memValue;
		//END_INCLUDE(get_bitmap_from_mem_cache)
	}

	@Override
	public Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
		//BEGIN_INCLUDE(get_bitmap_from_reusable_set)
		Bitmap bitmap = null;

		if(mReusableBitmaps != null && !mReusableBitmaps.isEmpty()) {
			synchronized(mReusableBitmaps) {
				final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps.iterator();
				Bitmap item;

				while(iterator.hasNext()) {
					item = iterator.next().get();

					if(null != item && item.isMutable()) {
						// Check to see it the item can be used for inBitmap
						if(canUseForInBitmap(item, options)) {
							bitmap = item;

							// Remove from reusable set so it can't be used again
							iterator.remove();
							break;
						}
					} else {
						// Remove from the set if the reference has been cleared.
						iterator.remove();
					}
				}
			}
		}

		return bitmap;
		//END_INCLUDE(get_bitmap_from_reusable_set)
	}

	/**
	 * Initialize the cache, providing all parameters.
	 *
	 * @param cacheParams The cache parameters to initialize the cache
	 */
	private void init(ImageCacheParams cacheParams) {
		mCacheParams = cacheParams;

		//BEGIN_INCLUDE(init_memory_cache)
		// Set up memory cache
		if(mCacheParams.memoryCacheEnabled) {
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "Memory cache created (size = " + mCacheParams.memCacheSize + ")");
			}

			// If we're running on Honeycomb or newer, create a set of reusable bitmaps that can be
			// populated into the inBitmap field of BitmapFactory.Options. Note that the set is
			// of SoftReferences which will actually not be very effective due to the garbage
			// collector being aggressive clearing Soft/WeakReferences. A better approach
			// would be to use a strongly references bitmaps, however this would require some
			// balancing of memory usage between this set and the bitmap LruCache. It would also
			// require knowledge of the expected size of the bitmaps. From Honeycomb to JellyBean
			// the size would need to be precise, from KitKat onward the size would just need to
			// be the upper bound (due to changes in how inBitmap can re-use bitmaps).
			if(Utils.hasHoneycomb()) {
				mReusableBitmaps =
						Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
			}

			mMemoryCache = new LruCache<String, BitmapDrawable>(mCacheParams.memCacheSize) {

				/**
				 * Notify the removed entry that is no longer being cached
				 */
				@Override
				protected void entryRemoved(boolean evicted, String key,
				                            BitmapDrawable oldValue, BitmapDrawable newValue) {
					if(RecyclingBitmapDrawable.class.isInstance(oldValue)) {
						// The removed entry is a recycling drawable, so notify it
						// that it has been removed from the memory cache
						((RecyclingBitmapDrawable) oldValue).setIsCached(false);
					} else {
						// The removed entry is a standard BitmapDrawable

						if(Utils.hasHoneycomb()) {
							// We're running on Honeycomb or later, so add the bitmap
							// to a SoftReference set for possible use with inBitmap later
							mReusableBitmaps.add(new SoftReference<Bitmap>(oldValue.getBitmap()));
						}
					}
				}

				/**
				 * Measure item size in kilobytes rather than units which is more practical
				 * for a bitmap cache
				 */
				@Override
				protected int sizeOf(String key, BitmapDrawable value) {
					final int bitmapSize = getBitmapSize(value) / 1024;
					return bitmapSize == 0 ? 1 : bitmapSize;
				}
			};
		}
		//END_INCLUDE(init_memory_cache)

		// By default the disk cache is not initialized here as it should be initialized
		// on a separate thread due to disk access.
		if(cacheParams.initDiskCacheOnCreate) {
			// Set up disk cache
			initDiskCache();
		}
	}

	@Override
	public void initDiskCache() {
		// Set up disk cache
		synchronized(mDiskCacheLock) {
			if(mDiskLruCache == null || mDiskLruCache.isClosed()) {
				File diskCacheDir = mCacheParams.diskCacheDir;
				if(mCacheParams.diskCacheEnabled && diskCacheDir != null) {
					if(!diskCacheDir.exists()) {
						diskCacheDir.mkdirs();
					}
					if(getUsableSpace(diskCacheDir) > mCacheParams.diskCacheSize) {
						try {
							mDiskLruCache = DiskLruCache.open(
									diskCacheDir, 1, 1, mCacheParams.diskCacheSize);
							if(BuildConfig.DEBUG) {
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
