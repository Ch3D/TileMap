package com.android.ch3d.tilemap.provider;

import com.android.ch3d.tilemap.model.Tile;
import com.android.ch3d.tilemap.model.TileFactory;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class OpenCycleCapTileProvider implements TilesProvider {

	public static final String TILE_IMG_URL = "http://b.tile.opencyclemap.org/cycle/15/%s/%s.png";

	public static final int START_X_POS = 23946;

	public static final int START_Y_POS = 10388;

	@Override
	public Tile getTile(final int x, final int y) {
		final String imgUrl = String.format(TILE_IMG_URL, START_X_POS + x, START_Y_POS + y);
		return TileFactory.create(x, y, imgUrl);
	}
}