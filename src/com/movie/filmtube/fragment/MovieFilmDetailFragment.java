package com.movie.filmtube.fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import com.movie.filmtube.data.FilmYoutube;
import com.youtube.bigbang.R;
public class MovieFilmDetailFragment extends MyParentFragment {
	private TextView tvTotalViews;
	private TextView tvLike;
	private TextView tvDislike;
	private TextView tvDescription;
	private FilmYoutube filmYoutube;
	@Override
	protected void init(View view) {
		tvDescription = (TextView) view.findViewById(R.id.tvDescription);
		tvLike = (TextView) view.findViewById(R.id.tvLikeCount);
		tvDislike = (TextView) view.findViewById(R.id.tvDislikeCount);
		tvTotalViews = (TextView) view.findViewById(R.id.tvTotalViews);
		tvDescription.setMovementMethod(new ScrollingMovementMethod());
		if (filmYoutube != null) {
			showFilmInformation();
		}
	}
	private void showFilmInformation() {
		tvDescription.setText(this.filmYoutube.getInformation());
		tvDislike.setText("" + this.filmYoutube.getDislikeCount());
		tvLike.setText("" + this.filmYoutube.getLikeCount());
		tvTotalViews.setText("" + this.filmYoutube.getViewCount() + " views");
	}
	@Override
	protected int getLayoutId() {
		return R.layout.fragment_information;
	}
	public static MovieFilmDetailFragment newInstance(FilmYoutube filmYoutube) {
		MovieFilmDetailFragment youtubeFilmDetailFragment = new MovieFilmDetailFragment();
		youtubeFilmDetailFragment.setFilmYoutube(filmYoutube);
		return youtubeFilmDetailFragment;
	}
	public void setFilmYoutube(FilmYoutube filmYoutube) {
		this.filmYoutube = filmYoutube;
		if (tvDescription != null) {
			showFilmInformation();
		}
	}

}
