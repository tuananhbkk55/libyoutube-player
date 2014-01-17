package com.movie.filmtube.adapters;
import java.util.ArrayList;
import java.util.Iterator;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.utils.OnBookmarkListener;
import com.movie.filmtube.utils.Utility;
import com.movie.filmtube.utils.caching.ImageLoadingHelper;
import com.movie.filmtube.utils.caching.ImageViewForCaching;
import com.movie.filmtube.utils.searching.AhoCorasick;
import com.youtube.bigbang.R;

public class MovieWorldAdapter extends ArrayAdapter<FilmYoutube> implements
		OnBookmarkListener {

	private String bookmarkVideos = "";
	private AhoCorasick ahoCorasick;

	private LayoutInflater mLayoutInflater;
	private ImageLoadingHelper mImageLoadingHelper;
	private Activity activity;

	public MovieWorldAdapter(Activity context, ArrayList<FilmYoutube> objects) {
		super(context, R.layout.item_movie_world_list, objects);
		mLayoutInflater = context.getLayoutInflater();
		int imageWidth = context.getResources().getDimensionPixelSize(
				R.dimen.image_film_width);
		int imageHeight = context.getResources().getDimensionPixelSize(
				R.dimen.image_film_height);
		mImageLoadingHelper = new ImageLoadingHelper(
				context.getApplicationContext(), 8, imageWidth, imageHeight);
		this.activity = context;
	}

	private class ViewHolder {
		ImageViewForCaching imageViewForCaching;
		ProgressBar progressBar;
		TextView tvFilmName;
		Button btnBookmark;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			convertView = mLayoutInflater.inflate(
					R.layout.item_movie_world_list, null);
			viewHolder = new ViewHolder();
			viewHolder.btnBookmark = (Button) convertView
					.findViewById(R.id.btnBookmarks);
			viewHolder.btnBookmark.setFocusable(false);
			viewHolder.progressBar = (ProgressBar) convertView
					.findViewById(R.id.pbLoading);
			viewHolder.imageViewForCaching = (ImageViewForCaching) convertView
					.findViewById(R.id.ivIcon);
			viewHolder.imageViewForCaching.setTag(viewHolder.progressBar);
			viewHolder.tvFilmName = (TextView) convertView
					.findViewById(R.id.tvName);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		final FilmYoutube filmYoutube = getItem(position);
		mImageLoadingHelper.loadImageView(viewHolder.imageViewForCaching,
				filmYoutube.getThumbnailUrl());
		viewHolder.tvFilmName.setText(filmYoutube.getName());
		ahoCorasick = new AhoCorasick();
		ahoCorasick.add(filmYoutube.getYoutubeId().getBytes(),
				filmYoutube.getYoutubeId());
		ahoCorasick.prepare();
		Iterator searcher = ahoCorasick.search(bookmarkVideos.getBytes());
		if (searcher.hasNext()) {
			filmYoutube.setIsBookmarked(true);
		} else {
			filmYoutube.setIsBookmarked(false);
		}
		if (filmYoutube.getIsBookmarked()) {
			viewHolder.btnBookmark
					.setBackgroundResource(R.drawable.ic_flag_focus);
			viewHolder.btnBookmark.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Utility.showDeleteBookmarkDialog(activity, filmYoutube,
							MovieWorldAdapter.this);
				}
			});
		} else {
			viewHolder.btnBookmark.setBackgroundResource(R.drawable.ic_flag);
			viewHolder.btnBookmark.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Utility.showBookmarkDialog(activity, filmYoutube,
							MovieWorldAdapter.this);
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

	public void setBookmarkVideos(String bookmarkVideos) {
		this.bookmarkVideos = bookmarkVideos;
	}
}
