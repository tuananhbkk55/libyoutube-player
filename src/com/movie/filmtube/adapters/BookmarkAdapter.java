package com.movie.filmtube.adapters;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.utils.OnBookmarkListener;
import com.movie.filmtube.utils.Utility;
import com.movie.filmtube.utils.caching.ImageLoadingHelper;
import com.movie.filmtube.utils.caching.ImageViewForCaching;
import com.youtube.bigbang.R;

public class BookmarkAdapter extends ArrayAdapter<FilmYoutube> implements
		OnBookmarkListener {
	private LayoutInflater mLayoutInflater;
	private ImageLoadingHelper mImageLoadingHelper;
	private ArrayList<FilmYoutube> backupData = new ArrayList<FilmYoutube>();
	private Activity activity;
	public BookmarkAdapter(Activity context, List<FilmYoutube> objects) {
		super(context, R.layout.item_bookmarks_list, objects);
		activity = context;
		mLayoutInflater = context.getLayoutInflater();
		int imageWidth = context.getResources().getDimensionPixelSize(
				R.dimen.image_film_width);
		int imageHeight = context.getResources().getDimensionPixelSize(
				R.dimen.image_film_height);
		mImageLoadingHelper = new ImageLoadingHelper(
				context.getApplicationContext(), 32, imageWidth, imageHeight);
		mImageLoadingHelper.setDiskCachEnable(true);
		for (FilmYoutube filmYoutube : objects) {
			backupData.add(filmYoutube);
		}
	}

	@Override
	public Filter getFilter() {
		return new Filter() {
			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				clear();
				for (FilmYoutube bookmark : (ArrayList<FilmYoutube>) results.values) {
					add(bookmark);
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
					for (FilmYoutube bookmark : backupData) {
						if (!TextUtils.isEmpty(bookmark.getName())
								&& bookmark.getName().toLowerCase()
										.contains(searchedText)) {
							list.add(bookmark);
						}
					}
					filterResults.count = list.size();
					filterResults.values = list;
				}
				return filterResults;
			}
		};
	}
	public void changeBookmarkList(List<FilmYoutube> list) {
		backupData.clear();		
		for (FilmYoutube Bookmark : list) {
			backupData.add(Bookmark);
			add(Bookmark);
		}
		notifyDataSetChanged();
	}	private class ViewHolder {
		public ImageViewForCaching ivIcon;
		public ProgressBar progressBar;
		public TextView tvFilmName;
		public TextView tvlastTime;
		public View btnCancel;

	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			convertView = mLayoutInflater.inflate(R.layout.item_bookmarks_list,
					null);
			viewHolder = new ViewHolder();
			viewHolder.ivIcon = (ImageViewForCaching) convertView
					.findViewById(R.id.ivIcon);
			viewHolder.progressBar = (ProgressBar) convertView
					.findViewById(R.id.pbLoading);
			viewHolder.ivIcon.setTag(viewHolder.progressBar);
			viewHolder.tvFilmName = (TextView) convertView
					.findViewById(R.id.tvName);
			viewHolder.tvlastTime = (TextView) convertView
					.findViewById(R.id.tvLastPlayedTime);
			viewHolder.btnCancel = convertView.findViewById(R.id.btnCancel);
			viewHolder.btnCancel.setFocusable(false);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		final FilmYoutube filmYoutube = getItem(position);
		mImageLoadingHelper.loadImageView(viewHolder.ivIcon,
				filmYoutube.getThumbnailUrl());
		viewHolder.tvFilmName.setText(filmYoutube.getName());

		if (filmYoutube.getLastPlayedTime().intValue() > 0) {
			viewHolder.tvlastTime.setVisibility(View.VISIBLE);
			viewHolder.tvlastTime
					.setText(Utility.convertTimeToDisplayedTime(filmYoutube
							.getLastPlayedTime()));

		} else {
			viewHolder.tvlastTime.setVisibility(View.GONE);
		}
		viewHolder.btnCancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Utility.showDeleteBookmarkDialog(activity, filmYoutube,
						BookmarkAdapter.this);
			}
		});
		return convertView;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		mImageLoadingHelper.clearCache();
	}

	@Override
	public void onBookmarkedSucceed(FilmYoutube filmYoutube) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});
	}
	@Override
	public void onBookmarkDeleted(FilmYoutube filmYoutube) {
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});
	}

}
