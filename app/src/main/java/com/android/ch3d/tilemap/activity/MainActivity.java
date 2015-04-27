package com.android.ch3d.tilemap.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.android.ch3d.tilemap.R;
import com.android.ch3d.tilemap.manager.TilesManager;
import com.android.ch3d.tilemap.provider.SimpleTileProvider;
import com.android.ch3d.tilemap.widget.TileLayout;

public class MainActivity extends AppCompatActivity {

	private TileLayout mTilesLayout;

	private SimpleTileProvider mTilesProvider;

	private TilesManager mTilesManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTilesLayout = (TileLayout) findViewById(R.id.tiles);
		mTilesLayout.setGridSize(getResources().getInteger(R.integer.config_tiles_grid_size));
		mTilesProvider = new SimpleTileProvider();
		mTilesManager = new TilesManager(this, mTilesProvider);
		mTilesLayout.setTilesManager(mTilesManager);
		mTilesLayout.initSize(getResources().getInteger(R.integer.config_tiles_count));
		mTilesLayout.renderTiles();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mTilesManager.closeCache();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mTilesManager.setExitTasksEarly(true);
		mTilesManager.flushCache();
	}

	@Override
	public void onResume() {
		super.onResume();
		mTilesManager.setExitTasksEarly(false);
	}
}
