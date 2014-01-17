package com.movie.filmtube.fragment;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListView;
import com.movie.filmtube.adapters.LocalFilmListAdapter;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.utils.Utility;
import com.youtube.bigbang.BuildConfig;
import com.youtube.bigbang.R;
public class LocalFilmListFragment extends MyParentFragment implements
		OnItemClickListener {
	private AbsListView lvFilms;
	private LocalFilmListAdapter mAdapter;
	private List<FilmYoutube> list = new ArrayList<FilmYoutube>();

	@Override
	protected void init(View view) {
		lvFilms = (AbsListView) view.findViewById(R.id.lvList);
		if (mAdapter == null) {
			mAdapter = new LocalFilmListAdapter(context, list);
		}
		if (lvFilms instanceof GridView) {
			((GridView) lvFilms).setAdapter(mAdapter);
		} else if (lvFilms instanceof ListView) {
			((ListView) lvFilms).setAdapter(mAdapter);
		}
		lvFilms.setOnItemClickListener(this);

	}

	public static LocalFilmListFragment newInstance() {
		return new LocalFilmListFragment();
	}
	public boolean isFilmListEmpty() {
		if (this.mAdapter != null && mAdapter.getCount() > 0) {
			return false;
		}
		return true;
	}
	public void setFilmsList(List<FilmYoutube> list) {
		this.list = list;
		if (mAdapter != null) {
			mAdapter.changeFilmList(list);
		}
	}

	public void searchFilmList(String text) {
		if (mAdapter != null) {
			mAdapter.getFilter().filter(text);
		}
	}
	@Override
	protected int getLayoutId() {
		return R.layout.fragment_list_films;
	}
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		final FilmYoutube film = mAdapter.getItem(arg2);
		if (BuildConfig.DEBUG) {
			Log.d(this.getClass().getName(), film.toString());
		}
		ArrayList<FilmYoutube> list = new ArrayList<FilmYoutube>();
		int size = mAdapter.getCount();
		for (int i = 0; i < size; i++) {
			list.add(mAdapter.getItem(i));
		}
		Utility.startFilmDetailActivity(context, film, list);
	}
}
