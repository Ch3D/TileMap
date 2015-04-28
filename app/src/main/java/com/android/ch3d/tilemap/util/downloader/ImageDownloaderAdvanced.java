package com.android.ch3d.tilemap.util.downloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.util.DiskLruCache;
import com.android.ch3d.tilemap.util.ImageResizer;
import com.android.ch3d.tilemap.util.cache.ImageCacheBase;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

import static com.android.ch3d.tilemap.util.Utils.close;

/**
 * Created by Ch3D on 24.04.2015.
 */
public class ImageDownloaderAdvanced extends ImageDownloaderBase {
	private static final String TAG = ImageDownloaderAdvanced.class.getSimpleName();

	private static final int HTTP_CACHE_SIZE = 10 * 1024 * 1024; // 10MB

	private static final String HTTP_CACHE_DIR = "http";

	private DiskLruCache mHttpDiskCache;

	private File mHttpCacheDir;

	private boolean mHttpDiskCacheStarting = true;

	private static final int DISK_CACHE_INDEX = 0;

	public ImageDownloaderAdvanced(final Context context, final int imageWidth, final int imageHeight) {
		super(context, imageWidth, imageHeight);
	}

	public ImageDownloaderAdvanced(final Context context, final int imageSize) {
		super(context, imageSize);
	}

	@Override
	protected void clearCacheInternal() {
		super.clearCacheInternal();
		synchronized(mDiskCacheLock) {
			if(mHttpDiskCache != null && !mHttpDiskCache.isClosed()) {
				try {
					mHttpDiskCache.delete();
					if(BuildConfig.DEBUG) {
						Log.d(TAG, "HTTP cache cleared");
					}
				} catch(IOException e) {
					Log.e(TAG, "clearCacheInternal", e);
				}
				mHttpDiskCache = null;
				mHttpDiskCacheStarting = true;
				initHttpDiskCache();
			}
		}
	}

	@Override
	protected void closeCacheInternal() {
		super.closeCacheInternal();
		synchronized(mDiskCacheLock) {
			if(mHttpDiskCache != null) {
				try {
					if(!mHttpDiskCache.isClosed()) {
						mHttpDiskCache.close();
						mHttpDiskCache = null;
						if(BuildConfig.DEBUG) {
							Log.d(TAG, "HTTP cache closed");
						}
					}
				} catch(IOException e) {
					Log.e(TAG, "closeCacheInternal", e);
				}
			}
		}
	}

	@Override
	protected void flushCacheInternal() {
		super.flushCacheInternal();
		synchronized(mDiskCacheLock) {
			if(mHttpDiskCache != null) {
				try {
					mHttpDiskCache.flush();
					if(BuildConfig.DEBUG) {
						Log.d(TAG, "HTTP cache flushed");
					}
				} catch(IOException e) {
					Log.e(TAG, "flushCacheInternal", e);
				}
			}
		}
	}

	@Override
	protected void init(Context context) {
		checkConnection(context);
		mHttpCacheDir = ImageCacheBase.getDiskCacheDir(context, HTTP_CACHE_DIR);
	}

	@Override
	protected void initDiskCacheInternal() {
		super.initDiskCacheInternal();
		initHttpDiskCache();
	}

	private void initHttpDiskCache() {
		if(!mHttpCacheDir.exists()) {
			mHttpCacheDir.mkdirs();
		}
		synchronized(mDiskCacheLock) {
			if(ImageCacheBase.getUsableSpace(mHttpCacheDir) > HTTP_CACHE_SIZE) {
				try {
					mHttpDiskCache = DiskLruCache.open(mHttpCacheDir, 1, 1, HTTP_CACHE_SIZE);
					if(BuildConfig.DEBUG) {
						Log.d(TAG, "HTTP cache initialized");
					}
				} catch(IOException e) {
					mHttpDiskCache = null;
				}
			}
			mHttpDiskCacheStarting = false;
			mDiskCacheLock.notifyAll();
		}
	}

	@Override
	protected Bitmap processBitmap(String data) {
		if(BuildConfig.DEBUG) {
			Log.d(TAG, "processing bitmap = " + data);
		}

		final String key = ImageCacheBase.hashKeyForDisk(data);
		FileDescriptor fileDescriptor = null;
		FileInputStream fileInputStream = null;
		DiskLruCache.Snapshot snapshot;
		synchronized(mDiskCacheLock) {
			while(mHttpDiskCacheStarting) {
				try {
					mDiskCacheLock.wait();
				} catch(InterruptedException e) {
					Log.e(TAG, "processBitmap", e);
				}
			}

			if(mHttpDiskCache != null) {
				try {
					snapshot = mHttpDiskCache.get(key);
					if(snapshot == null) {
						if(BuildConfig.DEBUG) {
							Log.d(TAG, "processBitmap, not found in http cache, downloading...");
						}
						DiskLruCache.Editor editor = mHttpDiskCache.edit(key);
						if(editor != null) {
							if(downloadUrlToStream(data, editor.newOutputStream(DISK_CACHE_INDEX))) {
								editor.commit();
							} else {
								editor.abort();
							}
						}
						snapshot = mHttpDiskCache.get(key);
					}
					if(snapshot != null) {
						fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
						fileDescriptor = fileInputStream.getFD();
					}
				} catch(IOException e) {
					Log.e(TAG, "processBitmap", e);
				} catch(IllegalStateException e) {
					Log.e(TAG, "processBitmap", e);
				} finally {
					if(fileDescriptor == null && fileInputStream != null) {
						close(fileInputStream);
					}
				}
			}
		}

		Bitmap bitmap = null;
		if(fileDescriptor != null) {
			bitmap = ImageResizer.decodeSampledBitmapFromDescriptor(fileDescriptor, mImageWidth, mImageHeight, getImageCache());
		}
		close(fileInputStream);
		return bitmap;
	}

}
