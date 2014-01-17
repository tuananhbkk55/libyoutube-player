package com.movie.filmtube.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.movie.filmtube.fragment.FilmPlayerFullScreenFragment;
import com.movie.filmtube.fragment.FragmentTransactionHelper;
import com.movie.filmtube.services.FilmService;
import com.movie.filmtube.services.FilmService.LocalBinder;
import com.movie.filmtube.utils.MyPreferenceManager;
import com.youtube.bigbang.R;

public class FullScreenActivity extends FragmentActivity {
	private FilmPlayerFullScreenFragment mFilmPlayerFullScreenFragment;
	private FragmentTransactionHelper mFragmentTransactionHelper;
	private void init(Bundle bundle) {
		mFragmentTransactionHelper = new FragmentTransactionHelper(this,
				R.id.containerVideoPlayer);
		mFilmPlayerFullScreenFragment = new FilmPlayerFullScreenFragment();
		mFragmentTransactionHelper.replaceFragmentToView(
				mFilmPlayerFullScreenFragment, false);
		this.bindService(new Intent(this, FilmService.class), connection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.activity_film_full_screen);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			showVideoAdsIfNeed();
		}
		init(arg0);
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(connection);
	}
	private FilmService mService;
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.e("FilmDetailActivity", "onServiceConnected");
			mService = ((LocalBinder) service).getService();
			mFilmPlayerFullScreenFragment.setService(mService);
		}
	};
	
}
