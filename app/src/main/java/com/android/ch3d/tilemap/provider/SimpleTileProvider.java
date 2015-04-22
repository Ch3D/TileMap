package com.android.ch3d.tilemap.provider;

import com.android.ch3d.tilemap.model.Tile;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class SimpleTileProvider implements TilesProvider {

	public static final String IMG_URL = "http://b.tile.opencyclemap.org/cycle/16/33198/22539.png";

	@Override
	public Tile getTile(final int x, final int y) {
		return new Tile(x, y, IMG_URL);
	}

}
