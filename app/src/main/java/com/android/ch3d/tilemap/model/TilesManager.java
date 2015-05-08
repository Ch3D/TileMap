package com.android.ch3d.tilemap.model;

import android.util.Log;
import android.widget.ImageView;

import com.android.ch3d.tilemap.BuildConfig;
import com.android.ch3d.tilemap.downloader.ImageDownloader;

public class TilesManager {

	private static final String TAG = TilesManager.class.getSimpleName();

	private final TilesProvider mTilesProvider;

	private ImageDownloader mDownloader;

	public TilesManager(TilesProvider tilesProvider, ImageDownloader downloader) {
		mTilesProvider = tilesProvider;
		mDownloader = downloader;
	}

	public void loadTile(int x, int y, ImageView imageView) {
		if(BuildConfig.DEBUG) {
			Log.d(TAG, "Trying to load tile for [" + x + ", " + y + "]");
		}
		mDownloader.loadImage(mTilesProvider.getTile(x, y).getImgUrl(), imageView);
	}
}