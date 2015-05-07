package com.android.ch3d.tilemap.util.cache;

import android.content.Context;
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

    public static final String CACHE_DIR = "images";

    private File diskCacheDir;

    public static ImageCacheSimple getInstance(Context context, FragmentManager fragmentManager, final int defaultImageSize) {
        final ImageCacheBase.RetainFragment mRetainFragment = findOrCreateRetainFragment(fragmentManager);
        ImageCacheSimple imageCache = (ImageCacheSimple) mRetainFragment.getObject();
        if (imageCache == null) {
            imageCache = new ImageCacheSimple(context, defaultImageSize);
            mRetainFragment.setObject(imageCache);
        }
        return imageCache;
    }

    private final Object mDiskCacheLock = new Object();

    private static final String TAG = ImageCacheSimple.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private final Context mContext;
    private final int mImgSize;

    private ImageCacheSimple(Context context, int imgSize) {
        this.mContext = context;
        mImgSize = imgSize;
        init();
    }

    @Override
    public void addBitmapToCache(final String data, final BitmapDrawable value) {
        if (mMemoryCache != null) {
            mMemoryCache.put(data, value);
        }

        synchronized (mDiskCacheLock) {
            final String key = hashKeyForDisk(data);
            final File file = getFileForKey(key);
            file.getParentFile().mkdirs();
            OutputStream out = null;
            try {
                out = new FileOutputStream(file);
                value.getBitmap().compress(DEFAULT_COMPRESS_FORMAT, DEFAULT_COMPRESS_QUALITY, out);
                out.close();
            } catch (final IOException e) {
                Log.e(TAG, "addBitmapToCache", e);
            } catch (Exception e) {
                Log.e(TAG, "addBitmapToCache", e);
            } finally {
                Utils.close(out);
            }
        }
    }

    @Override
    public void clearCache() {
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
            if (DEBUG) {
                Log.d(TAG, "Memory cache cleared");
            }
        }

        synchronized (mDiskCacheLock) {
            diskCacheDir.delete();
        }
    }

    @Override
    public Bitmap getBitmapFromDiskCache(final String data) {
        final String key = hashKeyForDisk(data);
        Bitmap bitmap = null;

        synchronized (mDiskCacheLock) {
            InputStream inputStream = null;
            try {
                if (DEBUG) {
                    Log.d(TAG, "Disk cache hit");
                }

                final File fileForKey = getFileForKey(key);
                if (!fileForKey.exists()) {
                    return null;
                }

                inputStream = new FileInputStream(fileForKey);
                if (inputStream != null) {
                    FileDescriptor fd = ((FileInputStream) inputStream).getFD();
                    // provide MAX_VALUE to skip sampling
                    bitmap = decodeSampledBitmapFromDescriptor(fd, mImgSize, mImgSize, this);
                }
            } catch (final IOException e) {
                Log.e(TAG, "getBitmapFromDiskCache - " + e);
            } finally {
                Utils.close(inputStream);
            }
            return bitmap;
        }
    }

    @Override
    public File getDiskCacheDir() {
        return diskCacheDir;
    }

    private File getFileForKey(final String key) {
        return new File(diskCacheDir + File.separator + key);
    }

    private void init() {
        diskCacheDir = getDiskCacheDir(mContext, CACHE_DIR);
        diskCacheDir.mkdirs();
        mReusableBitmaps = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());

        final int maxSize = Utils.calculateMemoryCacheSize(mContext);
        mMemoryCache = new LruCache<String, BitmapDrawable>(maxSize) {
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