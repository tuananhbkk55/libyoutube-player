package com.movie.filmtube.adapters;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.services.DownloadThread;
import com.movie.filmtube.utils.DownloadStatus;
import com.movie.filmtube.utils.caching.ImageLoadingHelper;
import com.movie.filmtube.utils.caching.ImageViewForCaching;
import com.youtube.bigbang.R;
public class DownloadChapterAdapter extends ArrayAdapter<FilmYoutube> {
	private LayoutInflater mInflater;
	private ImageLoadingHelper mImageLoadingHelper;
	private ArrayList<FilmYoutube> filterList = new ArrayList<FilmYoutube>();
	private ArrayList<FilmYoutube> backupData = new ArrayList<FilmYoutube>();
	public DownloadChapterAdapter(Activity context, List<FilmYoutube> objects) {
		super(context, R.layout.item_download_film_list, objects);
		mInflater = context.getLayoutInflater();
		int imageWidth = context.getResources().getDimensionPixelSize(
				R.dimen.image_film_width);
		int imageHeight = context.getResources().getDimensionPixelSize(
				R.dimen.image_film_height);
		mImageLoadingHelper = new ImageLoadingHelper(
				context.getApplicationContext(), 8, imageWidth, imageHeight);
		mImageLoadingHelper.setDiskCachEnable(true);
		for (FilmYoutube filmYoutube : objects) {
			filterList.add(filmYoutube);
			backupData.add(filmYoutube);
		}
	}

	public FilmYoutube getItemBasedOnId(String youtubeId) {
		int size = getCount();
		for (int i = 0; i < size; i++) {
			if (getItem(i).getYoutubeId().equals(youtubeId)) {
				return getItem(i);
			}
		}
		return null;
	}
	@Override
	public Filter getFilter() {
		return new Filter() {

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				filterList = (ArrayList<FilmYoutube>) results.values;
				clear();
				for (FilmYoutube downloadChapter : filterList) {
					add(downloadChapter);
				}
				notifyDataSetChanged();
			}
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults filterResults = new FilterResults();
				if (TextUtils.isEmpty(constraint)
						|| TextUtils.isEmpty(constraint.toString().trim())) {
					filterResults.count = backupData.size();
					filterResults.values = backupData.clone();
				} else {
					String searchedText = constraint.toString().toLowerCase();
					ArrayList<FilmYoutube> list = new ArrayList<FilmYoutube>();					
					for (FilmYoutube filmTube : backupData) {
						if (!TextUtils.isEmpty(filmTube.getName())
								&& filmTube.getName().toLowerCase()
										.contains(searchedText)) {
							list.add(filmTube);
						}
					}
					filterResults.count = list.size();
					filterResults.values = list;
				}
				return filterResults;
			}
		};
	}

	public void changeDownloadChapterList(List<FilmYoutube> list) {
		filterList.clear();
		backupData.clear();
		clear();
		for (FilmYoutube downloadChapter : list) {
			filterList.add(downloadChapter);
			backupData.add(downloadChapter);
			add(downloadChapter);
		}
		notifyDataSetChanged();
	}

	private static class ViewHolder {
		public ImageViewForCaching ivIcon;
		public ProgressBar progressBar;
		public TextView tvChapterName;
		public TextView tvPercent;
		public ProgressBar pbDownloadingPercent;
		public Button btnCancel;
		public TextView tvFileSize;

	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.item_download_film_list,
					null);
			viewHolder = new ViewHolder();
			viewHolder.ivIcon = (ImageViewForCaching) convertView
					.findViewById(R.id.ivIcon);
			viewHolder.progressBar = (ProgressBar) convertView
					.findViewById(R.id.pbLoading);
			viewHolder.ivIcon.setTag(viewHolder.progressBar);
			viewHolder.tvChapterName = (TextView) convertView
					.findViewById(R.id.tvChapterName);
			viewHolder.tvPercent = (TextView) convertView
					.findViewById(R.id.tvDownloadPercent);
			viewHolder.pbDownloadingPercent = (ProgressBar) convertView
					.findViewById(R.id.pbDownloadedPercent);
			viewHolder.btnCancel = (Button) convertView
					.findViewById(R.id.btnCancel);
			viewHolder.tvFileSize = (TextView) convertView
					.findViewById(R.id.tvFileSize);
			viewHolder.btnCancel.setFocusable(false);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		final FilmYoutube filmYoutube = getItem(position);
		mImageLoadingHelper.loadImageView(viewHolder.ivIcon,
				filmYoutube.getThumbnailUrl());
		viewHolder.tvChapterName.setText(filmYoutube.getName());
		if (filmYoutube.getDownloadStatus() >= DownloadStatus.DOWNLOAD_SUCCESS) {
			viewHolder.tvPercent.setText("100%");
			viewHolder.pbDownloadingPercent.setProgress(100);
		} else if (filmYoutube.getDownloadStatus() == DownloadStatus.DOWNLOAD_FAILED) {
			viewHolder.tvPercent.setText("Failed");
			viewHolder.pbDownloadingPercent.setProgress(0);
		} else if (filmYoutube.getDownloadStatus() == DownloadStatus.DOWNLOAD_NOT_STARTED) {
			viewHolder.tvPercent.setText("Not started");
			viewHolder.pbDownloadingPercent.setProgress(0);
		} else {
			viewHolder.tvPercent.setText(filmYoutube.getDownloadStatus() + "%");
			viewHolder.pbDownloadingPercent.setProgress(filmYoutube
					.getDownloadStatus());
		}
		if (filmYoutube.getFileSizeInMB() != null
				&& filmYoutube.getFileSizeInMB().intValue() > 0) {
			viewHolder.tvFileSize.setVisibility(View.VISIBLE);
			viewHolder.tvFileSize.setText(filmYoutube.getFileSizeInMB() + "MB");
		} else {
			viewHolder.tvFileSize.setVisibility(View.GONE);
		}

		viewHolder.btnCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showCancelDownloadDialog(filmYoutube);
			}
		});
		return convertView;
	}
	private void showCancelDownloadDialog(final FilmYoutube filmYoutube) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle("Cancel");
		String message = "";
		if (filmYoutube.getDownloadStatus() >= DownloadStatus.DOWNLOAD_SUCCESS) {
			message = "Do you want to delete saved video: "					+ filmYoutube.getName() + "?";
		} else {
			message = "Do you want to cancel saving video: "				+ filmYoutube.getName() + "?";
		}
		builder.setMessage(message);
		builder.setPositiveButton(getContext().getString(R.string.yes_label),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						DownloadThread downloadThread = DownloadThread
								.getInstance(getContext());
						downloadThread.cancelDownloadChapter(filmYoutube);
						remove(filmYoutube);
						notifyDataSetChanged();
					}
				});
		builder.setNegativeButton(getContext().getString(R.string.no_label),
				null);
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}
}
