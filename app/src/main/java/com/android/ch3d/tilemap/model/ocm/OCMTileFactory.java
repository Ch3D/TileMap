package com.android.ch3d.tilemap.model.ocm;

import com.android.ch3d.tilemap.model.Tile;

/**
 * Created by Ch3D on 08.05.2015.
 */
public class OCMTileFactory {

	public static final String TILE_IMG_URL = "http://b.tile.opencyclemap.org/cycle/15/%s/%s.png";

	public static final int START_X_POS = 23946;

	public static final int START_Y_POS = 10388;

	public static Tile create(final int x, final int y) {
		return new OCMTile(x, y, String.format(TILE_IMG_URL, START_X_POS + x, START_Y_POS + y));

	}
}
