package com.movie.filmtube.services;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import com.movie.filmtube.data.CategoryAndFilm;
import com.movie.filmtube.data.CategoryAndFilmDao;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.FilmYoutubeDao;
import com.movie.filmtube.data.sao.FilmSAO;
import com.movie.filmtube.utils.Constants;
import com.movie.filmtube.utils.MyDatabaseHelper;
import com.movie.filmtube.utils.MyPreferenceManager;
import com.youtube.bigbang.BuildConfig;
public class UpdateFilmThread extends Thread {

	public static boolean isUpdating = false;

	private MyDatabaseHelper databaseHelper;
	private MyPreferenceManager preferenceManager;
	private Context context;
	public static void runUpdateFilmThread(Context context) {
		if (!isRecentUpdate(context)) {
			UpdateFilmThread updateFilmThread = new UpdateFilmThread(context);
			updateFilmThread.start();
		}
	}
	public static boolean isRecentUpdate(Context context) {
		if (!isUpdating) {
			MyPreferenceManager preferenceManager = new MyPreferenceManager(
					context);

			String formatedDate = preferenceManager.getFormatedLastUpdate();
			Date date = preferenceManager.getDateLastUpdate();
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			if (date != null
					&& (TextUtils.isEmpty(formatedDate) || (calendar.getTime())
							.compareTo(date) > 0)) {
				return false;
			}
		}
		return true;
	}

	private UpdateFilmThread(Context context) {
		this.context = context.getApplicationContext();
		preferenceManager = new MyPreferenceManager(this.context);
		databaseHelper = MyDatabaseHelper.getInstance(this.context);
	}

	@Override
	public void run() {
		super.run();
		isUpdating = true;
		FilmYoutubeDao filmDao = databaseHelper.getmSession()
				.getFilmYoutubeDao();
		CategoryAndFilmDao categoryAndFilmDao = databaseHelper.getmSession()
				.getCategoryAndFilmDao();
		try {		
			filmDao.getDatabase().beginTransaction();
			List<FilmYoutube> updatedFilms = FilmSAO.getUpdateFilms(context);
			if (updatedFilms.size() > 0) {
				filmDao.getDatabase().beginTransaction();
				for (FilmYoutube film : updatedFilms) {
					FilmYoutube filmYoutube = filmDao
							.getFilmBasedOnVideoId(film.getYoutubeId());
					if (filmYoutube != null) {
						filmYoutube.setServerId(film.getServerId());
						filmYoutube.setIsLocalFilm(true);
						filmDao.update(filmYoutube);
					} else {
						long id = filmDao.insert(film);
						film.setId(id);
					}
					if (film.getCategoryAndFilms() != null) {
						for (CategoryAndFilm categoryAndFilm : film
								.getCategoryAndFilms()) {
							long cateId = categoryAndFilmDao
									.insert(categoryAndFilm);
							categoryAndFilm.setId(cateId);
						}
					}
				}
				preferenceManager.setLastUpdate(new Date());
			}
		} finally {
			filmDao.getDatabase().setTransactionSuccessful();
			filmDao.getDatabase().endTransaction();
			isUpdating = false;
			this.context.sendBroadcast(new Intent(
					Constants.ACTION_UPDATE_FILM_FINISHED));
		}
	}
}
