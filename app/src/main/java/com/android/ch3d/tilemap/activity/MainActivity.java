package com.android.ch3d.tilemap.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.android.ch3d.tilemap.R;
import com.android.ch3d.tilemap.manager.TilesManager;
import com.android.ch3d.tilemap.provider.SimpleTileProvider;

public class MainActivity extends AppCompatActivity {

	private SimpleTileProvider mTilesProvider;

	private ImageView mImg1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getSupportActionBar().getThemedContext();
		
		mImg1 = (ImageView) findViewById(R.id.img1);
		mTilesProvider = new SimpleTileProvider();
		mTilesManager = new TilesManager(this, mTilesProvider);
	}

	private TilesManager mTilesManager;

	@Override
	protected void onStart() {
		super.onStart();
		mTilesManager.loadTile(mTilesProvider.getTile(0, 0), mImg1);
	}

	@Override
	public void onResume() {
		super.onResume();
		mTilesManager.setExitTasksEarly(false);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mTilesManager.setExitTasksEarly(true);
		mTilesManager.flushCache();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mTilesManager.closeCache();
	}

}
