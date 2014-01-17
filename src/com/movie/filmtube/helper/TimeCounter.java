package com.movie.filmtube.helper;
import android.text.TextUtils;
import android.util.Log;
public class TimeCounter {
	private static final String TAG = "TimeCounter";

	private long timeStart;
	public void startTimeCounter(String message) {
		timeStart = System.currentTimeMillis();
		if (!TextUtils.isEmpty(message)) {
			Log.i(TAG, message);
		}
	}
	public void endTimeCounter(String message) {
		long diff = System.currentTimeMillis() - timeStart;
		String mess = "End time counter " + message + ":"
				+ (float) (diff / 1000f);
		Log.i(TAG, mess);
	}

}
