package com.movie.filmtube.fragment;
import java.util.ArrayList;
import java.util.List;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.movie.filmtube.adapters.RelatedFilmAdapter;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.utils.Constants;
import com.youtube.bigbang.BuildConfig;
import com.youtube.bigbang.R;

public class MovieRelatedFilmsFragment extends MyParentFragment implements
		OnItemClickListener {

	private ListView lvList;
	private RelatedFilmAdapter mAdapter;
	private OnFilmClickListener onFilmClickListener;
	private int playingPosition = 0;
	private List<FilmYoutube> filmYoutubes = new ArrayList<FilmYoutube>();

	@Override
	protected void init(View view) {
		if (mAdapter == null) {
			mAdapter = new RelatedFilmAdapter(context, filmYoutubes);
		}
		lvList = (ListView) view.findViewById(R.id.lvList);
		lvList.setAdapter(mAdapter);
		lvList.setOnItemClickListener(this);
		scrollToPosition(playingPosition);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_related_film;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		this.playingPosition = arg2;
		mAdapter.setPlayingPos(playingPosition);
		mAdapter.notifyDataSetChanged();
		if (onFilmClickListener != null) {
			onFilmClickListener.onFilmClicked(arg2);
		}
	}

	public static MovieRelatedFilmsFragment newInstance(List<FilmYoutube> list,
			OnFilmClickListener onFilmClickListener, int playingPosition) {
		MovieRelatedFilmsFragment filmsFragment = new MovieRelatedFilmsFragment();
		filmsFragment.setFilmList(list);
		filmsFragment.setOnFilmClickListener(onFilmClickListener);
		filmsFragment.setPlayingPosition(playingPosition);
		return filmsFragment;

	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Constants.ACTION_DOWNLOAD_VIDEO.equals(action)) {				
				String videoId = intent
						.getStringExtra(Constants.KEY_CHAPTER_UPDATE);
				final FilmYoutube film = mAdapter
						.getFilmBasedOnVideoId(videoId);
				if (film != null) {
					int downloadStatus = intent.getIntExtra(
							Constants.KEY_STATUS_UPDATE, 0);
					int fileSize = intent.getIntExtra(Constants.KEY_FILE_SIZE,
							0);
					film.setFileSizeInMB(fileSize);
					film.setDownloadStatus(downloadStatus);
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	};

	@Override
	public void onResume() {
		super.onResume();
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

	private void scrollToPosition(final int pos) {
		lvList.post(new Runnable() {

			@Override
			public void run() {
				lvList.smoothScrollToPosition(pos);
			}
		});
		mAdapter.setPlayingPos(playingPosition);
	}

	public void setFilmList(List<FilmYoutube> list) {
		filmYoutubes.clear();
		for (FilmYoutube filmYoutube : list) {
			filmYoutubes.add(filmYoutube);
		}
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	public void setOnFilmClickListener(OnFilmClickListener onFilmClickListener) {
		this.onFilmClickListener = onFilmClickListener;
	}

	public void setPlayingPosition(int playingPosition) {
		this.playingPosition = playingPosition;
		if (lvList != null) {
			mAdapter.setPlayingPos(playingPosition);
			mAdapter.notifyDataSetChanged();
			scrollToPosition(playingPosition);
		}
	}

	public static interface OnFilmClickListener {
	
		public void onFilmClicked(int position);
	}
}
