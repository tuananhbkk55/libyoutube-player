package com.movie.filmtube.adapters;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import android.app.Activity;
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
import com.movie.filmtube.utils.OnBookmarkListener;
import com.movie.filmtube.utils.Utility;
import com.movie.filmtube.utils.caching.ImageLoadingHelper;
import com.movie.filmtube.utils.caching.ImageViewForCaching;
import com.movie.filmtube.utils.searching.AhoCorasick;
import com.youtube.bigbang.BuildConfig;
import com.youtube.bigbang.R;

public class LocalFilmListAdapter extends ArrayAdapter<FilmYoutube> implements
		OnBookmarkListener {
	private LayoutInflater mLayoutInflater;
	private ImageLoadingHelper mImageLoadingHelper;
	private ArrayList<FilmYoutube> backupData = new ArrayList<FilmYoutube>();
	private Activity activity;
	public LocalFilmListAdapter(Activity context, List<FilmYoutube> objects) {
		super(context, R.layout.item_film_list, objects);
		mLayoutInflater = context.getLayoutInflater();
		int imageWidth = context.getResources().getDimensionPixelSize(
				R.dimen.image_chapter_width);
		int imageHeight = context.getResources().getDimensionPixelSize(
				R.dimen.image_chapter_height);
		mImageLoadingHelper = new ImageLoadingHelper(
				context.getApplicationContext(), 8, imageWidth, imageHeight);
		mImageLoadingHelper.setDiskCachEnable(true);
		this.activity = context;
		changeFilmList(objects);
	}

	@Override
	public Filter getFilter() {
		return new Filter() {

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				try {					
					for (FilmYoutube film : (ArrayList<FilmYoutube>) results.values) {
						add(film);
					}
					notifyDataSetChanged();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults filterResults = new FilterResults();
				if (TextUtils.isEmpty(constraint)
						|| TextUtils.isEmpty(constraint.toString().trim())) {
					filterResults.count = backupData.size();
					filterResults.values = backupData.clone();
				} else {
					try {
						String searchedText = constraint.toString()
								.toLowerCase();
						ArrayList<FilmYoutube> list = new ArrayList<FilmYoutube>();
						ahoCorasick = new AhoCorasick();
						ahoCorasick.add(searchedText.getBytes(), searchedText);
						ahoCorasick.prepare();
						int i = -1;
						for (FilmYoutube film : backupData) {
							i++;
							Iterator searcher = ahoCorasick
									.search(mFilmNamesbLowerCase.get(i)
											.getBytes());
							if (searcher.hasNext()) {
								list.add(film);
							}
						}

						filterResults.count = list.size();
						filterResults.values = list;						
						if (BuildConfig.DEBUG) {
							error.printStackTrace();
						}
						filterResults.count = backupData.size();
						filterResults.values = backupData.clone();
					}
				}

				return filterResults;
			}
		};
	}

	
	private AhoCorasick ahoCorasick;
	private List<String> mFilmNamesbLowerCase = new ArrayList<String>();

	public void changeFilmList(List<FilmYoutube> list) {
		mFilmNamesbLowerCase.clear();		
		backupData.clear();
		clear();
		for (FilmYoutube film : list) {
			backupData.add(film);
			mFilmNamesbLowerCase.add(film.getName().toLowerCase());
			add(film);
		}
		notifyDataSetChanged();
	}

	private class ViewHolder {
		public ImageViewForCaching ivIcon;
		public ProgressBar progressBar;
		public TextView tvName;
		public Button btnBookmarks;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			convertView = mLayoutInflater
					.inflate(R.layout.item_film_list, null);
			viewHolder = new ViewHolder();
			viewHolder.ivIcon = (ImageViewForCaching) convertView
					.findViewById(R.id.ivIcon);
			viewHolder.progressBar = (ProgressBar) convertView
					.findViewById(R.id.pbLoading);
			viewHolder.ivIcon.setTag(viewHolder.progressBar);
			viewHolder.tvName = (TextView) convertView
					.findViewById(R.id.tvName);
			viewHolder.btnBookmarks = (Button) convertView
					.findViewById(R.id.btnBookmarks);
			viewHolder.btnBookmarks.setFocusable(false);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		final FilmYoutube film = getItem(position);
		mImageLoadingHelper.loadImageView(viewHolder.ivIcon,
				film.getThumbnailUrl());
		viewHolder.tvName.setText(film.getName());
		if (film.getIsBookmarked()) {
					viewHolder.btnBookmarks
					.setBackgroundResource(R.drawable.ic_flag_focus);
			viewHolder.btnBookmarks.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Utility.showDeleteBookmarkDialog(activity, film,
							LocalFilmListAdapter.this);
				}
			});
		} else {
			viewHolder.btnBookmarks.setBackgroundResource(R.drawable.ic_flag);
			viewHolder.btnBookmarks.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Utility.showBookmarkDialog(activity, film,
							LocalFilmListAdapter.this);
				}
			});

		}

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
