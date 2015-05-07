package com.android.ch3d.tilemap.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.android.ch3d.tilemap.util.cache.ImageCache;

import java.io.FileDescriptor;

/**
 * Created by Ch3D on 30.04.2015.
 */
public class ImageUtils {
    public static Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fileDescriptor, int width, int height, ImageCache cache) {
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
        if (cache != null) {
            Bitmap inBitmap = cache.getBitmapFromReusableSet(options);
            if (inBitmap != null) {
                options.inBitmap = inBitmap;
            }
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }

            long totalPixels = width * height / inSampleSize;
            final long totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2;
                totalPixels /= 2;
            }
        }
        return inSampleSize;
    }
}
