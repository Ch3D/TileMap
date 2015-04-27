package com.android.ch3d.tilemap.model;

/**
 * Created by Ch3D on 23.04.2015.
 */
public class TileFactory {
	public static Tile create(final int x, final int y, final String url) {
		return new Tile(x, y, url);
	}
}
