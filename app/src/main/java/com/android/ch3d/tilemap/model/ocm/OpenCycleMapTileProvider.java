package com.android.ch3d.tilemap.model.ocm;

import com.android.ch3d.tilemap.model.Tile;
import com.android.ch3d.tilemap.model.TileFactory;
import com.android.ch3d.tilemap.model.TileProviderType;
import com.android.ch3d.tilemap.model.TilesProvider;

public class OpenCycleMapTileProvider implements TilesProvider {
	@Override
	public Tile getTile(final int x, final int y) {
		return TileFactory.create(x, y, TileProviderType.OCM);
	}
}