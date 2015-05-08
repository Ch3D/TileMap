package com.android.ch3d.tilemap.downloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.util.ImageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.android.ch3d.tilemap.util.Utils.close;
import static com.android.ch3d.tilemap.util.Utils.hashKeyForDisk;

/**
 * Created by Ch3D on 24.04.2015.
 */
public class ImageDownloader extends ImageDownloaderBase {

	private static final String TAG = ImageDownloader.class.getSimpleName();

	private final int mImgSize;

	public ImageDownloader(final Context context, final int imgSize) {
		super(context);
		mImgSize = imgSize;
	}

	private File getFileForKey(final String key) {
		return new File(getImageCache().getDiskCacheDir() + File.separator + key);
	}

	@Override
	protected Bitmap processBitmap(String url) {
		if(BuildConfig.DEBUG) {
			Log.d(TAG, "processing bitmap = " + url);
		}
		synchronized(mDiskCacheLock) {
			final String key = hashKeyForDisk(url);
			FileOutputStream outputStream = null;
			FileInputStream inputStream = null;
			try {
				final File file = getFileForKey(key);
				file.getParentFile().mkdirs();
				outputStream = new FileOutputStream(file);
				if(downloadUrlToStream(url, outputStream)) {
					inputStream = new FileInputStream(file);
					return ImageUtils.decodeSampledBitmap(inputStream.getFD(), mImgSize, mImgSize, getImageCache());
				}
			} catch(FileNotFoundException e) {
				Log.e(TAG, "processBitmap", e);
			} catch(IOException e) {
				Log.e(TAG, "processBitmap", e);
			} finally {
				close(outputStream);
				close(inputStream);
			}
			return null;
		}
	}
}
