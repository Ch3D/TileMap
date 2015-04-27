package com.android.ch3d.tilemap.util.cache;

import android.support.v4.app.FragmentManager;

/**
 * Created by Ch3D on 27.04.2015.
 */
public class ImageCacheFactory {
	public static ImageCache getInstance(int type, FragmentManager fragmentManager, ImageCacheBase.ImageCacheParams cacheParams) {
		switch(type) {
			case ImageCacheBase.CACHE_TYPE_ADVANCED:
				return ImageCacheAdvanced.getInstance(fragmentManager, cacheParams);

			case ImageCacheBase.CACHE_TYPE_SIMPLE:
			default:
				return ImageCacheSimple.getInstance(fragmentManager, cacheParams);
		}
	}

}

