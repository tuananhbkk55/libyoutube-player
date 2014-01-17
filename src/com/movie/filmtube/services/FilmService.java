package com.movie.filmtube.services;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.data.FilmYoutubeDao;
import com.movie.filmtube.utils.Constants;
import com.movie.filmtube.utils.Constants.MediaState;
import com.movie.filmtube.utils.DownloadStatus;
import com.movie.filmtube.utils.MyDatabaseHelper;
import com.movie.filmtube.utils.MyPreferenceManager;
import com.movie.filmtube.utils.Utility;
import com.movie.filmtube.utils.caching.ImageUtility;
import com.movie.filmtube.utils.movie.world.helper.MovieConstants.VideoQuality;
import com.youtube.bigbang.BuildConfig;
public class FilmService extends Service implements OnCompletionListener,
		OnBufferingUpdateListener, OnPreparedListener, OnErrorListener,
		OnInfoListener, OnSeekCompleteListener, OnVideoSizeChangedListener {
	private static final String TAG = "FilmService";
	private LocalBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		public FilmService getService() {
			return FilmService.this;
		}
	}
	private List<FilmYoutube> mFilmYoutubes = new ArrayList<FilmYoutube>();
	private int mPosition = 0;
	private MyPreferenceManager mPreferenceManager;
	private FilmYoutubeDao mFilmYoutubeDao;
	private MediaPlayer mediaPlayer;
	private MediaState mediaState = MediaState.END;
	private VideoQuality mVideoQuality = VideoQuality.High;
	private SurfaceHolder mSurfaceHolder;
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Constants.ACTION_DOWNLOAD_VIDEO_CANCELED.equals(action)) {
				String videoId = intent
						.getStringExtra(Constants.KEY_CHAPTER_UPDATE);
				if (!TextUtils.isEmpty(videoId) && mFilmYoutubes != null) {
					for (FilmYoutube filmYoutube : mFilmYoutubes) {
						if (videoId.equals(filmYoutube.getYoutubeId())) {
							filmYoutube
									.setDownloadStatus(DownloadStatus.NOT_DOWNLOADED);
							break;
						}
					}
				}
			} else if (Constants.ACTION_DOWNLOAD_VIDEO.equals(action)) {				
				String videoId = intent
						.getStringExtra(Constants.KEY_CHAPTER_UPDATE);
				FilmYoutube filmYoutube = null;
				for (FilmYoutube film : mFilmYoutubes) {
					if (film.getYoutubeId().equals(videoId)) {
						filmYoutube = film;
						break;
					}
				}
				if (filmYoutube != null) {
					int downloadStatus = intent.getIntExtra(
							Constants.KEY_STATUS_UPDATE, 0);
					int fileSize = intent.getIntExtra(Constants.KEY_FILE_SIZE,
							0);
					filmYoutube.setFileSizeInMB(fileSize);
					filmYoutube.setDownloadStatus(downloadStatus);
				}
			}
		}
	};

	private boolean isBroadcastRegisterd = false;
	private void initIfNeeded() {
		if (mPreferenceManager == null) {
			mPreferenceManager = new MyPreferenceManager(
					getApplicationContext());
		}
		mFilmYoutubeDao = MyDatabaseHelper.getInstance(getApplicationContext())
				.getmSession().getFilmYoutubeDao();
		if (!isBroadcastRegisterd) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(Constants.ACTION_DOWNLOAD_VIDEO_CANCELED);
			filter.addAction(Constants.ACTION_DOWNLOAD_VIDEO);
			registerReceiver(mBroadcastReceiver, filter);
			isBroadcastRegisterd = true;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		initIfNeeded();
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		initIfNeeded();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (isBroadcastRegisterd) {
			unregisterReceiver(mBroadcastReceiver);
			isBroadcastRegisterd = false;
		}
	}
	public void stopService() {
		isStoped = true;
		this.releaseMediaPlayer();
		stopSelf();
	}
	private void prepareMediaPlayer() {
		if (mThreadRunning != null) {
			mThreadRunning.interrupt();
		}
		if (this.mFilmYoutubes.get(mPosition) != null) {
			final FilmYoutube filmYoutube = mFilmYoutubes.get(mPosition);			
			this.releaseMediaPlayer();
			if (TextUtils.isEmpty(filmYoutube.getYoutubeId())) {
				onError(mediaPlayer, 0, 0);
				Toast.makeText(
						getApplicationContext(),
						"The chapter's url is broken. Please, watch another chapter.",
						Toast.LENGTH_SHORT).show();
				return;
			}
			this.mediaPlayer = new MediaPlayer();
			this.mediaState = MediaState.IDLE;
			this.mediaPlayer.setLooping(true);
			if (mPreferenceManager == null) {
				this.setVolume((new MyPreferenceManager(getApplicationContext()))
						.getVolume());
			} else {
				this.setVolume(mPreferenceManager.getVolume());
			}

			mediaPlayer.setOnCompletionListener(this);
			mediaPlayer.setOnBufferingUpdateListener(this);
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setOnErrorListener(this);
			mediaPlayer.setOnInfoListener(this);
			mediaPlayer.setOnSeekCompleteListener(this);
			mediaPlayer.setOnVideoSizeChangedListener(this);
			if (mSurfaceHolder != null) {
				this.mediaPlayer.setDisplay(mSurfaceHolder);
			}
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			try {
				String localFilePath = Utility.getFilmLocalPathFromUrl(this,
						filmYoutube.getYoutubeId());

				File file = new File(localFilePath);
				if (file.exists()) {					
					mediaPlayer.setDataSource(this, Uri.fromFile(file));
					mediaPlayer.prepareAsync();
				} else {
					final String url = getStreamingUrl();
					if (TextUtils.isEmpty(url)) {
						parseLinksAndPlay(filmYoutube);
					} else {
						mediaPlayer.setDataSource(url);
						mediaPlayer.prepareAsync();
					}
				}
				sendBroadcast(new Intent(
						Constants.ACTION_MEDIA_WAITING_FOR_LOADING));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	private String getStreamingUrl() {
		String url = "";
		FilmYoutube filmYoutube = getCurrentPlayingFilm();
		if (filmYoutube != null) {
			url = getStreamingUrlBasedOnQuality(this.mVideoQuality);
			if (TextUtils.isEmpty(url)) {
				url = TextUtils.isEmpty(filmYoutube.getStreamingUrlHQ()) ? filmYoutube
						.getStreamingUrlMedium() : filmYoutube
						.getStreamingUrlHQ();
			}
		}

		return url;
	}

	private String getStreamingUrlBasedOnQuality(VideoQuality videoQuality) {
		String url = "";
		FilmYoutube filmYoutube = getCurrentPlayingFilm();
		switch (this.mVideoQuality) {
		case High:
			url = filmYoutube.getStreamingUrlHQ();
			break;
		case Medium:
		case Low:
			url = filmYoutube.getStreamingUrlMedium();
			break;
		default:
			break;
		}
		return url;
	}

	private void parseLinksAndPlay(final FilmYoutube filmYoutube) {
		mThreadRunning = new Thread(new Runnable() {

			@Override
			public void run() {
				if (Utility.getVideoStreamingUrls(filmYoutube)) {
					if (mThreadRunning != null
							&& !mThreadRunning.isInterrupted()) {
						FilmYoutube filmYoutube = getCurrentPlayingFilm();
						if (filmYoutube != null && filmYoutube.getId() == null) {
							FilmYoutube filmYoutube2 = mFilmYoutubeDao
									.getFilmBasedOnVideoId(filmYoutube
											.getYoutubeId());
							if (filmYoutube2 != null) {
								filmYoutube.setId(filmYoutube2.getId());
								filmYoutube.setIsBookmarked(filmYoutube2
										.getIsBookmarked());
								mFilmYoutubeDao.update(filmYoutube);
							}
						}
						prepareMediaPlayer();
						mThreadRunning = null;
					}
				} else {
					if (mThreadRunning != null
							&& !mThreadRunning.isInterrupted()) {
						mThreadRunning = null;
						onError(mediaPlayer, 0,
								Constants.MEDIA_ERROR_CANT_FIND_LINK);
					}
				}

			}
		});
		mThreadRunning.start();
	}
	private Thread mThreadRunning;
	private Handler mUpdateProgressHandler = new Handler();
	private boolean isUpdatingProgress = false;
	private int mPlayedTime = 0;
	public static final int TIME_SECONDS = 1000;
	private void updateProgressHandler() {
		mUpdateProgressHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (isStoped) {
					stopSelf();
					return;
				}
				if (getMediaState() == MediaState.STARTED && isUpdatingProgress) {
					mPlayedTime = getCurrentVideoTime();
					updateProgressHandler();
					sendBroadcast(new Intent(
							Constants.ACTION_MEDIA_PROGRESS_UPDATED));
					if (mSurfaceHolder == null) {
						sendBroadcast(new Intent(
								Constants.ACTION_UPDATE_SURFACE_VIEW));
					}
				}
			}
		}, TIME_SECONDS);
	}
	private void startUpdateProgressIfNotStarted() {
		if (!isUpdatingProgress) {
			isUpdatingProgress = true;
			updateProgressHandler();
		}
	}
	private void stopUpdateProgress() {
		mUpdateProgressHandler.removeCallbacksAndMessages(null);
		isUpdatingProgress = false;
	}

	public boolean startPlayingVideo() {
		if (this.mediaPlayer != null && isMediaReadyForPlay()) {
			if (!mediaPlayer.isPlaying()) {
				mediaPlayer.start();
				sendBroadcast(new Intent(
						Constants.ACTION_MEDIA_RELOAD_CONTROLLER));
				if (BuildConfig.DEBUG) {
					Log.i(TAG, "startPlayingVideo");
				}
			}
			mediaState = MediaState.STARTED;
			startUpdateProgressIfNotStarted();
			return true;
		}
		return false;
	}
	public boolean pausePlayingVideo() {
		stopUpdateProgress();
		if (this.mediaState == MediaState.STARTED && this.mediaPlayer != null) {
			this.mediaPlayer.pause();
			mediaState = MediaState.PAUSED;
			sendBroadcast(new Intent(Constants.ACTION_MEDIA_RELOAD_CONTROLLER));
			if (BuildConfig.DEBUG) {
				Log.i(TAG, "pausePlayingVideo");
			}
			return true;
		}
		return false;
	}

	public void releaseMediaPlayer() {
		stopUpdateProgress();
		if (this.mediaPlayer != null) {
			this.mediaPlayer.release();
			this.mediaPlayer = null;
			this.mediaState = MediaState.END;
		}

	}
	public void seekVideoTo(final int time) {
		if (this.mediaPlayer != null && isMediaReadyForPlay()) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					Log.i(TAG, "seek to: " + time);
					mediaPlayer.seekTo(time);
					stopUpdateProgress();
				}
			}).start();
		}
	}
	public int getTotalVideoTime() {
		if (this.mediaPlayer != null
				&& (this.mediaState == MediaState.PAUSED
						|| this.mediaState == MediaState.PREPARED || this.mediaState == MediaState.STARTED)) {
			return this.mediaPlayer.getDuration();
		}
		return -1;
	}

	public int getCurrentVideoTime() {
		if (this.mediaPlayer != null
				&& (this.mediaState == MediaState.PAUSED || this.mediaState == MediaState.STARTED)) {
			return mediaPlayer.getCurrentPosition();
		}
		return 0;
	}

	public void playChapterAtPosition(int position) {
		if (position >= mFilmYoutubes.size() || position < 0) {
			Toast.makeText(this, "No next or previous chapter",
					Toast.LENGTH_SHORT).show();
			return;
		} else {
			mPosition = position;
			stopUpdateProgress();
			mPlayedTime = 0;
			mediaState = MediaState.IDLE;
			sendBroadcast(new Intent(Constants.ACTION_MEDIA_VIDEO_CHANGED));
			prepareMediaPlayer();
		}
	}

	public void playLastChapter() {
		if (mFilmYoutubes == null || mFilmYoutubes.size() <= 0) {
			return;
		}
		playChapterAtPosition(mFilmYoutubes.size() - 1);
	}
	public void playFirstChapter() {
		if (mFilmYoutubes == null || mFilmYoutubes.size() <= 0) {
			return;
		}
		playChapterAtPosition(0);
	}
	public void playNextChapter() {
		if (mFilmYoutubes == null || mFilmYoutubes.size() <= 0) {
			return;
		}
		playChapterAtPosition(mPosition + 1);
	}
	public void playPreviousChapter() {
		if (mFilmYoutubes == null || mFilmYoutubes.size() <= 0) {
			return;
		}
		playChapterAtPosition(mPosition - 1);
	}
	public List<FilmYoutube> getFilms() {
		return mFilmYoutubes;
	}

	public void setYoutubeFilms(List<FilmYoutube> filmYoutubes) {
		this.mFilmYoutubes = filmYoutubes;
	}
	public void restartMediaplayerIfNeed() {
		if (!isMediaReadyForPlay()) {
			prepareMediaPlayer();
		} else {
			onPrepared(mediaPlayer);
		}
	}
	public FilmYoutube getCurrentPlayingFilm() {
		if (this.mFilmYoutubes != null && mFilmYoutubes.size() > 0
				&& mPosition < mFilmYoutubes.size()) {
			return mFilmYoutubes.get(mPosition);
		} else {
			return null;
		}
	}
	public void setVolume(float volumeValue) {
		if (this.mediaPlayer != null && this.mediaState != MediaState.ERROR) {
			this.mediaPlayer.setVolume(volumeValue, volumeValue);
		}
	}
	public MediaState getMediaState() {
		return this.mediaState;
	}
	public boolean isMediaReadyForPlay() {
		if (this.mediaState == MediaState.PAUSED
				|| this.mediaState == MediaState.PREPARED
				|| this.mediaState == MediaState.STARTED) {
			return true;
		}
		return false;
	}
	public SurfaceHolder getSurfaceHolder() {
		return mSurfaceHolder;
	}
	public boolean changeSurfaceView(SurfaceHolder surfaceHolder) {
		this.mSurfaceHolder = surfaceHolder;
		if (mediaPlayer != null) {			
			mediaPlayer.setDisplay(mSurfaceHolder);
			return true;
		} else {			
			return false;
		}
	}
	public boolean isFilmPlaying(FilmYoutube film) {
		FilmYoutube filmYoutube = getCurrentPlayingFilm();
		if (filmYoutube != null
				&& film.getYoutubeId().equals(filmYoutube.getYoutubeId())) {
			return true;
		}
		return false;		
	}


	@Override
	public void onCompletion(MediaPlayer arg0) {
		this.mediaState = MediaState.STARTED;
		sendBroadcast(new Intent(Constants.ACTION_MEDIA_COMPLETE));		
	}
	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
		this.mediaState = MediaState.ERROR;
		stopUpdateProgress();
		if (ImageUtility.hasJellyBeanMR1()) {
			if (arg2 == MediaPlayer.MEDIA_ERROR_TIMED_OUT
					|| arg2 == MediaPlayer.MEDIA_ERROR_IO) {
				Toast.makeText(getApplicationContext(),
						"Network problem or The video link is broken!",
						Toast.LENGTH_LONG).show();
			}
		}
		Intent intent = new Intent(Constants.ACTION_MEDIA_ERROR);
		intent.putExtra(Constants.KEY_MEDIA_ERROR, arg2);
		sendBroadcast(intent);
		return true;
	}

	private void notifyMediaReadyToPlay() {
		sendBroadcast(new Intent(Constants.ACTION_MEDIA_READY_TO_PLAY));
		if (mPlayedTime > 0) {
			mediaPlayer.seekTo(mPlayedTime);
		}
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		if (mediaState == MediaState.PREPARED) {
			notifyMediaReadyToPlay();
		}
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		this.mediaState = MediaState.PREPARED;		
		if (this.mediaPlayer.getVideoHeight() > 0) {
			notifyMediaReadyToPlay();
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer arg0, int percent) {		
		if (percent > 0 && percent < 100) {
			Intent intent = new Intent(Constants.ACTION_MEDIA_BUFFERING_UPDATED);
			intent.putExtra(Constants.KEY_PERCENT_UPDATE, percent);
			sendBroadcast(intent);
		}
	}
	@Override
	public void onSeekComplete(MediaPlayer mp) {
		sendBroadcast(new Intent(Constants.ACTION_MEDIA_READY_TO_PLAY));
		startUpdateProgressIfNotStarted();
	}
	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {		
		if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
			sendBroadcast(new Intent(Constants.ACTION_MEDIA_WAITING_FOR_LOADING));
		} else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
			sendBroadcast(new Intent(Constants.ACTION_MEDIA_READY_TO_PLAY));
		}
		return true;
	}
	public void setPlayedTime(int playedTime) {
		this.mPlayedTime = playedTime;
	}
	public VideoQuality getVideoQuality() {
		return mVideoQuality;
	}
	public void setVideoQuality(VideoQuality videoQuality) {
		if (this.mVideoQuality != videoQuality) {
			this.mVideoQuality = videoQuality;
			String url = getStreamingUrlBasedOnQuality(videoQuality);
			if (!TextUtils.isEmpty(url)) {
				prepareMediaPlayer();
			} else {
			}
		}
	}
	private boolean isStoped = false;
}
