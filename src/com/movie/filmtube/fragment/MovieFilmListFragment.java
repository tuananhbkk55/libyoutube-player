package com.movie.filmtube.fragment;
import java.util.ArrayList;
import java.util.List;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import com.movie.filmtube.adapters.MovieWorldAdapter;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.FilmYoutubeDao;
import com.movie.filmtube.utils.Constants;
import com.movie.filmtube.utils.MyDatabaseHelper;
import com.movie.filmtube.utils.MyPreferenceManager;
import com.movie.filmtube.utils.Utility;
import com.movie.filmtube.utils.movie.world.helper.MovieSearchingHelper;
import com.youtube.bigbang.BuildConfig;
import com.youtube.bigbang.R;
public class MovieFilmListFragment extends MyParentFragment implements
		OnScrollListener, OnItemClickListener {
	private View viewLoadingMore;
	private TextView textView;
	private AbsListView lvList;
	private MovieWorldAdapter mAdapter;
	private MovieSearchingHelper mYoutubeSearchingHelper;
	private MyDatabaseHelper databaseHelper;
	@Override
	protected int getLayoutId() {
		return R.layout.fragment_list_films;
	}
	@Override
	protected void init(View view) {
		databaseHelper = MyDatabaseHelper.getInstance(context);
		viewLoadingMore = context.getLayoutInflater().inflate(
				R.layout.view_loading_more, null);
		textView = (TextView) view.findViewById(R.id.tvSuggest);
		lvList = (AbsListView) view.findViewById(R.id.lvList);
		if (mAdapter == null) {
			mAdapter = new MovieWorldAdapter(context, new MyPreferenceManager(
					context).getLastSerchedVideos());
			textView.setVisibility(View.VISIBLE);
		}
		if (lvList instanceof GridView) {
			((GridView) lvList).setEmptyView(textView);
			((GridView) lvList).setAdapter(mAdapter);
		} else if (lvList instanceof ListView) {
			((ListView) lvList).addFooterView(viewLoadingMore);
			((ListView) lvList).setEmptyView(textView);
			((ListView) lvList).setAdapter(mAdapter);
			((ListView) lvList).removeFooterView(viewLoadingMore);
		}
		mYoutubeSearchingHelper = new MovieSearchingHelper();
		lvList.setOnScrollListener(this);
		lvList.setOnItemClickListener(this);

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
				if (BuildConfig.DEBUG) {
					Log.i(getClass().getName(), "ACTION_RELOAD_BOOKMARKS");
				}
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
	public void onDestroy() {
		super.onDestroy();
		if (mAdapter != null) {
			int count = mAdapter.getCount();
			List<FilmYoutube> list = new ArrayList<FilmYoutube>();
			for (int i = 0; i < count; i++) {
				list.add(mAdapter.getItem(i));
			}
			new MyPreferenceManager(context).setLastSearchedVideos(list);
		}
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
				StringBuilder builder = new StringBuilder();
				if (result != null) {
					for (FilmYoutube filmYoutube : result) {
						builder.append(filmYoutube.getYoutubeId() + ";");
					}
				}
				final String bookmarkVideos = builder.toString();
			
				context.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (mAdapter != null) {
							mAdapter.setBookmarkVideos(bookmarkVideos);
							mAdapter.notifyDataSetChanged();
						}
					}
				});
			}
		}).start();
	}

	private ProgressDialog progressDialog;

	public void searchByKeyword(final String keyword) {
		progressDialog = ProgressDialog.show(context,
				context.getString(R.string.searching_youtube_title),
				context.getString(R.string.searching_youtube_message));
		new Thread(new Runnable() {

			@Override
			public void run() {
				final List<FilmYoutube> list = mYoutubeSearchingHelper
						.searchByKey(keyword);
				context.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mAdapter.clear();
						for (FilmYoutube filmYoutube : list) {
							mAdapter.add(filmYoutube);
						}
						mAdapter.notifyDataSetChanged();
						hideProgressDialog();
					}
				});
			}
		}).start();
	}

	private boolean isLoadingMoreResult = false;
	private void loadMoreResults() {
		if (mYoutubeSearchingHelper == null) {
			return;
		}
		if (mYoutubeSearchingHelper.getTotalSearchResults() <= mAdapter
				.getCount()) {		
			return;
		}
		if (lvList instanceof GridView) {
		} else if (lvList instanceof ListView) {
			((ListView) lvList).addFooterView(viewLoadingMore);
		}
		lvList.post(new Runnable() {

			@Override
			public void run() {
				lvList.smoothScrollToPosition(mAdapter.getCount());
			}
		});
		new Thread(new Runnable() {

			@Override
			public void run() {
				isLoadingMoreResult = true;
				final List<FilmYoutube> list = mYoutubeSearchingHelper
						.searchNextPage();
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						for (FilmYoutube filmYoutube : list) {
							mAdapter.add(filmYoutube);
						}
						mAdapter.notifyDataSetChanged();
						if (lvList instanceof GridView) {
						} else if (lvList instanceof ListView) {
							((ListView) lvList)
									.removeFooterView(viewLoadingMore);
						}
					}
				});
				isLoadingMoreResult = false;
			}
		}).start();
	}

	private void hideProgressDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}
	public static MovieFilmListFragment newInstance() {
		MovieFilmListFragment youtubeFilmListFragment = new MovieFilmListFragment();
		return youtubeFilmListFragment;
	}

	private boolean isLastItemVisible = false;

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		final int lastItem = firstVisibleItem + visibleItemCount;
		if (lastItem == totalItemCount) {
			isLastItemVisible = true;
		} else {
			isLastItemVisible = false;
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (isLastItemVisible && scrollState == SCROLL_STATE_IDLE
				&& mAdapter.getCount() < 500 && !isLoadingMoreResult) {
			loadMoreResults();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, final int arg2,
			long arg3) {
		ArrayList<FilmYoutube> filmYoutubes = new ArrayList<FilmYoutube>();
		int size = mAdapter.getCount();
		for (int i = 0; i < size; i++) {
			filmYoutubes.add(mAdapter.getItem(i));
		}
		Utility.startFilmDetailActivity(context, mAdapter.getItem(arg2),
				filmYoutubes);

	}

}
