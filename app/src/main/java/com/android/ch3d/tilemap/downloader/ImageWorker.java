package com.android.ch3d.tilemap.downloader;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.util.cache.ImageCache;

import java.lang.ref.WeakReference;

/**
 * Created by Ch3D on 23.04.2015.
 */
public abstract class ImageWorker {

	private static final String TAG = ImageWorker.class.getSimpleName();

	private ImageCache mImageCache;

	protected Context mContext;

	protected ImageWorker(Context context) {
		mContext = context;
	}

	public boolean cancelPotentialWork(Object data, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
		if(bitmapWorkerTask != null) {
			final String bitmapData = bitmapWorkerTask.mUrl;
			if(bitmapData == null || !bitmapData.equals(data)) {
				bitmapWorkerTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	private BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if(imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if(drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	protected ImageCache getImageCache() {
		return mImageCache;
	}

	public void setImageCache(ImageCache cache) {
		mImageCache = cache;
	}

	public void loadImage(final String url, final ImageView imageView) {
		if(url == null) {
			return;
		}

		BitmapDrawable value = null;
		if(mImageCache != null) {
			value = mImageCache.getBitmapFromMemCache(url);
		}

		if(value != null) {
			imageView.setImageDrawable(value);
		} else if(cancelPotentialWork(url, imageView)) {
			final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(), null, task);
			imageView.setImageDrawable(asyncDrawable);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
		}
	}

	protected abstract Bitmap processBitmap(final String url);

	private void setImageDrawable(ImageView imageView, Drawable drawable) {
		final TransitionDrawable td = new TransitionDrawable(new Drawable[]{
				new ColorDrawable(android.R.color.transparent),
				drawable
		});
		imageView.setImageDrawable(td);
		td.startTransition(mContext.getResources().getInteger(android.R.integer.config_shortAnimTime));
	}

	private class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {
		private final WeakReference<ImageView> imageViewReference;

		private String mUrl;

		public BitmapWorkerTask(ImageView imageView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		@Override
		protected BitmapDrawable doInBackground(String... params) {
			if(params.length == 0 || TextUtils.isEmpty(params[0])) {
				return null;
			}

			mUrl = String.valueOf(params[0]);
			Bitmap bitmap = null;
			BitmapDrawable drawable = null;

			if(mImageCache != null && !isCancelled() && getAttachedImageView() != null) {
				bitmap = mImageCache.getBitmapFromDiskCache(mUrl);
			}

			if(bitmap == null && !isCancelled() && getAttachedImageView() != null) {
				bitmap = processBitmap(mUrl);
			}

			if(bitmap != null) {
				drawable = new BitmapDrawable(mContext.getResources(), bitmap);
				if(mImageCache != null) {
					mImageCache.addBitmapToCache(mUrl, drawable);
				}
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
		protected void onPostExecute(BitmapDrawable value) {
			if(isCancelled()) {
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
}
