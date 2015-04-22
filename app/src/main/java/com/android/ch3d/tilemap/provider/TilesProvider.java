package com.android.ch3d.tilemap.provider;

import com.android.ch3d.tilemap.model.Tile;

/**
 * Created by Ch3D on 22.04.2015.
 */
public interface TilesProvider {
	public Tile getTile(int x, int y);
}
