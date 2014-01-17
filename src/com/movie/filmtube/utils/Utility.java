package com.movie.filmtube.utils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.R.bool;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Html;
import android.text.TextUtils;
import com.movie.filmtube.activity.FilmDetailActivity;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.FilmYoutubeDao;
import com.movie.filmtube.utils.caching.ImageUtility;
import com.movie.filmtube.utils.movie.world.helper.MovieSearchingHelper;
import com.movie.filmtube.utils.movie.world.helper.MovieVideo;
import com.movie.filmtube.utils.movie.world.helper.MovieConstants.VideoQuality;
import com.youtube.bigbang.R;

public class Utility {
	public static String foramtString(String s) {
		return TextUtils.isEmpty(s) ? s : Html.fromHtml(s).toString();
	}
	public static void startActivitySlidingLeftToRight(Activity outAct,
			Intent intent, boolean isFinished) {
		outAct.startActivity(intent);
		outAct.overridePendingTransition(R.anim.slide_in_left_to_right,
				R.anim.slide_out_left_to_right);
		if (isFinished) {
			outAct.finish();
		}

	}
	public static void startActivitySlidingRightToLeft(Activity outAct,
			Intent intent, boolean isFinished) {
		outAct.startActivity(intent);
		outAct.overridePendingTransition(R.anim.slide_in_right_to_left,
				R.anim.slide_out_right_to_left);
		if (isFinished) {
			outAct.finish();
		}
	}
	public static String convertTimeToDisplayedTime(long timeInMiliSeconds) {
		if (timeInMiliSeconds <= 0) {
			return "00:00";
		}
		long timeInSeconds = timeInMiliSeconds / 1000;
		int minutes = (int) (timeInSeconds / 60);
		int seconds = (int) (timeInSeconds % 60);
		return String.format("%02d:%02d", minutes, seconds);
	}
	public static String getFilmDownloadFolder(Context context) {
		String filmFolderPath = ImageUtility.getDownloadFolder(context)
				+ File.separator + Constants.FORDER_DOWNLOAD_FILMS;
		File file = new File(filmFolderPath);
		if (!file.exists() || !file.isDirectory()) {
			file.mkdir();
		}
		return filmFolderPath;
	}
	public static String getFilmLocalPathFromUrl(Context context,
			String youtubeVideoId) {
		return Utility.getFilmDownloadFolder(context) + File.separator
				+ youtubeVideoId.hashCode();
	}
	public static void showInformationDialog(final Activity activity,
			final String message) {
		AlertDialog.Builder builder = new Builder(activity);
		builder.setTitle(activity.getString(R.string.dialog_information_title));
		builder.setMessage(message);
		builder.setPositiveButton(activity.getString(R.string.ok_label), null);
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}
	public static boolean getVideoStreamingUrls(FilmYoutube filmYoutube) {
		List<MovieVideo> list = null;
		for (int i = 0; i < 3; i++) {
			list = MovieSearchingHelper
					.getVideoUrlFromYoutubeVideoId(filmYoutube.getYoutubeId());
			if (list.size() > 0) {
				break;
			}
		}
		boolean isMediumVideoAvailable = false;
		for (MovieVideo youtubeVideo : list) {
			if (youtubeVideo.getVideoQuality() == VideoQuality.High) {
				filmYoutube.setStreamingUrlHQ(youtubeVideo.getStreamingUrl());
			} else if (youtubeVideo.getVideoQuality() == VideoQuality.Medium) {
				filmYoutube.setStreamingUrlMedium(youtubeVideo
						.getStreamingUrl());
				isMediumVideoAvailable = true;
			} else if (youtubeVideo.getVideoQuality() == VideoQuality.Low
					&& !isMediumVideoAvailable) {
				filmYoutube.setStreamingUrlMedium(youtubeVideo
						.getStreamingUrl());
			}
		}
		if (list.size() > 0) {
			return true;
		} else {
			return false;
		}
	}
	public static String getHighestQualityUrl(FilmYoutube filmYoutube) {
		String url = null;
		url = TextUtils.isEmpty(filmYoutube.getStreamingUrlHQ()) ? filmYoutube
				.getStreamingUrlMedium() : filmYoutube.getStreamingUrlHQ();
		return url;
	}
	public static void startFilmDetailActivity(Activity context,
			FilmYoutube filmYoutube, ArrayList<FilmYoutube> relatedFilms) {
		Intent intent = new Intent(context, FilmDetailActivity.class);
		intent.putExtra(Constants.KEY_FILM_PICKED, filmYoutube);
		intent.putParcelableArrayListExtra(Constants.KEY_FILMS_RELATED,
				relatedFilms);
		startActivitySlidingRightToLeft(context, intent, false);
	}
	public static void showDeleteBookmarkDialog(final Context context,
			final FilmYoutube film, final OnBookmarkListener onBookmarkListener) {
		MyDatabaseHelper databaseHelper = MyDatabaseHelper.getInstance(context);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.bookmark_delete_title));
		builder.setMessage(context.getString(R.string.bookmark_delete_message));
		builder.setPositiveButton(context.getString(R.string.ok_label),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						new Thread(new Runnable() {

							@Override
							public void run() {
								final FilmYoutubeDao filmYoutubeDao = MyDatabaseHelper
										.getInstance(context).getmSession()
										.getFilmYoutubeDao();
								film.setIsBookmarked(false);
								if (!film.getIsLocalFilm()
										&& film.getDownloadStatus() == DownloadStatus.NOT_DOWNLOADED) {
									filmYoutubeDao.delete(film);
								} else {
									filmYoutubeDao.update(film);
								}
								onBookmarkListener.onBookmarkDeleted(film);
								Intent intent = new Intent(
										Constants.ACTION_RELOAD_BOOKMARKS);
								intent.putExtra(Constants.KEY_BOOKMARK_DELETED,
										film.getYoutubeId());
								context.sendBroadcast(intent);

							}
						}).start();
					}
				});
		builder.setNegativeButton(context.getString(R.string.cancel_label),
				null);
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}
	public static void showBookmarkDialog(final Context context,
			final FilmYoutube filmYoutube,
			final OnBookmarkListener onBookmarkListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.bookmark_dialog_title));
		builder.setMessage(context.getString(R.string.bookmark_dialog_message)
				+ filmYoutube.getName());
		builder.setPositiveButton(context.getString(R.string.bookmark_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Thread thread = new Thread(new Runnable() {

							@Override
							public void run() {
								FilmYoutubeDao filmYoutubeDao = MyDatabaseHelper
										.getInstance(context).getmSession()
										.getFilmYoutubeDao();
								filmYoutube.setIsBookmarked(true);
								if (filmYoutube.getId() != null
										&& filmYoutube.getId() > 0) {
									filmYoutubeDao.update(filmYoutube);
								} else {
									FilmYoutube film = filmYoutubeDao
											.getFilmBasedOnVideoId(filmYoutube
													.getYoutubeId());
									if (film != null) {
										film.setIsBookmarked(true);
										filmYoutubeDao.update(film);
									} else {
										long id = filmYoutubeDao
												.insert(filmYoutube);
										filmYoutube.setId(id);
									}
								}
								if (onBookmarkListener != null) {
									onBookmarkListener
											.onBookmarkedSucceed(filmYoutube);
								}
								context.sendBroadcast(new Intent(
										Constants.ACTION_RELOAD_BOOKMARKS));
							}
						});
						thread.start();

					}
				});

		builder.setNegativeButton(context.getString(R.string.bookmark_cancel),
				null);
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

}
