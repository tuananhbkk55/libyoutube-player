package com.movie.filmtube.utils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.sao.GsonObjectHelper.GsonGetAllFilms;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
public class MyPreferenceManager {
	public static final String KEY_VOLUME = "volume";
	public static final String KEY_LAST_UPDATE = "lastUpdate";
	public static final String KEY_LAST_CHAPTER = "lastChapter";
	public static final String KEY_PLAYED_VIDEOS_COUNT = "fullScreenCount";
	public static final String KEY_SHOULD_SHOW_VIDEO_ADS = "showAds";
	public static final String KEY_LAST_SEARCHED_VIDEOS = "lastSearchedVideo";
	private SharedPreferences mSharedPreferences;
	public MyPreferenceManager(Context context) {
		mSharedPreferences = context.getSharedPreferences(
				Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
	}
	public void setVolume(float volume) {
		mSharedPreferences.edit().putFloat(KEY_VOLUME, volume).commit();
	}
	public float getVolume() {
		return mSharedPreferences.getFloat(KEY_VOLUME, 0.5f);
	}
	public void setLastUpdate(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				Constants.DATE_PATTERN);
		String formatedDate = simpleDateFormat.format(date);
		mSharedPreferences.edit().putString(KEY_LAST_UPDATE, formatedDate)
				.commit();
	}
	public String getFormatedLastUpdate() {
		return mSharedPreferences.getString(KEY_LAST_UPDATE, "");
	}
	public Date getDateLastUpdate() {
		String formatedDate = getFormatedLastUpdate();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				Constants.DATE_PATTERN);
		Date date = null;
		try {
			date = simpleDateFormat.parse(formatedDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}
	private String getLastChapter() {
		return mSharedPreferences.getString(KEY_LAST_CHAPTER, "");
	}
	public void setLastChapter(String videoId) {
		String previousChapterId = getLastChapter();
		if (!previousChapterId.equals(videoId)) {
			mSharedPreferences.edit().putString(KEY_LAST_CHAPTER, videoId)
					.commit();
			int count = getPlayedVideoWithoutAds() + 1;
			setPlayedVideoWithoutAds(count);
			if (count >= 3) {
				setShouldShowVideoAds(true);
			}
		}
	}
	private void setPlayedVideoWithoutAds(int count) {
		mSharedPreferences.edit().putInt(KEY_PLAYED_VIDEOS_COUNT, count)
				.commit();
	}
	private int getPlayedVideoWithoutAds() {
		return mSharedPreferences.getInt(KEY_PLAYED_VIDEOS_COUNT, 0);
	}
	public void setShouldShowVideoAds(boolean shouldShow) {
		mSharedPreferences.edit()
				.putBoolean(KEY_SHOULD_SHOW_VIDEO_ADS, shouldShow).commit();
		if (!shouldShow) {
			setPlayedVideoWithoutAds(0);
		}
	}
	public boolean shouldShowVideoAds() {
		return mSharedPreferences.getBoolean(KEY_SHOULD_SHOW_VIDEO_ADS, false);
	}
	public void setLastSearchedVideos(List<FilmYoutube> list) {
		if (list.size() <= 0) {
			return;
		}
		JsonArray jsonArray = new JsonArray();
		for (FilmYoutube filmYoutube : list) {
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty(ServiceConfig.TAG_VIDEO_ID,
					filmYoutube.getYoutubeId());
			jsonObject.addProperty(ServiceConfig.TAG_NAME,
					filmYoutube.getName());
			jsonArray.add(jsonObject);
		}
		this.mSharedPreferences.edit()
				.putString(KEY_LAST_SEARCHED_VIDEOS, jsonArray.toString())
				.commit();

	}
	public ArrayList<FilmYoutube> getLastSerchedVideos() {
		String jsonLast = mSharedPreferences.getString(
				KEY_LAST_SEARCHED_VIDEOS, "");
		if (TextUtils.isEmpty(jsonLast)) {
			return new ArrayList<FilmYoutube>();
		}
		Gson gson = new Gson();
		FilmYoutube[] filmYoutubes = gson.fromJson(jsonLast,
				FilmYoutube[].class);
		if (filmYoutubes == null) {
			return new ArrayList<FilmYoutube>();
		} else {
			return new ArrayList<FilmYoutube>(Arrays.asList(filmYoutubes));
		}
	}
}
