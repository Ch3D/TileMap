package com.android.ch3d.tilemap.model;

/**
 * Created by Ch3D on 22.04.2015.
 */
public class Tile {
	public Tile(int x, int y, String imgUrl) {
		mX = x;
		mY = y;
		mImgUrl = imgUrl;
	}

	public int getX() {
		return mX;
	}

	public int getY() {
		return mY;
	}

	public String getImgUrl() {
		return mImgUrl;
	}

	private final int mX;

	private final int mY;

	private final String mImgUrl;

	@Override
	public boolean equals(final Object o) {
		if(this == o) {
			return true;
		}
		if(!(o instanceof Tile)) {
			return false;
		}

		final Tile tile = (Tile) o;

		if(mX != tile.mX) {
			return false;
		}
		if(mY != tile.mY) {
			return false;
		}
		if(!mImgUrl.equals(tile.mImgUrl)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = mX;
		result = 31 * result + mY;
		result = 31 * result + mImgUrl.hashCode();
		return result;
	}

}
