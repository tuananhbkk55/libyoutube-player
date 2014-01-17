package com.movie.filmtube.activity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.movie.filmtube.adapters.CategoryAdapter;
import com.movie.filmtube.data.Category;
import com.movie.filmtube.data.CategoryAndFilm;
import com.movie.filmtube.data.CategoryAndFilmDao;
import com.movie.filmtube.data.CategoryDao;
import com.movie.filmtube.data.DaoSession;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.FilmYoutubeDao;
import com.movie.filmtube.data.sao.CategorySAO;
import com.movie.filmtube.data.sao.FilmSAO;
import com.movie.filmtube.fragment.BookmarkListFragment;
import com.movie.filmtube.fragment.DownloadListFragment;
import com.movie.filmtube.fragment.LocalFilmListFragment;
import com.movie.filmtube.fragment.MovieFilmListFragment;
import com.movie.filmtube.services.DownloadThread;
import com.movie.filmtube.services.FilmService;
import com.movie.filmtube.services.FilmService.LocalBinder;
import com.movie.filmtube.services.UpdateFilmThread;
import com.movie.filmtube.utils.Constants;
import com.movie.filmtube.utils.MyDatabaseHelper;
import com.movie.filmtube.utils.MyPreferenceManager;
import com.movie.filmtube.utils.OnBookmarkListener;
import com.movie.filmtube.utils.Utility;
import com.youtube.bigbang.BuildConfig;
import com.youtube.bigbang.R;


public class MainActivity extends ActionBarActivity implements OnClickListener,
		Callback {
	public static final String TAG = "MainActivity";
	private static final int TAB_CATEGORY = 0;
	private static final int TAB_YOUTUBE = 1;
	private static final int TAB_BOOKMARK = 2;
	private static final int TAB_DOWNLOAD = 3;

	private Category mCategoryAllFilms = new Category(
			Constants.CATEGORY_ALL_FILMS_ID, Constants.CATEGORY_ALL_FILMS_NAME,
			Constants.CATEGORY_ALL_FILMS_ID);
	private int mTab = TAB_CATEGORY;
	private Category mOldCategory = mCategoryAllFilms;
	private DaoSession mDaoSession;
	private ActionBar mActionBar;
	private SearchView mSearchView;
	private String lastSearchString = "";
	private MovieFilmListFragment mYoutubeFilmListFragment;
	private LocalFilmListFragment mLocalFilmListFragment;
	private BookmarkListFragment mBookmarkListFragment;
	private DownloadListFragment mDownloadListFragment;
	private ViewPager mViewPager;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle actionBarDrawerToggle;
	private ListView lvCategory;
	private CategoryAdapter mCategoryAdapter;
	private void initActionBar() {
		mActionBar = getSupportActionBar();
		mActionBar.setIcon(R.drawable.ic_launcher);
		mActionBar.setHomeButtonEnabled(true);
		mActionBar.setDisplayHomeAsUpEnabled(true);
	}
	private void initDrawerLayout() {
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		lvCategory = (ListView) findViewById(R.id.listCategory);
		mCategoryAdapter = new CategoryAdapter(this, new ArrayList<Category>());
		lvCategory.setAdapter(mCategoryAdapter);
		addOnCategorClickListener();

		actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open_label,
				R.string.drawer_close_label);
		mDrawerLayout.setDrawerListener(actionBarDrawerToggle);
	}
	private void addOnCategorClickListener() {
		lvCategory.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				mViewPager.setCurrentItem(0);
				showIndicator(0);
				Category category = mCategoryAdapter.getItem(arg2);
				loadAllFilms(category);
				mDrawerLayout.closeDrawers();
			}
		});
	}
	private void init() {
		mDaoSession = MyDatabaseHelper.getInstance(getApplicationContext())
				.getmSession();
		initViewPager();
		loadCategories();
	}
	private boolean isLoadAllFilm(Category category) {
		if (category == null
				|| category.getId() == Constants.CATEGORY_ALL_FILMS_ID) {
			return true;
		}
		return false;
	}
	private ProgressDialog progressDialog;
	private boolean isLoadingFilms = false;
	private int mLoadedFilmsSize = 0;
	private void loadAllFilms(final Category category) {		
		isLoadingFilms = true;
		mOldCategory = category;
		new Thread(new Runnable() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (mLoadedFilmsSize <= 0) {
							progressDialog = ProgressDialog.show(
									MainActivity.this, "Loading",
									"Loading will take a few minutes...");
						}
					}
				});
				
				FilmYoutubeDao filmDao = mDaoSession.getFilmYoutubeDao();
				String query = "";
				if (isLoadAllFilm(category)) {
					query = String.format("select * from %s f",
							FilmYoutubeDao.TABLENAME);
				} else {
					// Query films based on category
					query = String
							.format("select distinct f.* from FILM_YOUTUBE f "
									+ "inner join CATEGORY_AND_FILM c "
									+ "on f.server_id = c.film_Id where c.CATEGORY_ID = %s",
									category.getServerId() + "");
				}
				List<FilmYoutube> list = filmDao.queryRawStatement(query);
				if (isLoadAllFilm(category)
						&& (list == null || list.size() <= 0)) {
					list = FilmSAO.getAllFilms(getApplicationContext());
					if (list.size() > 0) {
						try {
							filmDao.getDatabase().beginTransaction();
							CategoryAndFilmDao categoryAndFilmDao = mDaoSession
									.getCategoryAndFilmDao();
							for (FilmYoutube film : list) {
								long id = filmDao.insert(film);
								film.setId(id);
								if (film.getCategoryAndFilms() != null) {
									for (CategoryAndFilm categoryAndFilm : film
											.getCategoryAndFilms()) {
										long cateId = categoryAndFilmDao
												.insert(categoryAndFilm);
										categoryAndFilm.setId(cateId);
									}
								}
							}
						} finally {
							filmDao.getDatabase().setTransactionSuccessful();
							filmDao.getDatabase().endTransaction();
							(new MyPreferenceManager(MainActivity.this))
									.setLastUpdate(new Date());
						}
					}
				}
				final List<FilmYoutube> result = list;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (progressDialog != null
								&& progressDialog.isShowing()) {
							progressDialog.dismiss();
						}
						if (result != null && result.size() > 0) {
							mLoadedFilmsSize = result.size();
							if (BuildConfig.DEBUG) {
								Log.i(TAG, "size: " + mLoadedFilmsSize);
							}
							if (mLocalFilmListFragment != null) {
								mLocalFilmListFragment.setFilmsList(result);
								if (!TextUtils.isEmpty(lastSearchString)) {
									mLocalFilmListFragment
											.searchFilmList(lastSearchString);
								}
							}
						} else {
							showNoInternetDialog();
						}
						changeActionBarLabel(category.getName());
					}
				});
				isLoadingFilms = false;
			}
		}).start();
	}
	private void loadCategories() {
		new AsyncTask<Void, Void, List<Category>>() {

			@Override
			protected List<Category> doInBackground(Void... params) {
				CategoryDao categoryDao = mDaoSession.getCategoryDao();
				List<Category> categories = categoryDao.loadAll();
				if (categories == null || categories.size() <= 0) {
					categories = CategorySAO
							.getAllCategories(getApplicationContext());
					if (categories.size() > 0) {
						categoryDao.insertInTx(categories);
					}
				}
				if (categories != null) {
					Collections.sort(categories, new Comparator<Category>() {

						@Override
						public int compare(Category lhs, Category rhs) {
							return lhs.getName().compareTo(rhs.getName());
						}
					});
					;
				}
				return categories;
			}

			protected void onPostExecute(java.util.List<Category> result) {
				if (result != null && result.size() > 0) {				
					mCategoryAdapter.clear();
					mCategoryAdapter.add(mCategoryAllFilms);
					for (Category category : result) {
						mCategoryAdapter.add(category);
					}
					mCategoryAdapter.notifyDataSetChanged();
				} else {
				}
				changeActionBarLabel(mCategoryAllFilms.getName());
			};
		}.execute();
	}
	private void changeActionBarLabel(String label) {
		mActionBar.setTitle(label);
	}
	private void searchLocalFilms(final String text) {
		mBookmarkListFragment.searchFilm(text);
		mLocalFilmListFragment.searchFilmList(text);
		mDownloadListFragment.searchDownloadedVideos(text);
	}
	private void searchYoutubeFilms(final String text) {
		if (mYoutubeFilmListFragment != null
				&& mYoutubeFilmListFragment.isVisible()) {
			mYoutubeFilmListFragment.searchByKeyword(text);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initActionBar();
		initDrawerLayout();
		init();
		initHistoryView();
		startService(new Intent(this, FilmService.class));
		if (!DownloadThread.isDownloading) {
			DownloadThread.getInstance(this).start();
		}
		bindService(new Intent(this, FilmService.class), connection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		actionBarDrawerToggle.syncState();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(connection);
		stopService(new Intent(this, FilmService.class));
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceivers();
		if (!isLoadingFilms) {
			loadAllFilms(mOldCategory);
		}
		surfaceView.getHolder().addCallback(this);
		showUpdatingIconBasedOnState();
		showHistoryInformation();
		startAppAdd.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(broadcastReceiver);
		startAppAdd.onPause();
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Constants.ACTION_RELOAD_NAVIGATION_PANE.equals(action)) {
			} else if (Constants.ACTION_UPDATE_FILM_FINISHED.equals(action)) {
				loadAllFilms(mOldCategory);
				showUpdatingIconBasedOnState();
				Utility.showInformationDialog(MainActivity.this,
						getString(R.string.message_update_successfully));
			} else if (Constants.ACTION_RELOAD_BOOKMARKS.equals(action)) {
				loadAllFilms(mOldCategory);
				FilmYoutube filmYoutube;
				if (mService != null
						&& (filmYoutube = mService.getCurrentPlayingFilm()) != null) {
					String videoId = intent
							.getStringExtra(Constants.KEY_BOOKMARK_DELETED);
					if (filmYoutube.getYoutubeId().equals(videoId)) {
						filmYoutube.setIsBookmarked(false);
						showHistoryInformation();
					}

				}
			} else if (Constants.ACTION_MEDIA_PROGRESS_UPDATED.equals(action)) {
				if (mService != null) {
					showCurrentPlayingTime();
				}
				if (viewHistory.getVisibility() != View.VISIBLE) {
					showHistoryInformation();
				}
			} else if (Constants.ACTION_MEDIA_RELOAD_CONTROLLER.equals(action)) {
				if (mService != null) {
					showPlayControllerState();
				}
				if (BuildConfig.DEBUG) {
					Log.i(TAG, "ACTION_MEDIA_RELOAD_CONTROLLER");
				}
			} else if (Constants.ACTION_DOWNLOAD_VIDEO_CANCELED.equals(action)) {
				loadAllFilms(mOldCategory);
			}
		}
	};
	private void registerReceivers() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_RELOAD_NAVIGATION_PANE);
		filter.addAction(Constants.ACTION_UPDATE_FILM_FINISHED);
		filter.addAction(Constants.ACTION_RELOAD_BOOKMARKS);
		filter.addAction(Constants.ACTION_MEDIA_PROGRESS_UPDATED);
		filter.addAction(Constants.ACTION_DOWNLOAD_VIDEO_CANCELED);
		registerReceiver(broadcastReceiver, filter);
	}
	private void addListenerForSearchAction(final Menu menu) {
		mSearchView.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(final String text) {				
				mSearchView.clearFocus();
				if (mTab == TAB_YOUTUBE) {
					searchYoutubeFilms(text);
				}
				return true;
			}

			@Override
			public boolean onQueryTextChange(String text) {
				lastSearchString = text;
				searchLocalFilms(text);
				return false;
			}
		});
		MenuItemCompat.setOnActionExpandListener(
				menu.findItem(R.id.action_search),
				new OnActionExpandListener() {
					@Override
					public boolean onMenuItemActionCollapse(MenuItem item) {
						lastSearchString = "";
						searchLocalFilms("");
						return true; 
					}

					@Override
					public boolean onMenuItemActionExpand(MenuItem item) {
						return true; 
					}
				});

	}
	private MenuItem mRefreshItem;
	private void showUpdatingIconBasedOnState() {
		if (mRefreshItem == null) {
			return;
		}
		if (UpdateFilmThread.isUpdating) {
			MenuItemCompat.setActionView(mRefreshItem,
					R.layout.view_update_films);
		} else {
			MenuItemCompat.setActionView(mRefreshItem, null);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main_activity, menu);
		mRefreshItem = menu.findItem(R.id.action_refresh);
		mSearchView = (SearchView) MenuItemCompat.getActionView(menu
				.findItem(R.id.action_search));
		addListenerForSearchAction(menu);
		showUpdatingIconBasedOnState();
		return super.onCreateOptionsMenu(menu);
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
		case R.id.action_refresh:
			if (!UpdateFilmThread.isRecentUpdate(this)) {
				UpdateFilmThread.runUpdateFilmThread(this);
				MenuItemCompat.setActionView(mRefreshItem,
						R.layout.view_update_films);
			} else {
				Utility.showInformationDialog(MainActivity.this,
						getString(R.string.message_update_successfully));
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}

	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.viewBookmark:
			mViewPager.setCurrentItem(2);
			showIndicator(2);
			break;
		case R.id.viewDownload:
			mViewPager.setCurrentItem(3);
			showIndicator(3);
			break;
		case R.id.viewLocalFilm:
			mViewPager.setCurrentItem(0);
			showIndicator(0);
			break;
		case R.id.viewYoutubeFilms:
			mViewPager.setCurrentItem(1);
			showIndicator(1);
			break;
		case R.id.btnCancel:
			hideHistoryInformation();
			break;
		case R.id.btnBookmarkHistory:
			if (mService != null && mService.getCurrentPlayingFilm() != null) {
				Utility.showBookmarkDialog(this,
						mService.getCurrentPlayingFilm(),
						new OnBookmarkListener() {
							@Override
							public void onBookmarkedSucceed(
									FilmYoutube filmYoutube) {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										showHistoryInformation();
									}
								});
							}
							@Override
							public void onBookmarkDeleted(
									FilmYoutube filmYoutube) {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										showHistoryInformation();
									}
								});
							}
						});
			}
			break;

		case R.id.viewHistory:
			if (mService != null) {
				ArrayList<FilmYoutube> list = new ArrayList<FilmYoutube>();
				Utility.startFilmDetailActivity(this,
						mService.getCurrentPlayingFilm(), list);
			}
			break;
		case R.id.btnPlayOrPause:
			if (mService != null) {
				switch (mService.getMediaState()) {
				case PAUSED:
					mService.startPlayingVideo();
					btnPlayOrPause
							.setBackgroundResource(R.drawable.selector_controller_pause);
					break;
				case STARTED:
					btnPlayOrPause
							.setBackgroundResource(R.drawable.selector_controller_play);
					mService.pausePlayingVideo();
				default:
					break;
				}
			}
			break;
		default:
			break;
		}
	}
	private View viewHistory;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private TextView tvFilmName;
	private TextView tvTime;
	private Button btnBookmarkHistory;
	private Button btnPlayOrPause;

	private FilmService mService;
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "onServiceConnected");
			mService = ((LocalBinder) service).getService();
			if (surfaceHolder != null) {
				mService.changeSurfaceView(surfaceHolder);
				showHistoryInformation();
			}
		}
	};
	private void showHistoryInformation() {
		if (mService != null && mService.isMediaReadyForPlay()) {
			viewHistory.setVisibility(View.VISIBLE);
			FilmYoutube film = mService.getCurrentPlayingFilm();
			tvFilmName.setText(film.getName());
			showCurrentPlayingTime();
			if (film.getIsBookmarked()) {
				btnBookmarkHistory
						.setBackgroundResource(R.drawable.ic_flag_focus);
				btnBookmarkHistory.setOnClickListener(null);
			} else {
				btnBookmarkHistory.setBackgroundResource(R.drawable.ic_flag);
				btnBookmarkHistory.setOnClickListener(this);
			}
			new Thread(new Runnable() {

				@Override
				public void run() {
					mService.startPlayingVideo();
				}
			}).start();
			showPlayControllerState();
		} else {
			viewHistory.setVisibility(View.GONE);
		}
	}

	private void showCurrentPlayingTime() {
		tvTime.setText(Utility.convertTimeToDisplayedTime(mService
				.getCurrentVideoTime()));
	}

	private void showPlayControllerState() {
		switch (mService.getMediaState()) {
		case PAUSED:
			btnPlayOrPause
					.setBackgroundResource(R.drawable.selector_controller_play);
			break;
		case STARTED:
			btnPlayOrPause
					.setBackgroundResource(R.drawable.selector_controller_pause);

		default:
			break;
		}
	}
	private void hideHistoryInformation() {
		viewHistory.setVisibility(View.GONE);
		if (mService != null && mService.isMediaReadyForPlay()) {
			mService.releaseMediaPlayer();
		}
	}
	private void initHistoryView() {
		viewHistory = findViewById(R.id.viewHistory);
		surfaceView = (SurfaceView) findViewById(R.id.svVideoPlayer);
		tvFilmName = (TextView) findViewById(R.id.tvFilmName);
		tvTime = (TextView) findViewById(R.id.tvTime);
		btnBookmarkHistory = (Button) findViewById(R.id.btnBookmarkHistory);
		btnPlayOrPause = (Button) findViewById(R.id.btnPlayOrPause);
		btnPlayOrPause.setOnClickListener(this);
		findViewById(R.id.btnCancel).setOnClickListener(this);

		viewHistory.setOnClickListener(this);
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		surfaceHolder = holder;
		if (mService != null) {
			mService.changeSurfaceView(surfaceHolder);
			showHistoryInformation();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mService != null && surfaceHolder == mService.getSurfaceHolder()) {
			mService.changeSurfaceView(null);
		}
		surfaceHolder = null;
	}

	private void showNoInternetDialog() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(getString(R.string.dialog_no_internet_title));
		builder.setMessage(getString(R.string.dialog_no_internet_message));
		builder.setPositiveButton(getString(R.string.ok_label), null);
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	@Override
	public void onBackPressed() {
		if (isTaskRoot()) {
			AlertDialog.Builder builder = new Builder(this);
			builder.setTitle(getString(R.string.dialog_exit_title));
			builder.setMessage(getString(R.string.dialog_exit_message));
			builder.setPositiveButton(getString(R.string.yes_label),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							
							if (mService != null) {
								mService.stopService();
							}
							MainActivity.this.finish();
						}
					});
			builder.setNegativeButton(getString(R.string.no_label), null);
			builder.setNeutralButton(getString(R.string.dialog_exit_more_app),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent startMain = new Intent(Intent.ACTION_MAIN);
							startMain.addCategory(Intent.CATEGORY_HOME);
							startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(startMain);
						}
					});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		} else {			
			super.onBackPressed();
		}
	}

	private View tabIndicatorLocalFilm;
	private View tabIndicatorYoutubeFilm;
	private View tabIndicatorBookmark;
	private View tabIndicatorDownload;

	private void initViewPager() {
		tabIndicatorLocalFilm = findViewById(R.id.tabIndicatorviewLocalFilm);
		tabIndicatorYoutubeFilm = findViewById(R.id.tabIndicatorYoutubeFilms);
		tabIndicatorBookmark = findViewById(R.id.tabIndicatorBookmark);
		tabIndicatorDownload = findViewById(R.id.tabIndicatorDownload);

		findViewById(R.id.viewBookmark).setOnClickListener(this);
		findViewById(R.id.viewDownload).setOnClickListener(this);
		findViewById(R.id.viewLocalFilm).setOnClickListener(this);
		findViewById(R.id.viewYoutubeFilms).setOnClickListener(this);

		mYoutubeFilmListFragment = MovieFilmListFragment.newInstance();
		mLocalFilmListFragment = LocalFilmListFragment.newInstance();
		mBookmarkListFragment = BookmarkListFragment.newInstance();
		mDownloadListFragment = DownloadListFragment.newInstance();

		mViewPager = (ViewPager) findViewById(R.id.viewPager);
		mViewPager
				.setAdapter(new MainPagerAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

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

	private void showIndicator(int pos) {
		mTab = pos;
		tabIndicatorLocalFilm.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));
		tabIndicatorYoutubeFilm.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));
		tabIndicatorBookmark.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));
		tabIndicatorDownload.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));

		switch (pos) {
		case TAB_CATEGORY:
			tabIndicatorLocalFilm.setBackgroundColor(getResources().getColor(
					R.color.blue_light));
			changeActionBarLabel(mOldCategory.getName());
			break;
		case TAB_YOUTUBE:
			tabIndicatorYoutubeFilm.setBackgroundColor(getResources().getColor(
					R.color.blue_light));
			changeActionBarLabel("Movies World");
			break;
		case TAB_BOOKMARK:
			tabIndicatorBookmark.setBackgroundColor(getResources().getColor(
					R.color.blue_light));
			changeActionBarLabel("Bookmarks");
			break;
		case TAB_DOWNLOAD:
			tabIndicatorDownload.setBackgroundColor(getResources().getColor(
					R.color.blue_light));
			changeActionBarLabel("Saved Films");
			break;
		default:
			break;
		}
	}
	private class MainPagerAdapter extends FragmentStatePagerAdapter {

		public MainPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int arg0) {
			switch (arg0) {
			case TAB_CATEGORY:
				return mLocalFilmListFragment;
			case TAB_YOUTUBE:

				return mYoutubeFilmListFragment;
			case TAB_BOOKMARK:
				return mBookmarkListFragment;
			case TAB_DOWNLOAD:
				return mDownloadListFragment;
			default:
				return new Fragment();
			}
		}

		@Override
		public int getCount() {
			return 4;
		}

	}
}
