package com.android.ch3d.tilemap.util.downloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.android.ch3d.tilemap.R;
import com.android.ch3d.tilemap.util.ImageResizer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.android.ch3d.tilemap.util.Utils.close;

/**
 * Created by Ch3D on 28.04.2015.
 */
public abstract class ImageDownloaderBase extends ImageResizer {

	protected static final int IO_BUFFER_SIZE = 8 * 1024;

	protected final Object mDiskCacheLock = new Object();

	private static final String TAG = ImageDownloaderBase.class.getSimpleName();

	public ImageDownloaderBase(final Context context, final int imageWidth, final int imageHeight) {
		super(context, imageWidth, imageHeight);
		init(context);
	}

	public ImageDownloaderBase(final Context context, final int imageSize) {
		super(context, imageSize);
		init(context);
	}

	protected void checkConnection(Context context) {
		final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if(networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
			Toast.makeText(context, R.string.no_network_connection_toast, Toast.LENGTH_LONG).show();
			Log.e(TAG, "checkConnection : no internet connection");
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
			close(out);
			close(in);
		}
		return false;
	}

	protected abstract void init(final Context context);

	@Override
	protected Bitmap processBitmap(Object data) {
		return processBitmap(String.valueOf(data));
	}

	protected abstract Bitmap processBitmap(final String data);
}
