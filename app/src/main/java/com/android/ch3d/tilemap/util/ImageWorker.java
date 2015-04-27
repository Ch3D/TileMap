package com.android.ch3d.tilemap.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.util.cache.ImageCache;

import java.lang.ref.WeakReference;

public abstract class ImageWorker {

	public static void cancelWork(ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
		if(bitmapWorkerTask != null) {
			bitmapWorkerTask.cancel(true);
			if(BuildConfig.DEBUG) {
				final Object bitmapData = bitmapWorkerTask.mData;
				Log.d(TAG, "cancelWork - cancelled work for " + bitmapData);
			}
		}
	}

	public static boolean cancelPotentialWork(Object data, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if(bitmapWorkerTask != null) {
			final Object bitmapData = bitmapWorkerTask.mData;
			if(bitmapData == null || !bitmapData.equals(data)) {
				bitmapWorkerTask.cancel(true);
				if(BuildConfig.DEBUG) {
					Log.d(TAG, "cancelPotentialWork  for " + data);
				}
			} else {
				return false;
			}
		}
		return true;
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if(imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if(drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	private static final String TAG = ImageWorker.class.getSimpleName();

	private static final int FADE_IN_TIME = 200;

	private ImageCache mImageCache;

	private boolean mFadeInBitmap = true;

	private boolean mExitTasksEarly = false;

	protected boolean mPauseWork = false;

	private final Object mPauseWorkLock = new Object();

	protected Resources mResources;

	private static final int MESSAGE_CLEAR = 0;

	private static final int MESSAGE_INIT_DISK_CACHE = 1;

	private static final int MESSAGE_FLUSH = 2;

	private static final int MESSAGE_CLOSE = 3;

	protected ImageWorker(Context context) {
		mResources = context.getResources();
	}

	public void addImageCache(ImageCache cache) {
		mImageCache = cache;
		new CacheAsyncTask().execute(MESSAGE_INIT_DISK_CACHE);
	}

	public void clearCache() {
		new CacheAsyncTask().execute(MESSAGE_CLEAR);
	}

	protected void clearCacheInternal() {
		if(mImageCache != null) {
			mImageCache.clearCache();
		}
	}

	public void closeCache() {
		new CacheAsyncTask().execute(MESSAGE_CLOSE);
	}

	protected void closeCacheInternal() {
		if(mImageCache != null) {
			mImageCache.close();
			mImageCache = null;
		}
	}

	public void flushCache() {
		new CacheAsyncTask().execute(MESSAGE_FLUSH);
	}

	protected void flushCacheInternal() {
		if(mImageCache != null) {
			mImageCache.flush();
		}
	}

	protected ImageCache getImageCache() {
		return mImageCache;
	}

	protected void initDiskCacheInternal() {
		if(mImageCache != null) {
			mImageCache.initDiskCache();
		}
	}

	public void loadImage(Object data, ImageView imageView) {
		if(data == null) {
			return;
		}

		BitmapDrawable value = null;
		if(mImageCache != null) {
			value = mImageCache.getBitmapFromMemCache(String.valueOf(data));
		}

		if(value != null) {
			imageView.setImageDrawable(value);
		} else if(cancelPotentialWork(data, imageView)) {
			final BitmapWorkerTask task = new BitmapWorkerTask(data, imageView);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(mResources, null, task);
			imageView.setImageDrawable(asyncDrawable);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	protected abstract Bitmap processBitmap(Object data);

	public void setExitTasksEarly(boolean exitTasksEarly) {
		mExitTasksEarly = exitTasksEarly;
		setPauseWork(false);
	}

	private void setImageDrawable(ImageView imageView, Drawable drawable) {
		if(mFadeInBitmap) {
			final TransitionDrawable td =
					new TransitionDrawable(new Drawable[]{
							new ColorDrawable(android.R.color.transparent),
							drawable
					});
			imageView.setBackgroundDrawable(null);
			imageView.setImageDrawable(td);
			td.startTransition(FADE_IN_TIME);
		} else {
			imageView.setImageDrawable(drawable);
		}
	}

	public void setImageFadeIn(boolean fadeIn) {
		mFadeInBitmap = fadeIn;
	}

	public void setPauseWork(boolean pauseWork) {
		synchronized(mPauseWorkLock) {
			mPauseWork = pauseWork;
			if(!mPauseWork) {
				mPauseWorkLock.notifyAll();
			}
		}
	}

	private class BitmapWorkerTask extends AsyncTask<Void, Void, BitmapDrawable> {
		private Object mData;

		private final WeakReference<ImageView> imageViewReference;

		public BitmapWorkerTask(Object data, ImageView imageView) {
			mData = data;
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		@Override
		protected BitmapDrawable doInBackground(Void... params) {
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "doInBackground - starting work");
			}

			final String dataString = String.valueOf(mData);
			Bitmap bitmap = null;
			BitmapDrawable drawable = null;

			synchronized(mPauseWorkLock) {
				while(mPauseWork && !isCancelled()) {
					try {
						mPauseWorkLock.wait();
					} catch(InterruptedException e) {
					}
				}
			}

			if(mImageCache != null && !isCancelled() && getAttachedImageView() != null
					&& !mExitTasksEarly) {
				bitmap = mImageCache.getBitmapFromDiskCache(dataString);
			}

			if(bitmap == null && !isCancelled() && getAttachedImageView() != null
					&& !mExitTasksEarly) {
				bitmap = processBitmap(mData);
			}

			if(bitmap != null) {
				drawable = new BitmapDrawable(mResources, bitmap);
				if(mImageCache != null) {
					mImageCache.addBitmapToCache(dataString, drawable);
				}
			}

			if(BuildConfig.DEBUG) {
				Log.d(TAG, "doInBackground - finished work");
			}

			return drawable;
		}

		private ImageView getAttachedImageView() {
			final ImageView imageView = imageViewReference.get();
			final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

			if(this == bitmapWorkerTask) {
				return imageView;
			}

			return null;
		}

		@Override
		protected void onCancelled(BitmapDrawable value) {
			super.onCancelled(value);
			synchronized(mPauseWorkLock) {
				mPauseWorkLock.notifyAll();
			}
		}

		@Override
		protected void onPostExecute(BitmapDrawable value) {
			if(isCancelled() || mExitTasksEarly) {
				value = null;
			}

			final ImageView imageView = getAttachedImageView();
			if(value != null && imageView != null) {
				if(BuildConfig.DEBUG) {
					Log.d(TAG, "onPostExecute - setting bitmap");
				}
				setImageDrawable(imageView, value);
			}
		}
	}

	private static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	protected class CacheAsyncTask extends AsyncTask<Object, Void, Void> {

		@Override
		protected Void doInBackground(Object... params) {
			switch((Integer) params[0]) {
				case MESSAGE_CLEAR:
					clearCacheInternal();
					break;
				case MESSAGE_INIT_DISK_CACHE:
					initDiskCacheInternal();
					break;
				case MESSAGE_FLUSH:
					flushCacheInternal();
					break;
				case MESSAGE_CLOSE:
					closeCacheInternal();
					break;
			}
			return null;
		}
	}
}
