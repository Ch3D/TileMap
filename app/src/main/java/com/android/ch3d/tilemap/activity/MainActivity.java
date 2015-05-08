package com.android.ch3d.tilemap.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.android.ch3d.tilemap.R;
import com.android.ch3d.tilemap.downloader.ImageDownloader;
import com.android.ch3d.tilemap.model.TilesManager;
import com.android.ch3d.tilemap.model.ocm.OpenCycleMapTileProvider;
import com.android.ch3d.tilemap.cache.ImageCacheSimple;
import com.android.ch3d.tilemap.widget.TileLayout;

public class MainActivity extends AppCompatActivity {

	private TileLayout mTilesLayout;

	private TilesManager mTilesManager;

	private OpenCycleMapTileProvider mTilesProvider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final int gridSize = getResources().getInteger(R.integer.config_tiles_grid_size);

		mTilesLayout = (TileLayout) findViewById(R.id.tiles);
		mTilesLayout.setGridSize(gridSize);
		mTilesProvider = new OpenCycleMapTileProvider();

		final int defaultImageSize = getResources().getDimensionPixelSize(R.dimen.item_size);
		final ImageDownloader mImageDownloader = new ImageDownloader(this, defaultImageSize);
		mImageDownloader.setImageCache(ImageCacheSimple.getInstance(this, getSupportFragmentManager(), defaultImageSize));

		mTilesManager = new TilesManager(mTilesProvider, mImageDownloader);
		mTilesLayout.setTilesManager(mTilesManager);
		mTilesLayout.initSize(gridSize * gridSize);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
		mTilesLayout.renderTiles();
	}
}
