package com.android.ch3d.tilemap.model;

import com.android.ch3d.tilemap.model.ocm.OCMTileFactory;

public class TileFactory {
	public static Tile create(final int x, final int y, final TileProviderType tileProvider) {
		if(tileProvider == null) {
			throw new IllegalArgumentException("Unable to instantiate tile for NULL provider");
		}
		if(x < 0 || y < 0) {
			throw new IllegalArgumentException("Wrong tile coordinates - x = " + x + " y = " + y);
		}

		if(TileProviderType.OCM.equals(tileProvider)) {
			return OCMTileFactory.create(x, y);
		}
		throw new IllegalArgumentException("Unknown tile provider - " + tileProvider.getName());
	}
}
