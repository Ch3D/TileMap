package com.android.ch3d.tilemap.provider;

import com.android.ch3d.tilemap.model.Tile;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class SimpleTileProvider implements TilesProvider {
	@Override
	public Tile getTile(final int x, final int y) {
		final String format = String.format("http://b.tile.opencyclemap.org/cycle/16/%s/%s.png", 33198 + x, 22539 + y);
		return new Tile(x, y, format);
	}
}
