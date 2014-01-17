package com.movie.filmtube.fragment;
import android.view.View;
import android.widget.TextView;
import com.movie.filmtube.data.FilmYoutube;
import com.youtube.bigbang.R;
public class BookmarkDetailFragment extends MyParentFragment {

	private TextView tvBookmark;
	private FilmYoutube mFilm;

	@Override
	protected void init(View view) {
		tvBookmark = (TextView) view.findViewById(R.id.tvBookmark);
		showBookmarkFromFilm();
	}
	public static BookmarkDetailFragment newInstance(FilmYoutube film) {
		BookmarkDetailFragment bookmarkDetailFragment = new BookmarkDetailFragment();
		bookmarkDetailFragment.setmFilm(film);
		return bookmarkDetailFragment;
	}
	private void showBookmarkFromFilm() {
		if (mFilm != null && tvBookmark != null) {
			String text = mFilm.getIsBookmarked() ? context
					.getString(R.string.is_bookmarked) : context
					.getString(R.string.is_not_bookmarked);
			tvBookmark.setText(text);
		}
	}
	@Override
	protected int getLayoutId() {
		return R.layout.fragment_book_mark;
	}
	public void setmFilm(FilmYoutube mFilm) {
		this.mFilm = mFilm;
		showBookmarkFromFilm();
	}

}
