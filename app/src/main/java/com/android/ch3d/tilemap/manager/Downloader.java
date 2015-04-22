package com.android.ch3d.tilemap.manager;

/**
 * Created by Ch3D on 22.04.2015.
 */
public interface Downloader<K, V> {

	public interface Callback<V> {
		void onDownloadComplete(V value);
	}

	public void download(K key, Callback<V> callback);
}
