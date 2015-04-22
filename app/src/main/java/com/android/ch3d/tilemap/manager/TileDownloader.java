package com.android.ch3d.tilemap.manager;

import android.content.Context;
import android.graphics.Bitmap;

import com.android.ch3d.tilemap.model.Tile;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class TileDownloader implements Downloader<Tile, Bitmap> {
	public TileDownloader(Context context) {
		mContext = context;
	}

	private Context mContext;

	@Override
	public void download(final Tile tile, final Callback<Bitmap> callback) {

	}
}
