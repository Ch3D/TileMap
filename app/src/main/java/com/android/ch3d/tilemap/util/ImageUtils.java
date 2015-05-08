package com.android.ch3d.tilemap.util;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;

import com.android.ch3d.tilemap.util.cache.ImageCache;

import java.io.FileDescriptor;

public class ImageUtils {

	public static Bitmap decodeSampledBitmap(FileDescriptor fileDescriptor, int width, int height, ImageCache cache) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
		options.inSampleSize = calculateInSampleSize(options, width, height);
		options.inJustDecodeBounds = false;
		addInBitmapOptions(options, cache);
		return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
	}

	private static void addInBitmapOptions(BitmapFactory.Options options, ImageCache cache) {
		options.inMutable = true;
		if(cache != null) {
			Bitmap inBitmap = cache.getBitmapFromReusableSet(options);
			if(inBitmap != null) {
				options.inBitmap = inBitmap;
			}
		}
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if(height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			while((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}

			long totalPixels = width * height / inSampleSize;
			final long totalReqPixelsCap = reqWidth * reqHeight * 2;

			while(totalPixels > totalReqPixelsCap) {
				inSampleSize *= 2;
				totalPixels /= 2;
			}
		}
		return inSampleSize;
	}

	public static int getBytesPerPixel(Bitmap.Config config) {
		if(config == Bitmap.Config.ARGB_8888) {
			return 4;
		} else if(config == Bitmap.Config.RGB_565) {
			return 2;
		} else if(config == Bitmap.Config.ARGB_4444) {
			return 2;
		} else if(config == Bitmap.Config.ALPHA_8) {
			return 1;
		}
		return 1;
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static int getBitmapSize(BitmapDrawable value) {
		Bitmap bitmap = value.getBitmap();
		if(Utils.hasKitKat()) {
			return bitmap.getAllocationByteCount();
		}
		return bitmap.getByteCount();
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static boolean canUseForInBitmap(Bitmap candidate, BitmapFactory.Options targetOptions) {
		if(!Utils.hasKitKat()) {
			return candidate.getWidth() == targetOptions.outWidth
					&& candidate.getHeight() == targetOptions.outHeight
					&& targetOptions.inSampleSize == 1;
		}

		int width = targetOptions.outWidth / targetOptions.inSampleSize;
		int height = targetOptions.outHeight / targetOptions.inSampleSize;
		int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
		return byteCount <= candidate.getAllocationByteCount();
	}
}
