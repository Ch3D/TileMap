package com.android.ch3d.tilemap.model;

public class TileProviderType {

	public static final TileProviderType OCM = new TileProviderType("OpenCycleMap");

	private String mName;

	public TileProviderType(final String name) {
		mName = name;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) {
			return true;
		}
		if(!(o instanceof TileProviderType)) {
			return false;
		}

		final TileProviderType that = (TileProviderType) o;

		if(mName != null ? !mName.equals(that.mName) : that.mName != null) {
			return false;
		}

		return true;
	}

	public String getName() {
		return mName;
	}

	@Override
	public int hashCode() {
		return mName != null ? mName.hashCode() : 0;
	}
}
