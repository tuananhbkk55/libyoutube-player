package com.movie.filmtube.services;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.FilmYoutubeDao;
import com.movie.filmtube.utils.Constants;
import com.movie.filmtube.utils.DownloadStatus;
import com.movie.filmtube.utils.MyDatabaseHelper;
import com.movie.filmtube.utils.Utility;
import com.youtube.bigbang.BuildConfig;

public class DownloadThread extends Thread {
	private static final String TAG = "DownloadThread";
	private static final int MB = 1048876;
	public static boolean isDownloading = false;
	private static DownloadThread mDownloadThread;
	private Context context;
	private MyDatabaseHelper myDatabaseHelper;
	private FilmYoutubeDao mFilmYoutubeDao;
	private FilmYoutube mCancelFilm;
	private DownloadThread(Context context) {
		this.context = context.getApplicationContext();
		this.myDatabaseHelper = MyDatabaseHelper.getInstance(this.context);
		mFilmYoutubeDao = myDatabaseHelper.getmSession().getFilmYoutubeDao();
	}

	public void cancelDownloadChapter(FilmYoutube downloadFilm) {
		mCancelFilm = mFilmYoutubeDao.getFilmBasedOnVideoId(downloadFilm
				.getYoutubeId());
		if (mCancelFilm == null) {	
			return;
		}
		
		mCancelFilm.setDownloadStatus(DownloadStatus.NOT_DOWNLOADED);
		
		if (!downloadFilm.getIsLocalFilm() && !downloadFilm.getIsBookmarked()) {
			mFilmYoutubeDao.delete(mCancelFilm);
		} else {
			mFilmYoutubeDao.update(mCancelFilm);
		}
		String filePath = Utility.getFilmLocalPathFromUrl(context,
				downloadFilm.getYoutubeId());
		File file = new File(filePath);
		if (file.exists()) {			
			file.delete();
		}		
		Intent intent = new Intent(Constants.ACTION_DOWNLOAD_VIDEO_CANCELED);
		intent.putExtra(Constants.KEY_CHAPTER_UPDATE,
				downloadFilm.getYoutubeId());
		context.sendBroadcast(intent);
	}

	private void downloadVideo(FilmYoutube filmYoutube) {
		updateStatus(filmYoutube, DownloadStatus.DOWNLOADING);
		
		String filePath = Utility.getFilmLocalPathFromUrl(this.context,
				filmYoutube.getYoutubeId());
		File checkFile = new File(filePath);	
		if (checkFile.exists()) {			
			updateStatus(filmYoutube, DownloadStatus.DOWNLOAD_SUCCESS);
			return;
		}
		String url = Utility.getHighestQualityUrl(filmYoutube);		
		if (TextUtils.isEmpty(url)) {
			Utility.getVideoStreamingUrls(filmYoutube);
			url = Utility.getHighestQualityUrl(filmYoutube);
			if (!TextUtils.isEmpty(url)) {
				mFilmYoutubeDao.update(filmYoutube);
			}
		}

		try {
			final URL downloadFileUrl = new URL(url);
			final URLConnection urlConnection = downloadFileUrl
					.openConnection();
			File file = new File(Utility.getFilmDownloadFolder(context)
					+ File.separator + Constants.TEMP_FILE);			
			if (file.exists()) {				
				file.delete();
			}
			long fileSize = urlConnection.getContentLength();
			if (fileSize <= 0) {			
				fileSize = 30 * MB;
			} else {				
				filmYoutube.setFileSizeInMB((int) (fileSize / MB));
				updateStatus(filmYoutube, DownloadStatus.DOWNLOADING);
			}
		
			final FileOutputStream fileOutputStream = new FileOutputStream(file);
			final byte buffer[] = new byte[2048];
			final InputStream inputStream = urlConnection.getInputStream();
			int len = 0;

			long count = 0;
			int percent = 0;
			while ((len = inputStream.read(buffer)) > 0) {
				if (mCancelFilm != null) {
					if (mCancelFilm.getYoutubeId().equals(
							filmYoutube.getYoutubeId())) {
						
						fileOutputStream.flush();
						fileOutputStream.close();
						mCancelFilm = null;
						return;
					}
					mCancelFilm = null;
				}
				fileOutputStream.write(buffer, 0, len);
				count += len;
				int curentPercent = (int) (count * 100 / fileSize);
				if (curentPercent > percent) {
					percent = curentPercent;					
					updateStatus(filmYoutube, curentPercent);
				}

			}
			fileOutputStream.flush();
			fileOutputStream.close();
			file.renameTo(checkFile);
			updateStatus(filmYoutube, DownloadStatus.DOWNLOAD_SUCCESS);
		} catch (Exception exception) {
			exception.printStackTrace();
			updateStatus(filmYoutube, DownloadStatus.DOWNLOAD_FAILED);
		} finally {
			File file = new File(Utility.getFilmDownloadFolder(context)
					+ File.separator + Constants.TEMP_FILE);
			if (file.exists()) {
				file.delete();
			}
		}
	}
	private void updateStatus(FilmYoutube filmYoutube, int status) {
		filmYoutube.setDownloadStatus(status);
		mFilmYoutubeDao.update(filmYoutube);
		Intent intent = new Intent(Constants.ACTION_DOWNLOAD_VIDEO);
		intent.putExtra(Constants.KEY_STATUS_UPDATE, status);
		intent.putExtra(Constants.KEY_CHAPTER_UPDATE,
				filmYoutube.getYoutubeId());
		if (filmYoutube.getFileSizeInMB() != null
				&& filmYoutube.getFileSizeInMB() > 0) {
			intent.putExtra(Constants.KEY_FILE_SIZE, filmYoutube
					.getFileSizeInMB().intValue());
		}
		this.context.sendBroadcast(intent);
	}
	private FilmYoutube getNextDownloadNotStarted() {
		List<FilmYoutube> list = this.mFilmYoutubeDao
				.queryBuilder()
				.where(FilmYoutubeDao.Properties.DownloadStatus
						.eq(DownloadStatus.DOWNLOAD_NOT_STARTED)).limit(1)
				.list();
		if (list == null || list.size() <= 0) {
			return null;
		}
		return list.get(0);
	}
}
