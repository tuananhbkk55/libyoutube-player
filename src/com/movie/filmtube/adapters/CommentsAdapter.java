package com.movie.filmtube.adapters;
import java.util.ArrayList;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.movie.filmtube.utils.caching.ImageLoadingHelper;
import com.movie.filmtube.utils.caching.ImageViewForCaching;
import com.movie.filmtube.utils.movie.world.helper.MovieComment;
import com.youtube.bigbang.R;
public class CommentsAdapter extends ArrayAdapter<MovieComment> {
	private LayoutInflater mLayoutInflater;
	private ImageLoadingHelper mImageLoadingHelper;
	public CommentsAdapter(Activity context, ArrayList<MovieComment> objects) {
		super(context, R.layout.item_comment, objects);
		mLayoutInflater = context.getLayoutInflater();
		int imageWidth = context.getResources().getDimensionPixelSize(
				R.dimen.image_chapter_width);
		int imageHeight = context.getResources().getDimensionPixelSize(
				R.dimen.image_chapter_height);
		mImageLoadingHelper = new ImageLoadingHelper(
				context.getApplicationContext(), 8, imageWidth, imageHeight);
		mImageLoadingHelper
				.setDefaultLoadedImageId(R.drawable.default_user_avatar);
	}
	private class ViewHolder {
		ImageViewForCaching ivIcon;
		ProgressBar progressBar;
		TextView tvName;
		TextView tvComment;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = mLayoutInflater.inflate(R.layout.item_comment, null);
			viewHolder.ivIcon = (ImageViewForCaching) convertView
					.findViewById(R.id.ivIcon);
			viewHolder.progressBar = (ProgressBar) convertView
					.findViewById(R.id.pbLoading);
			viewHolder.ivIcon.setTag(viewHolder.progressBar);
			viewHolder.tvComment = (TextView) convertView
					.findViewById(R.id.tvComment);
			viewHolder.tvName = (TextView) convertView
					.findViewById(R.id.tvName);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		MovieComment youtubeComment = getItem(position);
		mImageLoadingHelper.loadImageView(viewHolder.ivIcon,
				youtubeComment.getAvatarUrl());
		viewHolder.tvComment.setText(youtubeComment.getComment());
		viewHolder.tvName.setText(youtubeComment.getUserName());
		return convertView;
	}
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		mImageLoadingHelper.clearCache();
	}
}
