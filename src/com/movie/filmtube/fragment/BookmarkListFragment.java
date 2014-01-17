package com.movie.filmtube.fragment;
import java.util.ArrayList;
import java.util.List;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListView;
import com.movie.filmtube.adapters.BookmarkAdapter;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.FilmYoutubeDao;
import com.movie.filmtube.utils.Constants;
import com.movie.filmtube.utils.MyDatabaseHelper;
import com.movie.filmtube.utils.Utility;
import com.youtube.bigbang.BuildConfig;
import com.youtube.bigbang.R;

public class BookmarkListFragment extends MyParentFragment implements
		OnItemClickListener {
	private MyDatabaseHelper databaseHelper;
	private AbsListView lvList;
	private BookmarkAdapter mAdapter;
	@Override
	protected void init(View view) {
		databaseHelper = MyDatabaseHelper.getInstance(context);
		lvList = (AbsListView) view.findViewById(R.id.lvList);
		if (mAdapter == null) {
			mAdapter = new BookmarkAdapter(context,
					new ArrayList<FilmYoutube>());
		}
		if (lvList instanceof GridView) {
			((GridView) lvList).setAdapter(mAdapter);
		} else if (lvList instanceof ListView) {
			((ListView) lvList).setAdapter(mAdapter);
		}
		lvList.setOnItemClickListener(this);
		loadAllBookmarks();
		if (adView == null || adView.getVisibility() != View.VISIBLE) {
			createAdmob(view);
		}
	}
	private void registerReceivers() {
		isRegister = true;
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_RELOAD_BOOKMARKS);
		context.registerReceiver(mBroadcastReceiver, filter);
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Constants.ACTION_RELOAD_BOOKMARKS.equals(action)) {
				loadAllBookmarks();
			}
		}
	};

	private boolean isRegister = false;

	@Override
	public void onResume() {
		super.onResume();
		registerReceivers();
		loadAllBookmarks();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (isRegister) {
			isRegister = false;
			context.unregisterReceiver(mBroadcastReceiver);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_bookmark_list;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		ArrayList<FilmYoutube> list = new ArrayList<FilmYoutube>();
		int size = mAdapter.getCount();
		for (int i = 0; i < size; i++) {
			list.add(mAdapter.getItem(i));
		}
		Utility.startFilmDetailActivity(context, mAdapter.getItem(arg2), list);
	}

	public void searchFilm(String filmName) {
		if (mAdapter != null) {
			mAdapter.getFilter().filter(filmName);
		}
	}

	public static BookmarkListFragment newInstance() {
		return new BookmarkListFragment();
	}

	private void loadAllBookmarks() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				FilmYoutubeDao filmYoutubeDao = databaseHelper.getmSession()
						.getFilmYoutubeDao();
				final List<FilmYoutube> result = filmYoutubeDao.queryBuilder()
						.where(FilmYoutubeDao.Properties.IsBookmarked.eq(true))
						.list();
				context.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mAdapter.changeBookmarkList(result);
					}
				});
			}
		}).start();
	}

}
