package com.android.ch3d.tilemap.util.cache;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.util.Utils;

import java.io.File;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Ch3D on 23.04.2015.
 */
public abstract class ImageCacheBase implements ImageCache {

	public static final int CACHE_TYPE_SIMPLE = 1;

	public static final int CACHE_TYPE_ADVANCED = 2;

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

	public static class ImageCacheParams {
		public File diskCacheDir;

		public ImageCacheParams(Context context, String diskCacheDirectoryName) {
			diskCacheDir = getDiskCacheDir(context, diskCacheDirectoryName);
		}

		public void setMemCacheSizePercent(float percent) {
			if(percent < 0.01f || percent > 0.8f) {
				throw new IllegalArgumentException("setMemCacheSizePercent - percent must be between 0.01 and 0.8 (inclusive)");
			}
			memCacheSize = Math.round(percent * Runtime.getRuntime().maxMemory() / 1024);
		}

		public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;

		public int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;

		public Bitmap.CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;

		public int compressQuality = DEFAULT_COMPRESS_QUALITY;

		public boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;

		public boolean diskCacheEnabled = DEFAULT_DISK_CACHE_ENABLED;

		public boolean initDiskCacheOnCreate = DEFAULT_INIT_DISK_CACHE_ON_CREATE;

	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	static boolean canUseForInBitmap(Bitmap candidate, BitmapFactory.Options targetOptions) {
		if(!Utils.hasKitKat()) {
			return candidate.getWidth() == targetOptions.outWidth
					&& candidate.getHeight() == targetOptions.outHeight
					&& targetOptions.inSampleSize == 1;
		}

		int width = targetOptions.outWidth / targetOptions.inSampleSize;
		int height = targetOptions.outHeight / targetOptions.inSampleSize;
		int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
		return byteCount <= candidate.getAllocationByteCount();
	}

	static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
		RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(TAG);
		if(mRetainFragment == null) {
			mRetainFragment = new RetainFragment();
			fm.beginTransaction().add(mRetainFragment, TAG).commitAllowingStateLoss();
		}
		return mRetainFragment;
	}

	private static int getBytesPerPixel(Bitmap.Config config) {
		if(config == Bitmap.Config.ARGB_8888) {
			return 4;
		} else if(config == Bitmap.Config.RGB_565) {
			return 2;
		} else if(config == Bitmap.Config.ARGB_4444) {
			return 2;
		} else if(config == Bitmap.Config.ALPHA_8) {
			return 1;
		}
		return 1;
	}

	public static File getDiskCacheDir(Context context, String uniqueName) {
		final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
				!isExternalStorageRemovable() ? getExternalCacheDir(context).getPath() :
				context.getCacheDir().getPath();

		return new File(cachePath + File.separator + uniqueName);
	}

	public static String hashKeyForDisk(String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch(NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	private static String bytesToHexString(byte[] bytes) {
		// http://stackoverflow.com/questions/332079
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if(hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static int getBitmapSize(BitmapDrawable value) {
		Bitmap bitmap = value.getBitmap();
		if(Utils.hasKitKat()) {
			return bitmap.getAllocationByteCount();
		}
		return bitmap.getByteCount();
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static boolean isExternalStorageRemovable() {
		if(Utils.hasGingerbread()) {
			return Environment.isExternalStorageRemovable();
		}
		return true;
	}

	public static File getExternalCacheDir(Context context) {
		return context.getExternalCacheDir();
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static long getUsableSpace(File path) {
		if(Utils.hasGingerbread()) {
			return path.getUsableSpace();
		}
		final StatFs stats = new StatFs(path.getPath());
		return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
	}

	private static final String TAG = "ImageCacheBase";

	private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;

	private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;

	private static final boolean DEFAULT_INIT_DISK_CACHE_ON_CREATE = true;

	private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 8; // 5MB

	private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB

	private static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;

	private static final int DEFAULT_COMPRESS_QUALITY = 70;

	protected LruCache<String, BitmapDrawable> mMemoryCache;

	protected Set<SoftReference<Bitmap>> mReusableBitmaps;

	protected ImageCacheBase.ImageCacheParams mCacheParams;

	@Override
	public BitmapDrawable getBitmapFromMemCache(final String data) {
		BitmapDrawable memValue = null;
		if(mMemoryCache != null) {
			memValue = mMemoryCache.get(data);
		}
		if(BuildConfig.DEBUG && memValue != null) {
			Log.d(TAG, "Memory cache hit");
		}
		return memValue;
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
						if(canUseForInBitmap(item, options)) {
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

	@Override
	public ImageCacheParams getParams() {
		return mCacheParams;
	}
}
