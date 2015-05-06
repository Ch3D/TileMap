package com.android.ch3d.tilemap.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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

    private boolean mPaused = false;

    protected boolean mPauseWork = false;

    private final Object mPauseWorkLock = new Object();

    protected Context mContext;

    protected ImageWorker(Context context) {
        mContext = context;
    }

    public void addImageCache(ImageCache cache) {
        mImageCache = cache;
    }

    public boolean cancelPotentialWork(Object data, Holder holder) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(holder);
        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.mUrl;
            if (bitmapData == null || !bitmapData.equals(data)) {
                bitmapWorkerTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    private BitmapWorkerTask getBitmapWorkerTask(Holder holder) {
        if (holder != null) {
            final Drawable drawable = holder.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    protected ImageCache getImageCache() {
        return mImageCache;
    }

    public void loadImage(final String url, final Holder holder) {
        if (url == null) {
            return;
        }

        BitmapDrawable value = null;
        if (mImageCache != null) {
            value = mImageCache.getBitmapFromMemCache(url);
        }

        if (value != null) {
            holder.getCanvas().drawBitmap(value.getBitmap(), holder.getX() * 256, holder.getY() * 256, null);
        } else if (cancelPotentialWork(url, holder)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(holder);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(), null, task);
            holder.setDrawable(asyncDrawable);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
        }
    }

//    public void loadImage(final String url, final ImageView imageView) {
//        if (url == null) {
//            return;
//        }
//
//        BitmapDrawable value = null;
//        if (mImageCache != null) {
//            value = mImageCache.getBitmapFromMemCache(url);
//        }
//
//        if (value != null) {
//            imageView.setImageDrawable(value);
//        } else if (cancelPotentialWork(url, imageView)) {
//            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
//            final AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(), null, task);
//            imageView.setImageDrawable(asyncDrawable);
//            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
//        }
//    }

    protected abstract Bitmap processBitmap(final String url);

    private void setImageDrawable(ImageView imageView, Drawable drawable) {
        final TransitionDrawable td = new TransitionDrawable(new Drawable[]{
                new ColorDrawable(android.R.color.transparent),
                drawable
        });
        imageView.setImageDrawable(td);
        td.startTransition(mContext.getResources().getInteger(android.R.integer.config_shortAnimTime));
    }

    public void setPauseWork(boolean pauseWork) {
        synchronized (mPauseWorkLock) {
            mPauseWork = pauseWork;
            if (!mPauseWork) {
                mPauseWorkLock.notifyAll();
            }
        }
    }

    public void setPaused(boolean paused) {
        mPaused = paused;
        setPauseWork(false);
    }

    public static class Holder {
        private Drawable drawable;
        private Canvas canvas;
        private int x;

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        private int y;

        public Holder(Canvas canvas, int x, int y) {
            this.canvas = canvas;
            this.x = x;
            this.y = y;
        }

        public Drawable getDrawable() {
            return drawable;
        }

        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }

        public Canvas getCanvas() {
            return canvas;
        }
    }

    private class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {
        private final WeakReference<Holder> imageViewReference;

        private String mUrl;

        public BitmapWorkerTask(Holder holder) {
            imageViewReference = new WeakReference<Holder>(holder);
        }

        @Override
        protected BitmapDrawable doInBackground(String... params) {
            if (params.length == 0 || TextUtils.isEmpty(params[0])) {
                return null;
            }

            mUrl = String.valueOf(params[0]);
            Bitmap bitmap = null;
            BitmapDrawable drawable = null;

            synchronized (mPauseWorkLock) {
                while (mPauseWork && !isCancelled()) {
                    try {
                        mPauseWorkLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }

            if (mImageCache != null && !isCancelled() && getAttachedHolder() != null && !mPaused) {
                bitmap = mImageCache.getBitmapFromDiskCache(mUrl);
            }

            if (bitmap == null && !isCancelled() && getAttachedHolder() != null && !mPaused) {
                bitmap = processBitmap(mUrl);
            }

            if (bitmap != null) {
                drawable = new BitmapDrawable(mContext.getResources(), bitmap);
                if (mImageCache != null) {
                    mImageCache.addBitmapToCache(mUrl, drawable);
                }
            }
            return drawable;
        }

        private Holder getAttachedHolder() {
            final Holder holder = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(holder);

            if (this == bitmapWorkerTask) {
                return holder;
            }
            return null;
        }

        @Override
        protected void onCancelled(final BitmapDrawable value) {
            super.onCancelled(value);
            synchronized (mPauseWorkLock) {
                mPauseWorkLock.notifyAll();
            }
        }

        @Override
        protected void onPostExecute(BitmapDrawable value) {
            if (isCancelled() || mPaused) {
                value = null;
            }

            final Holder holder = getAttachedHolder();
            if (value != null && holder != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onPostExecute - setting bitmap");
                }
                holder.setDrawable(value);
                holder.getCanvas().drawBitmap(value.getBitmap(), holder.getX() * 256, holder.getY() * 256, null);
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
