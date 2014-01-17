package com.movie.filmtube.adapters;
import java.io.File;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.FilmYoutubeDao;
import com.movie.filmtube.services.DownloadThread;
import com.movie.filmtube.utils.Constants;
import com.movie.filmtube.utils.DownloadStatus;
import com.movie.filmtube.utils.MyDatabaseHelper;
import com.movie.filmtube.utils.Utility;
import com.movie.filmtube.utils.caching.ImageLoadingHelper;
import com.movie.filmtube.utils.caching.ImageViewForCaching;
import com.youtube.bigbang.BuildConfig;
import com.youtube.bigbang.R;

public class RelatedFilmAdapter extends ArrayAdapter<FilmYoutube> {

	private LayoutInflater mLayoutInflater;
	private ImageLoadingHelper mImageLoadingHelper;
	private MyDatabaseHelper databaseHelper;

	private int playingPos = 0;
	public void setPlayingPos(int pos) {
		playingPos = pos;
	}
	public FilmYoutube getFilmBasedOnVideoId(String videoId) {
		int count = getCount();
		for (int i = 0; i < count; i++) {
			if (getItem(i).getYoutubeId().equals(videoId)) {
				return getItem(i);
			}
		}
		return null;
	}

	public RelatedFilmAdapter(Activity context, List<FilmYoutube> objects) {
		super(context, R.layout.item_related_films_list, objects);
		mLayoutInflater = context.getLayoutInflater();
		int imageWidth = context.getResources().getDimensionPixelSize(
				R.dimen.image_chapter_width);
		int imageHeight = context.getResources().getDimensionPixelSize(
				R.dimen.image_chapter_height);
		mImageLoadingHelper = new ImageLoadingHelper(
				context.getApplicationContext(), 16, imageWidth, imageHeight);
		databaseHelper = MyDatabaseHelper.getInstance(context);
	}

	private class ViewHolder {
		public ImageViewForCaching ivIcon;
		public ProgressBar pbLoading;
		public Button btnDownload;
		public TextView tvName;
		public TextView tvPercent;
		public ProgressBar pbPercentDownloaded;
		public TextView tvFileSize;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = mLayoutInflater.inflate(
					R.layout.item_related_films_list, null);
			viewHolder.ivIcon = (ImageViewForCaching) convertView
					.findViewById(R.id.ivIcon);
			viewHolder.pbLoading = (ProgressBar) convertView
					.findViewById(R.id.pbLoading);
			viewHolder.ivIcon.setTag(viewHolder.pbLoading);
			viewHolder.btnDownload = (Button) convertView
					.findViewById(R.id.btnDownload);
			viewHolder.btnDownload.setFocusable(false);
			viewHolder.tvName = (TextView) convertView
					.findViewById(R.id.tvChapterName);
			viewHolder.tvPercent = (TextView) convertView
					.findViewById(R.id.tvDownloadPercent);
			viewHolder.pbPercentDownloaded = (ProgressBar) convertView
					.findViewById(R.id.pbDownloadedPercent);
			viewHolder.tvFileSize = (TextView) convertView
					.findViewById(R.id.tvFileSize);
			convertView.setTag(viewHolder);

		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		final FilmYoutube filmYoutube = getItem(position);
		viewHolder.tvName.setText(filmYoutube.getName());
		mImageLoadingHelper.loadImageView(viewHolder.ivIcon,
				filmYoutube.getThumbnailUrl());
		if (playingPos == position) {
			viewHolder.tvName.setTextColor(getContext().getResources()
					.getColor(R.color.blue_light));
		} else {
			viewHolder.tvName.setTextColor(getContext().getResources()
					.getColor(R.color.black));
		}
		File file = new File(Utility.getFilmLocalPathFromUrl(getContext(),
				filmYoutube.getYoutubeId()));
		if (file.exists()) {
			viewHolder.btnDownload.setVisibility(View.INVISIBLE);
			filmYoutube.setDownloadStatus(DownloadStatus.DOWNLOAD_SUCCESS);
		} else {
			viewHolder.btnDownload.setVisibility(View.VISIBLE);
		}
		int downloadStatus = filmYoutube.getDownloadStatus().intValue();
		switch (downloadStatus) {
		case DownloadStatus.NOT_DOWNLOADED:
		case DownloadStatus.DOWNLOAD_FAILED:
			viewHolder.tvPercent.setVisibility(View.GONE);
			viewHolder.pbPercentDownloaded.setVisibility(View.GONE);
			viewHolder.btnDownload.setVisibility(View.VISIBLE);
			viewHolder.tvFileSize.setVisibility(View.GONE);
			break;
		case DownloadStatus.DOWNLOAD_NOT_STARTED:
			viewHolder.tvPercent.setVisibility(View.VISIBLE);
			viewHolder.pbPercentDownloaded.setVisibility(View.VISIBLE);
			viewHolder.tvPercent.setText("Not started");
			viewHolder.btnDownload.setVisibility(View.INVISIBLE);
			viewHolder.pbPercentDownloaded.setMax(100);
			viewHolder.pbPercentDownloaded.setProgress(0);
			break;
		default:
			viewHolder.tvPercent.setVisibility(View.VISIBLE);
			viewHolder.pbPercentDownloaded.setVisibility(View.VISIBLE);
			viewHolder.tvPercent.setText(downloadStatus + "%");
			viewHolder.pbPercentDownloaded.setMax(100);
			viewHolder.pbPercentDownloaded.setProgress(downloadStatus);
			viewHolder.btnDownload.setVisibility(View.INVISIBLE);
			viewHolder.tvFileSize.setVisibility(View.VISIBLE);
			viewHolder.tvFileSize.setText("");
			break;
		}

		viewHolder.btnDownload.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialogDownloadChapter(filmYoutube);
			}
		});

		return convertView;
	}
	private void insertOrUpdateUsingThread(final FilmYoutube filmYoutube,
			final FilmYoutubeDao filmYoutubeDao) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				List<FilmYoutube> list = filmYoutubeDao
						.queryBuilder()
						.where(FilmYoutubeDao.Properties.YoutubeId
								.eq(filmYoutube.getYoutubeId())).limit(1)
						.list();
				if (list.size() > 0) {
					FilmYoutube film = list.get(0);
					filmYoutube.setId(film.getId());
					if (film.getDownloadStatus().intValue() >= DownloadStatus.DOWNLOADING) {
					} else {
						if (BuildConfig.DEBUG) {
							Log.i("RelatedFilm", "Update film");
						}
						filmYoutube
								.setDownloadStatus(DownloadStatus.DOWNLOAD_NOT_STARTED);
						filmYoutubeDao.update(filmYoutube);
						sendBroadcastDownloadFilm(filmYoutube,
								DownloadStatus.DOWNLOAD_NOT_STARTED);
					}
				} else {
					if (BuildConfig.DEBUG) {
						Log.i("RelatedFilm", "Insert Film");
					}
					filmYoutube.setId(null);
					filmYoutube
							.setDownloadStatus(DownloadStatus.DOWNLOAD_NOT_STARTED);
					long id = filmYoutubeDao.insert(filmYoutube);
					filmYoutube.setId(id);
					sendBroadcastDownloadFilm(filmYoutube,
							DownloadStatus.DOWNLOAD_NOT_STARTED);
				}
				if (!DownloadThread.isDownloading) {
					DownloadThread downloadThread = DownloadThread
							.getInstance(getContext());
					downloadThread.start();
				}

			}
		}).start();
	}

	private void sendBroadcastDownloadFilm(FilmYoutube filmYoutube, int status) {
		Intent intent = new Intent(Constants.ACTION_DOWNLOAD_VIDEO);
		intent.putExtra(Constants.KEY_STATUS_UPDATE, status);
		intent.putExtra(Constants.KEY_CHAPTER_UPDATE,
				filmYoutube.getYoutubeId());
		getContext().sendBroadcast(intent);
	}
	private void showDialogDownloadChapter(final FilmYoutube filmYoutube) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		final FilmYoutubeDao filmYoutubeDao = databaseHelper.getmSession()
				.getFilmYoutubeDao();
		builder.setTitle("Save!");
		builder.setMessage("Do you want to save video: "
				+ filmYoutube.getName());
		builder.setPositiveButton("Save",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Toast.makeText(getContext(),
								"Video is added to saving queue.",
								Toast.LENGTH_SHORT).show();
						insertOrUpdateUsingThread(filmYoutube, filmYoutubeDao);
						notifyDataSetChanged();
					}
				});
		builder.setNegativeButton("Cancel", null);
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}
}
