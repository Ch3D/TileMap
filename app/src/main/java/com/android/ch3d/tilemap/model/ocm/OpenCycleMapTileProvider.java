package com.android.ch3d.tilemap.model.ocm;

import com.android.ch3d.tilemap.model.Tile;
import com.android.ch3d.tilemap.model.TileFactory;
import com.android.ch3d.tilemap.model.TileProviderType;
import com.android.ch3d.tilemap.model.TilesProvider;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class OpenCycleMapTileProvider implements TilesProvider {
	@Override
	public Tile getTile(final int x, final int y) {
		return TileFactory.create(x, y, TileProviderType.OCM);
	}
}