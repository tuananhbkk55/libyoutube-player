package com.movie.filmtube.utils.caching;

import java.io.File;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

public class ImageUtility {

	public static String getDownloadFolder(Context context) {
		String downloadPath = "";
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)
				&& context.getExternalFilesDir(null) != null) {
			String sdcard = context.getExternalFilesDir(null).getAbsolutePath();
			downloadPath = sdcard + File.separator
					+ ImageConstant.DOWNLOAD_FOLDER_NAME;
		} else {
			downloadPath = context.getFilesDir() + File.separator
					+ ImageConstant.DOWNLOAD_FOLDER_NAME;
		}
		File file = new File(downloadPath);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdir()) {
			
			}
		}
		return downloadPath;
	}

	public static final int VERSION_CODES_HONEYCOMB_MR1 = 12;
	public static final int VERSION_CODES_HONEYCOMB = 11;
	public static final int VERSION_CODES_JELLYBEAN_MR1 = 17;

	public static boolean hasHoneycombMR1() {
		return Build.VERSION.SDK_INT >= VERSION_CODES_HONEYCOMB_MR1;
	}

	public static boolean hasHoneycomb() {
		return Build.VERSION.SDK_INT >= VERSION_CODES_HONEYCOMB;
	}

	public static boolean hasJellyBeanMR1() {
		return Build.VERSION.SDK_INT >= VERSION_CODES_JELLYBEAN_MR1;
	}

}
