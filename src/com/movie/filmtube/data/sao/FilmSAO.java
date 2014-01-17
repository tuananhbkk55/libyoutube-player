package com.movie.filmtube.data.sao;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.sao.GsonObjectHelper.GsonGetAllFilms;
import com.movie.filmtube.helper.ServiceHelper;
import com.movie.filmtube.utils.MyPreferenceManager;
import com.movie.filmtube.utils.ServiceConfig;
import com.youtube.bigbang.BuildConfig;

public class FilmSAO {
	public static final String TAG = "FilmSAO";xt) {
		List<FilmYoutube> list = new ArrayList<FilmYoutube>();
		try {
			String respond = ServiceHelper.getInfo(
					ServiceConfig.URL_GET_ALL_FILMS, null);

			if (!TextUtils.isEmpty(respond)) {
				Gson gson = new Gson();
				GsonGetAllFilms[] array = gson.fromJson(respond,
						GsonGetAllFilms[].class);
				if (array != null) {
					for (GsonGetAllFilms gsonGetAllFilms : array) {
						gsonGetAllFilms.filmYoutube
								.setCategoryAndFilms(gsonGetAllFilms.categoryAndFilms);
						gsonGetAllFilms.filmYoutube.setIsLocalFilm(true);
						list.add(gsonGetAllFilms.filmYoutube);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		}
		
		return list;
	}
	public static List<FilmYoutube> getUpdateFilms(final Context context) {
		MyPreferenceManager preferenceManager = new MyPreferenceManager(context);
		List<FilmYoutube> list = new ArrayList<FilmYoutube>();
		try {
			String respond = ServiceHelper.getInfo(
					ServiceConfig.URL_UPDATE_FILMS
							+ preferenceManager.getFormatedLastUpdate(), null);
	
			if (!TextUtils.isEmpty(respond)) {
				Gson gson = new Gson();
				GsonGetAllFilms[] array = gson.fromJson(respond,
						GsonGetAllFilms[].class);
				if (array != null) {
					for (GsonGetAllFilms gsonGetAllFilms : array) {
						gsonGetAllFilms.filmYoutube
								.setCategoryAndFilms(gsonGetAllFilms.categoryAndFilms);
						gsonGetAllFilms.filmYoutube.setIsLocalFilm(true);
						list.add(gsonGetAllFilms.filmYoutube);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		}
	
		return list;
	}

}
