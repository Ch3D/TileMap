package com.android.ch3d.tilemap.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.android.ch3d.tilemap.util.cache.ImageCacheBase.hashKeyForDisk;

/**
 * Created by Ch3D on 24.04.2015.
 */
@Deprecated
public class ImageDownloaderSimple extends ImageResizer {
	private static final String TAG = ImageDownloader.class.getSimpleName();

	private static final int IO_BUFFER_SIZE = 8 * 1024;

	private final Object mDiskCacheLock = new Object();

	public ImageDownloaderSimple(Context context, int imageWidth, int imageHeight) {
		super(context, imageWidth, imageHeight);
		init(context);
	}

	public ImageDownloaderSimple(Context context, int imageSize) {
		super(context, imageSize);
		init(context);
	}

	private void checkConnection(Context context) {
		final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if(networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
			Toast.makeText(context, R.string.no_network_connection_toast, Toast.LENGTH_LONG).show();
			Log.e(TAG, "checkConnection : no internet connection");
		}
	}

	@Override
	protected void closeCacheInternal() {
		super.closeCacheInternal();
		synchronized(mDiskCacheLock) {
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "HTTP cache closed");
			}
		}
	}

	public boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
		HttpURLConnection urlConnection = null;
		BufferedOutputStream out = null;
		BufferedInputStream in = null;

		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
			out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

			int b;
			while((b = in.read()) != -1) {
				out.write(b);
			}
			return true;
		} catch(final IOException e) {
			Log.e(TAG, "downloadUrlToStream", e);
		} finally {
			if(urlConnection != null) {
				urlConnection.disconnect();
			}
			try {
				if(out != null) {
					out.close();
				}
				if(in != null) {
					in.close();
				}
			} catch(final IOException e) {
				Log.e(TAG, "downloadUrlToStream", e);
			}
		}
		return false;
	}

	@Override
	protected void flushCacheInternal() {
		super.flushCacheInternal();
		synchronized(mDiskCacheLock) {
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "HTTP cache flushed");
			}
		}
	}

	private File getFileForKey(final String key) {
		return new File(getImageCache().getParams().diskCacheDir + "/" + key);
	}

	private void init(Context context) {
		checkConnection(context);
	}

	private Bitmap processBitmap(String data) {
		if(BuildConfig.DEBUG) {
			Log.d(TAG, "processing bitmap = " + data);
		}
		synchronized(mDiskCacheLock) {
			final String key = hashKeyForDisk(data);
			FileOutputStream outputStream;
			try {
				final File file = getFileForKey(key);
				file.getParentFile().mkdirs();
				outputStream = new FileOutputStream(file);
				if(downloadUrlToStream(data, outputStream)) {
					final FileDescriptor fd = new FileInputStream(file).getFD();
					return decodeSampledBitmapFromDescriptor(fd, mImageWidth, mImageHeight, getImageCache());
				}
			} catch(FileNotFoundException e) {
				Log.e(TAG, "processBitmap", e);
			} catch(IOException e) {
				Log.e(TAG, "processBitmap", e);
			}
			return null;
		}
	}

	@Override
	protected Bitmap processBitmap(Object data) {
		return processBitmap(String.valueOf(data));
	}
}
