package com.movie.filmtube.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.FilmYoutubeDao;
import com.movie.filmtube.fragment.BookmarkDetailFragment;
import com.movie.filmtube.fragment.FilmPlayerFragment;
import com.movie.filmtube.fragment.FragmentTransactionHelper;
import com.movie.filmtube.fragment.MovieCommentFragment;
import com.movie.filmtube.fragment.MovieFilmDetailFragment;
import com.movie.filmtube.fragment.MovieRelatedFilmsFragment;
import com.movie.filmtube.fragment.MovieRelatedFilmsFragment.OnFilmClickListener;
import com.movie.filmtube.services.FilmService;
import com.movie.filmtube.services.FilmService.LocalBinder;
import com.movie.filmtube.utils.Constants;
import com.movie.filmtube.utils.MyDatabaseHelper;
import com.movie.filmtube.utils.OnBookmarkListener;
import com.movie.filmtube.utils.Utility;
import com.movie.filmtube.utils.movie.world.helper.MovieComment;
import com.movie.filmtube.utils.movie.world.helper.MovieSearchingHelper;
import com.youtube.bigbang.BuildConfig;
import com.youtube.bigbang.R;

public class FilmDetailActivity extends FragmentActivity implements
		OnClickListener, OnFilmClickListener, OnBookmarkListener {

	private static final String TAG = "FilmDetailActivity";

	private FragmentTransactionHelper mFragmentTransactionHelper;
	private FilmPlayerFragment mFilmPlayerFragment;
	private MyDatabaseHelper myDatabaseHelper;


	private MovieFilmDetailFragment mYoutubeFilmDetailFragment;
	private MovieRelatedFilmsFragment mYoutubeRelatedFilmsFragment;
	private MovieCommentFragment mYoutubeCommentFragment;
	private BookmarkDetailFragment mBookmarkDetailFragment;

	private FilmYoutube mFilm;
	private List<FilmYoutube> mRelatedFilms = new ArrayList<FilmYoutube>();

	private TextView tvTitle;

	private ViewPager viewPager;
	private Button btnBookmark;
	private View tabIndicatorChapter;
	private View tabIndicatorContent;
	private View tabIndicatorComments;
	private View tabIndicatorBookmark;


	private void initView() {

		viewPager = (ViewPager) findViewById(R.id.viewPager);
		tvTitle = (TextView) findViewById(R.id.tvTitle);
		tabIndicatorChapter = findViewById(R.id.tabIndicatorChapter);
		tabIndicatorContent = findViewById(R.id.tabIndicatorContent);
		tabIndicatorBookmark = findViewById(R.id.tabIndicatorBookmark);
		tabIndicatorComments = findViewById(R.id.tabIndicatorComments);
		btnBookmark = (Button) findViewById(R.id.btnBookmarks);
		btnBookmark.setOnClickListener(this);
		findViewById(R.id.viewComments).setOnClickListener(this);
		findViewById(R.id.viewContent).setOnClickListener(this);
		findViewById(R.id.viewRelatedFilms).setOnClickListener(this);
		findViewById(R.id.btnBack).setOnClickListener(this);
		findViewById(R.id.btnRate).setOnClickListener(this);
		initViewPagers();
		showBookmarkIcon();
		tvTitle.setText(mFilm.getName());
	}


	private void initViewPagers() {
		mYoutubeFilmDetailFragment = MovieFilmDetailFragment.newInstance(null);
		mYoutubeRelatedFilmsFragment = MovieRelatedFilmsFragment.newInstance(
				mRelatedFilms, this, findFilmPosition());
		mYoutubeCommentFragment = MovieCommentFragment
				.newInstance(new ArrayList<MovieComment>());
		mBookmarkDetailFragment = BookmarkDetailFragment.newInstance(mFilm);
		viewPager.setAdapter(new FilmDetailPagerAdapter(
				getSupportFragmentManager()));
		viewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int arg0) {
				showIndicator(arg0);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
	}
	private void showBookmarkIcon() {
		if (mFilm != null) {
			int id = mFilm.getIsBookmarked() ? R.drawable.ic_flag_focus
					: R.drawable.ic_flag;
			btnBookmark.setBackgroundResource(id);
			mBookmarkDetailFragment.setmFilm(mFilm);
		}
	}

	private FilmYoutube findFilmClicked() {
		mFilm = getIntent().getParcelableExtra(Constants.KEY_FILM_PICKED);
		mRelatedFilms = getIntent().getParcelableArrayListExtra(
				Constants.KEY_FILMS_RELATED);
		if (mFilm == null) {
			Toast.makeText(getApplicationContext(), "Error in finding film",
					Toast.LENGTH_SHORT).show();
			this.finish();
		}	
		return mFilm;
	}

	private void init(Bundle bundle) {
		myDatabaseHelper = MyDatabaseHelper
				.getInstance(getApplicationContext());
		mFragmentTransactionHelper = new FragmentTransactionHelper(this,
				R.id.containerVideoPlayer);
		findFilmClicked();
		mFilmPlayerFragment = FilmPlayerFragment.newInstance(mFilm);
		mFragmentTransactionHelper.replaceFragmentToView(mFilmPlayerFragment,
				false);
		initView();
		this.bindService(new Intent(this, FilmService.class), connection,
				Context.BIND_AUTO_CREATE);
		mDisplay = getWindowManager().getDefaultDisplay();
	}

	private Display mDisplay;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		switch (mDisplay.getRotation()) {
		case Surface.ROTATION_90:
		case Surface.ROTATION_270:
			toggleFullScreen();
			break;

		default:
			break;
		}
	}
	private void toggleFullScreen() {
		if (mFilmPlayerFragment != null) {
			mFilmPlayerFragment.toggleFullScreen();
		}
	}

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.activity_film_detail);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		init(arg0);
		registerReceiver();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mThread != null) {
			mThread.interrupt();
		}
		if (mService != null) {
			updateBookmarkIfNeed();
		}
		unregister();
		unbindService(connection);
	}

	private void updateBookmarkIfNeed() {
			if (mFilm == null || !mFilm.getIsBookmarked() || mService == null) {
			return;
		}
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				FilmYoutubeDao filmYoutubeDao = myDatabaseHelper.getmSession()
						.getFilmYoutubeDao();
				FilmYoutube filmYoutube = filmYoutubeDao
						.getFilmBasedOnVideoId(mFilm.getYoutubeId());
				if (filmYoutube != null) {
					mFilm.setId(filmYoutube.getId());
					mFilm.setLastPlayedTime((long) mService
							.getCurrentVideoTime());
					myDatabaseHelper.getmSession().getFilmYoutubeDao()
							.update(mFilm);
					sendBroadcast(new Intent(Constants.ACTION_RELOAD_BOOKMARKS));				
				}
			}
		});
		thread.start();
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.slide_in_left_to_right,
				R.anim.slide_out_left_to_right);
	}

	private void showIndicator(int pos) {
		tabIndicatorComments.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));
		tabIndicatorContent.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));
		tabIndicatorChapter.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));
		tabIndicatorBookmark.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));
		switch (pos) {
		case TAB_RELATED_FILMS:
			tabIndicatorChapter.setBackgroundColor(getResources().getColor(
					R.color.blue_light));
			break;
		case TAB_INFORMATION:
			tabIndicatorContent.setBackgroundColor(getResources().getColor(
					R.color.blue_light));
			break;
		case TAB_COMMENTS:
			tabIndicatorComments.setBackgroundColor(getResources().getColor(
					R.color.blue_light));
			break;
		case TAB_BOOKMARK:
			tabIndicatorBookmark.setBackgroundColor(getResources().getColor(
					R.color.blue_light));
			break;
		default:
			break;
		}
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.viewRelatedFilms:
			viewPager.setCurrentItem(TAB_RELATED_FILMS);
			showIndicator(TAB_RELATED_FILMS);
			break;
		case R.id.viewContent:
			viewPager.setCurrentItem(TAB_INFORMATION);
			showIndicator(TAB_INFORMATION);
			break;
		case R.id.viewComments:
			viewPager.setCurrentItem(TAB_COMMENTS);
			showIndicator(TAB_COMMENTS);
			break;
		case R.id.btnBack:
			this.finish();
			break;
		case R.id.btnRate:
			showRateDialog();
			break;
		case R.id.btnBookmarks:
			viewPager.setCurrentItem(TAB_BOOKMARK);
			showIndicator(TAB_BOOKMARK);
			if (!mFilm.getIsBookmarked()) {
				Utility.showBookmarkDialog(this, mFilm, this);
			} else {
				Utility.showDeleteBookmarkDialog(this, mFilm, this);
			}
			break;
		default:
			break;
		}
	}

	private void showRateDialog() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(getString(R.string.dialog_rate_title));
		builder.setMessage(getString(R.string.dialog_rate_message));
		builder.setPositiveButton(getString(R.string.dialog_rate_yes),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String packageName = "com.xxx.xxxx";
						try {
							startActivity(new Intent(Intent.ACTION_VIEW,
									Uri.parse("market://details?id="
											+ packageName)));
						} catch (android.content.ActivityNotFoundException anfe) {
							startActivity(new Intent(
									Intent.ACTION_VIEW,
									Uri.parse("http://play.google.com/store/apps/details?id="
											+ packageName)));
						}
					}
				});
		builder.setNegativeButton(getString(R.string.no_label), null);
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}
	private int findFilmPosition() {
		int pos = -1;
		for (FilmYoutube filmYoutube : mRelatedFilms) {
			pos++;
			if (filmYoutube.getYoutubeId().equals(mFilm.getYoutubeId())) {				
				return pos;
			}
		}
		return 0;
	}

	private void playSelectedFilm() {
		int pos = findFilmPosition();
		mRelatedFilms.set(pos, mFilm);
		mService.setYoutubeFilms(mRelatedFilms);
		mFilmPlayerFragment.setService(mService);
		if (mFilm.getIsBookmarked()) {
			mService.setPlayedTime((mFilm.getLastPlayedTime().intValue()));
		}
		mService.playChapterAtPosition(pos);
	}
	
	private Thread mThread = null;
	private boolean isThreadInterupt() {
		if (mThread == null || mThread.isInterrupted()) {		
			return true;
		} else {
			return false;
		}
	}
	private void getFilmDetailTask() {		
		if (mService.getCurrentPlayingFilm() != null) {
			mFilm = mService.getCurrentPlayingFilm();
			tvTitle.setText(mFilm.getName());
			if (mYoutubeRelatedFilmsFragment != null) {
				int pos = findFilmPosition();
				mYoutubeRelatedFilmsFragment.setPlayingPosition(pos);
			}
		
			if (mFilm.getViewCount() != null) {
				showFilmInformation();
			}
			if (mFilm.getComments() != null) {
				showComments(mFilm.getComments());
				return;
			}
			
			if (mThread != null) {
				mThread.interrupt();
			}
			mThread = new Thread(new Runnable() {

				@Override
				public void run() {
					if (isThreadInterupt()) {
						return;
					}
					FilmYoutubeDao filmYoutubeDao = myDatabaseHelper
							.getmSession().getFilmYoutubeDao();
					FilmYoutube film = filmYoutubeDao
							.getFilmBasedOnVideoId(mFilm.getYoutubeId());
					if (film != null) {
						mFilm.setId(film.getId());
					}
					
					MovieSearchingHelper.getVideoInformation(mFilm);
					if (isThreadInterupt()) {
						return;
					}
					showFilmInformation();
					
					if (mFilm != null && mFilm.getId() != null) {
						filmYoutubeDao.update(mFilm);
					}
					List<MovieComment> list = MovieSearchingHelper
							.getComments(mFilm.getYoutubeId());
					mFilm.setComments(list);
					showComments(list);
					if (isThreadInterupt()) {
						return;
					}					
					mThread = null;
				}
			});
			mThread.start();
		}
	}

	private void showComments(final List<MovieComment> list) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mYoutubeCommentFragment.setComments(list);
			}
		});
	}
	private void showFilmInformation() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mYoutubeFilmDetailFragment.setFilmYoutube(mFilm);
			}
		});
	}
	private FilmService mService;
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((LocalBinder) service).getService();
			
			if (mService.isFilmPlaying(mFilm)) {
			
				mService.getCurrentPlayingFilm().setIsBookmarked(
						mFilm.getIsBookmarked());
				mRelatedFilms = mService.getFilms();
				mFilm = mService.getCurrentPlayingFilm();
				mFilmPlayerFragment.setService(mService);
			
				if (!mService.isMediaReadyForPlay()) {
					mService.restartMediaplayerIfNeed();
				}
		
				if (mYoutubeRelatedFilmsFragment != null) {
					mYoutubeRelatedFilmsFragment.setFilmList(mRelatedFilms);
					mYoutubeRelatedFilmsFragment
							.setPlayingPosition(findFilmPosition());
				}
			} else {
				playSelectedFilm();
			}
			getFilmDetailTask();
		}
	};

	@Override
	public void onFilmClicked(int pos) {

		if (mService == null) {
			mFilm = mRelatedFilms.get(pos);
		} else {
			mService.playChapterAtPosition(pos);
			if (mThread != null) {
					mThread.interrupt();
			}
		}
		tvTitle.setText(mFilm.getName());
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Constants.ACTION_MEDIA_VIDEO_CHANGED.equals(action)) {
				getFilmDetailTask();
			}
		}
	};

	private boolean isRegistered = false;

	private void registerReceiver() {
		isRegistered = true;
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_MEDIA_VIDEO_CHANGED);
		this.registerReceiver(mBroadcastReceiver, filter);
	}

	private void unregister() {
		if (isRegistered) {
			unregisterReceiver(mBroadcastReceiver);
		}
		isRegistered = false;
	}

	private static final int TAB_RELATED_FILMS = 0;
	private static final int TAB_INFORMATION = 1;
	private static final int TAB_COMMENTS = 2;
	private static final int TAB_BOOKMARK = 3;

	private class FilmDetailPagerAdapter extends FragmentStatePagerAdapter {
		public FilmDetailPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			if (position == TAB_RELATED_FILMS) {
				return mYoutubeRelatedFilmsFragment;
			} else if (position == TAB_INFORMATION) {
				return mYoutubeFilmDetailFragment;
			} else if (position == TAB_COMMENTS) {
				return mYoutubeCommentFragment;
			} else if (position == TAB_BOOKMARK) {
				return mBookmarkDetailFragment;
			} else {
				return new Fragment();
			}
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}

		@Override
		public int getCount() {
			return 4;
		}
	}

	@Override
	public void onBookmarkedSucceed(FilmYoutube filmYoutube) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				showBookmarkIcon();
			}
		});
	}

	@Override
	public void onBookmarkDeleted(FilmYoutube filmYoutube) {
		mFilm.setIsBookmarked(false);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				showBookmarkIcon();
			}
		});
	}
}
