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
import com.movie.filmtube.adapters.DownloadChapterAdapter;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.FilmYoutubeDao;
import com.movie.filmtube.utils.Constants;
import com.movie.filmtube.utils.DownloadStatus;
import com.movie.filmtube.utils.MyDatabaseHelper;
import com.movie.filmtube.utils.Utility;
import com.youtube.bigbang.BuildConfig;
import com.youtube.bigbang.R;
public class DownloadListFragment extends MyParentFragment implements
		OnItemClickListener {
	private MyDatabaseHelper databaseHelper;
	private FilmYoutubeDao mFilmYoutubeDao;
	private AbsListView lvList;
	private DownloadChapterAdapter mAdapter;
	@Override
	protected void init(View view) {
		databaseHelper = MyDatabaseHelper.getInstance(context);
		mFilmYoutubeDao = databaseHelper.getmSession().getFilmYoutubeDao();
		lvList = (AbsListView) view.findViewById(R.id.lvList);
		if (mAdapter == null) {
			mAdapter = new DownloadChapterAdapter(context,
					new ArrayList<FilmYoutube>());
		}
		if (lvList instanceof GridView) {
			((GridView) lvList).setAdapter(mAdapter);
		} else if (lvList instanceof ListView) {
			((ListView) lvList).setAdapter(mAdapter);
		}
		lvList.setOnItemClickListener(this);
	}
	@Override
	protected int getLayoutId() {
		return R.layout.fragment_list_films;
	}
	public static DownloadListFragment newInstance() {
		return new DownloadListFragment();
	}
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Constants.ACTION_DOWNLOAD_VIDEO.equals(action)) {
				String chapterId = intent
						.getStringExtra(Constants.KEY_CHAPTER_UPDATE);
				FilmYoutube filmYoutube = mAdapter.getItemBasedOnId(chapterId);
				if (filmYoutube != null) {
					int downloadStatus = intent.getIntExtra(
							Constants.KEY_STATUS_UPDATE, 0);
					int fileSize = intent.getIntExtra(Constants.KEY_FILE_SIZE,
							0);
					filmYoutube.setFileSizeInMB(fileSize);
					filmYoutube.setDownloadStatus(downloadStatus);
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		getAllDownloadChapter();
		registerReceiver();
	}

	@Override
	public void onPause() {
		super.onPause();
		context.unregisterReceiver(broadcastReceiver);
	}
	private void registerReceiver() {
		if (BuildConfig.DEBUG) {
			Log.d(this.getClass().getName(), "registerReceiver");
		}
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_DOWNLOAD_VIDEO);
		context.registerReceiver(broadcastReceiver, filter);
	}
	private void getAllDownloadChapter() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				final List<FilmYoutube> downloadChapters = mFilmYoutubeDao
						.queryBuilder()
						.where(FilmYoutubeDao.Properties.DownloadStatus
								.notEq(DownloadStatus.NOT_DOWNLOADED)).list();
				context.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mAdapter.clear();
						mAdapter.changeDownloadChapterList(downloadChapters);
					}
				});
			}
		}).start();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		FilmYoutube downloadChapter = mAdapter.getItem(arg2);
		ArrayList<FilmYoutube> list = new ArrayList<FilmYoutube>();
		int count = mAdapter.getCount();
		for (int i = 0; i < count; i++) {
			list.add(mAdapter.getItem(i));
		}
		Utility.startFilmDetailActivity(context, downloadChapter, list);
	}

	public void searchDownloadedVideos(String text) {
		if (mAdapter != null) {
			mAdapter.getFilter().filter(text);
		}
	}
}
