package com.movie.filmtube.fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.movie.filmtube.activity.FullScreenActivity;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.services.FilmService;
import com.movie.filmtube.utils.Constants;
import com.movie.filmtube.utils.Constants.MediaState;
import com.movie.filmtube.utils.Utility;
import com.youtube.bigbang.BuildConfig;
import com.youtube.bigbang.R;

public class FilmPlayerFragment extends MyParentFragment implements Callback,
		OnClickListener, OnSeekBarChangeListener {
	public static final int TIME_STEP = 10000;
	public static final int TIME_OUT_CONTROLLER = 7000;

	protected static final String TAG = "FilmPlayerFragment";
	protected FilmYoutube mFilmYoutube;

	protected FilmService mService;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	protected View viewController;
	protected Button btnPreviousChapter;
	protected Button btnNextChapter;
	protected Button btnBackward;
	protected Button btnForward;
	protected Button btnPauseOrPlay;
	protected Button btnToggleScreen;
	protected SeekBar sbProgress;
	protected TextView tvChapterName;
	protected TextView tvPlayedTime;
	protected TextView tvTotalTime;
	protected ProgressBar pbLoading;
	protected Button btnRestart;

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Constants.ACTION_MEDIA_READY_TO_PLAY.equals(action)) {
				startPlayingVideo();
				showFilmInformationAndStartIfNeeded();
				btnRestart.setVisibility(View.GONE);
			} else if (Constants.ACTION_MEDIA_ERROR.equals(action)) {
				btnRestart.setVisibility(View.VISIBLE);
				pbLoading.setVisibility(View.GONE);

			} else if (Constants.ACTION_MEDIA_BUFFERING_UPDATED.equals(action)) {
				int percent = intent.getIntExtra(Constants.KEY_PERCENT_UPDATE,
						0);
				if (percent > 0) {
					updateBufferingProgress(percent);
				}
			} else if (Constants.ACTION_MEDIA_COMPLETE.equals(action)) {
			
			} else if (Constants.ACTION_MEDIA_WAITING_FOR_LOADING
					.equals(action)) {
				pbLoading.setVisibility(View.VISIBLE);
				btnRestart.setVisibility(View.GONE);
			} else if (Constants.ACTION_MEDIA_PROGRESS_UPDATED.equals(action)) {
				updateProgressInSeekBar();
			} else if (Constants.ACTION_MEDIA_VIDEO_CHANGED.equals(action)) {
				showFilmInformationAndStartIfNeeded();
			} else if (Constants.ACTION_MEDIA_RELOAD_CONTROLLER.equals(action)) {
				showPlayControllerState();
				
			} else if (Constants.ACTION_UPDATE_SURFACE_VIEW.equals(action)) {
				if (mService != null && surfaceHolder != null) {
					pausePlayingVideo();
					mService.changeSurfaceView(surfaceHolder);
					new Handler().postDelayed(new Runnable() {

						@Override
						public void run() {
							startPlayingVideo();
						}
					}, 100);
				}
			}
		}
	};
	private void registerReceivers() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_MEDIA_BUFFERING_UPDATED);
		filter.addAction(Constants.ACTION_MEDIA_COMPLETE);
		filter.addAction(Constants.ACTION_MEDIA_ERROR);
		filter.addAction(Constants.ACTION_MEDIA_READY_TO_PLAY);
		filter.addAction(Constants.ACTION_MEDIA_WAITING_FOR_LOADING);
		filter.addAction(Constants.ACTION_MEDIA_PROGRESS_UPDATED);
		filter.addAction(Constants.ACTION_MEDIA_VIDEO_CHANGED);
		filter.addAction(Constants.ACTION_MEDIA_RELOAD_CONTROLLER);
		filter.addAction(Constants.ACTION_UPDATE_SURFACE_VIEW);
		context.registerReceiver(mBroadcastReceiver, filter);
	}

	private void findViewElements(View view) {
		surfaceView = (SurfaceView) view.findViewById(R.id.svVideoPlayer);
		viewController = view.findViewById(R.id.viewController);
		tvChapterName = (TextView) view.findViewById(R.id.tvVideoName);
		tvPlayedTime = (TextView) view.findViewById(R.id.tvPlayedTime);
		tvTotalTime = (TextView) view.findViewById(R.id.tvTotalTime);
		pbLoading = (ProgressBar) view.findViewById(R.id.pbLoading);

		btnPreviousChapter = (Button) view
				.findViewById(R.id.btnPreviousChapter);
		btnNextChapter = (Button) view.findViewById(R.id.btnNextChapter);
		btnBackward = (Button) view.findViewById(R.id.btnBackward);
		btnForward = (Button) view.findViewById(R.id.btnForward);
		btnPauseOrPlay = (Button) view.findViewById(R.id.btnPause);
		btnToggleScreen = (Button) view.findViewById(R.id.btnScreenZoom);
		sbProgress = (SeekBar) view.findViewById(R.id.sbFilmProgress);
		btnRestart = (Button) view.findViewById(R.id.btnRestart);
		btnRestart.setOnClickListener(this);
		surfaceView.setOnClickListener(this);
		btnPreviousChapter.setOnClickListener(this);
		btnNextChapter.setOnClickListener(this);
		btnBackward.setOnClickListener(this);
		btnForward.setOnClickListener(this);
		btnPauseOrPlay.setOnClickListener(this);
		btnToggleScreen.setOnClickListener(this);
		sbProgress.setOnSeekBarChangeListener(this);
		enableControllers(false);
	}

	private void enableControllers(boolean enable) {
		btnBackward.setClickable(enable);
		btnForward.setClickable(enable);
		sbProgress.setEnabled(enable);
	}

	@Override
	protected void init(View view) {
		registerReceivers();
		findViewElements(view);
		initSurfaceView();
	}

	private void showFilmInformationAndStartIfNeeded() {
		if (mService != null && mService.getCurrentPlayingFilm() != null) {
			tvChapterName.setText(""
					+ mService.getCurrentPlayingFilm().getName());
			if (mService.isMediaReadyForPlay()) {
				startPlayingVideo();
			}
			showPlayControllerState();
		}
	}
	protected void showPlayControllerState() {
		if (mService == null) {
			return;
		}
		switch (mService.getMediaState()) {
		case PAUSED:
			btnPauseOrPlay
					.setBackgroundResource(R.drawable.selector_controller_play);
			break;
		case STARTED:
			btnPauseOrPlay
					.setBackgroundResource(R.drawable.selector_controller_pause);

		default:
			break;
		}

	}

	
	private void initSurfaceView() {
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
	}
	private int totalTimeInMili = 0;
	
	protected void startPlayingVideo() {
		if (mService != null && mService.isMediaReadyForPlay()) {
			mService.startPlayingVideo();
			enableControllers(true);
			btnPauseOrPlay
					.setBackgroundResource(R.drawable.selector_controller_pause);
			totalTimeInMili = mService.getTotalVideoTime();
			sbProgress.setMax(totalTimeInMili);
			tvTotalTime.setText(Utility
					.convertTimeToDisplayedTime(totalTimeInMili));
			pbLoading.setVisibility(View.GONE);
		}
	}
	protected void pausePlayingVideo() {
		if (mService != null && mService.isMediaReadyForPlay()) {
			mService.pausePlayingVideo();
		}
		btnPauseOrPlay
				.setBackgroundResource(R.drawable.selector_controller_play);
		;
		pbLoading.setVisibility(View.VISIBLE);
	
	}
	private int timeUpdating = 0;

	private void updateProgressInSeekBar() {
		if (mService != null && mService.isMediaReadyForPlay()) {
			int mCurrentTime = mService.getCurrentVideoTime();
			tvPlayedTime.setText(Utility
					.convertTimeToDisplayedTime(mCurrentTime));
			sbProgress.setProgress(mCurrentTime);
			timeUpdating += FilmService.TIME_SECONDS;
			if (timeUpdating > TIME_OUT_CONTROLLER) {
				hideController();
			}
		}
	}

	protected void hideController() {
		viewController.setVisibility(View.GONE);
	}

	protected void showController() {
		viewController.setVisibility(View.VISIBLE);
	}

	private void updateBufferingProgress(int percent) {
		int buffering = percent * totalTimeInMili / 100;
		sbProgress.setSecondaryProgress(buffering);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (surfaceHolder == null) {
			initSurfaceView();
		}
		showFilmInformationAndStartIfNeeded();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mService != null) {
			mService.pausePlayingVideo();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		context.unregisterReceiver(mBroadcastReceiver);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_film_player;
	}

	public static FilmPlayerFragment newInstance(FilmYoutube filmYoutube) {
		FilmPlayerFragment filmPlayerFragment = new FilmPlayerFragment();
		filmPlayerFragment.setmFilmYoutube(filmYoutube);
		return filmPlayerFragment;
	}

	public void setmFilmYoutube(FilmYoutube filmYoutube) {
		this.mFilmYoutube = filmYoutube;
	}
	public FilmService getService() {
		return mService;
	}

	public void setService(FilmService mService) {
		this.mService = mService;
		if (surfaceHolder != null && isSurfaceCreated) {
			mService.changeSurfaceView(surfaceHolder);
			showFilmInformationAndStartIfNeeded();
		} 
	}

	public void toggleFullScreen() {
		if (btnToggleScreen != null) {
			btnToggleScreen.performClick();
		}
	}

	protected boolean isSurfaceCreated = false;

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {	
		isSurfaceCreated = true;
		if (mService != null) {
			mService.changeSurfaceView(surfaceHolder);
			showFilmInformationAndStartIfNeeded();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		
		isSurfaceCreated = false;
		if (mService != null
				&& mService.getSurfaceHolder() == this.surfaceHolder) {
			mService.changeSurfaceView(null);
		} 
		surfaceHolder = null;
	}

	private void seekTo(int timeToSeek, int displayTime) {
		mService.seekVideoTo(timeToSeek);
		sbProgress.setProgress(displayTime);
		tvPlayedTime.setText(Utility.convertTimeToDisplayedTime(displayTime));
		pbLoading.setVisibility(View.VISIBLE);
	}

	
	protected void onToggleScreenClicked() {
		pausePlayingVideo();
		context.startActivity(new Intent(context, FullScreenActivity.class));
	}

	@Override
	public void onClick(View arg0) {
		timeUpdating = 0;
		switch (arg0.getId()) {
		case R.id.btnBackward:
			if (mService != null && mService.isMediaReadyForPlay()) {			
				int currentTime = mService.getCurrentVideoTime();
				seekTo(currentTime - TIME_STEP - 5, currentTime - TIME_STEP);
			}
			break;
		case R.id.btnForward:
			if (mService != null && mService.isMediaReadyForPlay()) {			
				int currentTime = mService.getCurrentVideoTime();
				seekTo(currentTime + TIME_STEP - 5, currentTime + TIME_STEP);
			}
			break;
		case R.id.btnPause:
			if (mService != null && mService.isMediaReadyForPlay()) {
				if (mService.getMediaState() == MediaState.STARTED) {
					pausePlayingVideo();
				} else {
					startPlayingVideo();
				}
			}
			break;
		case R.id.btnPreviousChapter:
			if (mService != null) {
				mService.playPreviousChapter();
			}
			break;
		case R.id.btnNextChapter:
			if (mService != null) {
				mService.playNextChapter();
			}
			break;
		case R.id.btnScreenZoom:
			if (mService != null) {
				onToggleScreenClicked();
			}
			break;
		case R.id.svVideoPlayer:
			if (viewController.getVisibility() == View.VISIBLE) {
				hideController();
			} else {
				showController();
			}
			break;
		case R.id.btnRestart:
			if (mService != null) {
				mService.restartMediaplayerIfNeed();
			}
			btnRestart.setVisibility(View.GONE);
			pbLoading.setVisibility(View.VISIBLE);
			break;
		default:
			break;
		}
	}

	@Override
	public void onProgressChanged(SeekBar arg0, int progress, boolean fromUser) {
		if (isStartTracking) {
			tvPlayedTime.setText(Utility.convertTimeToDisplayedTime(progress));
		}
	}
	private boolean isStartTracking = false;

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		isStartTracking = true;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		isStartTracking = false;
		seekTo(sbProgress.getProgress(), sbProgress.getProgress());
	
	}
}
