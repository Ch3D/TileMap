package com.android.ch3d.tilemap.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.android.ch3d.tilemap.R;
import com.android.ch3d.tilemap.model.TilesManager;
import com.android.ch3d.tilemap.provider.OpenCycleMapTileProvider;
import com.android.ch3d.tilemap.widget.TileView;

public class MainActivity extends AppCompatActivity {

	private TileView mTilesLayout;

	private OpenCycleMapTileProvider mTilesProvider;

	private TilesManager mTilesManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTilesLayout = (TileView) findViewById(R.id.tiles);
		mTilesProvider = new OpenCycleMapTileProvider();
		mTilesManager = new TilesManager(this, mTilesProvider);
		mTilesLayout.setTilesManager(mTilesManager);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mTilesManager.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		mTilesManager.onResume();
		mTilesLayout.renderTiles();
	}
}
