package com.android.ch3d.tilemap.provider;

import com.android.ch3d.tilemap.model.Tile;
import com.android.ch3d.tilemap.model.TileFactory;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class SimpleTileProvider implements TilesProvider {

	public static final String TILE_IMG_URL = "http://b.tile.opencyclemap.org/cycle/16/%s/%s.png";

	@Override
	public Tile getTile(final int x, final int y) {
		final String imgUrl = String.format(TILE_IMG_URL, 33198 + x, 22539 + y);
		return TileFactory.create(x, y, imgUrl);
	}
}
