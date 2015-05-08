package com.android.ch3d.tilemap.downloader;

import android.content.Context;
import android.util.Log;

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
public abstract class ImageDownloaderBase extends ImageWorker {

	protected static final int BUFFER_SIZE = 8 * 1024;

	protected final Object mDiskCacheLock = new Object();

	private static final String TAG = ImageDownloaderBase.class.getSimpleName();

	public ImageDownloaderBase(final Context context) {
		super(context);
	}

	public boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
		HttpURLConnection urlConnection = null;
		BufferedOutputStream out = null;
		BufferedInputStream in = null;

		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(), BUFFER_SIZE);
			out = new BufferedOutputStream(outputStream, BUFFER_SIZE);

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

}
