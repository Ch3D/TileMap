package com.android.ch3d.tilemap.util.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.util.ImageUtils;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Ch3D on 23.04.2015.
 */
public abstract class ImageCacheBase implements ImageCache {

	public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;

	public static final int DEFAULT_COMPRESS_QUALITY = 70;

	public static class RetainFragment extends Fragment {
		private Object mObject;

		public RetainFragment() {}

		public Object getObject() {
			return mObject;
		}

		public void setObject(Object object) {
			mObject = object;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}
	}

	static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
		RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(TAG);
		if(mRetainFragment == null) {
			mRetainFragment = new RetainFragment();
			fm.beginTransaction().add(mRetainFragment, TAG).commitAllowingStateLoss();
		}
		return mRetainFragment;
	}

	public static File getExternalCacheDir(Context context) {
		return context.getExternalCacheDir();
	}

	private static final String TAG = ImageCacheBase.class.getSimpleName();

	protected LruCache<String, BitmapDrawable> mMemoryCache;

	protected Set<SoftReference<Bitmap>> mReusableBitmaps;

	@Override
	public BitmapDrawable getBitmapFromMemCache(final String data) {
		BitmapDrawable result = null;
		if(mMemoryCache != null) {
			result = mMemoryCache.get(data);
		}
		if(BuildConfig.DEBUG && result != null) {
			Log.d(TAG, "Use memory cache - " + data);
		}
		return result;
	}

	@Override
	public Bitmap getBitmapFromReusableSet(final BitmapFactory.Options options) {
		Bitmap bitmap = null;

		if(mReusableBitmaps != null && !mReusableBitmaps.isEmpty()) {
			synchronized(mReusableBitmaps) {
				final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps.iterator();
				Bitmap item;

				while(iterator.hasNext()) {
					item = iterator.next().get();
					if(null != item && item.isMutable()) {
						if(ImageUtils.canUseForInBitmap(item, options)) {
							bitmap = item;
							iterator.remove();
							break;
						}
					} else {
						iterator.remove();
					}
				}
			}
		}

		return bitmap;
	}
}
