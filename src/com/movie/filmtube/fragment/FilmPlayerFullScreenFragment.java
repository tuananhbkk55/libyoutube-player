package com.movie.filmtube.fragment;import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.utils.MyPreferenceManager;
import com.movie.filmtube.utils.VerticalSeekBar;
import com.movie.filmtube.utils.movie.world.helper.MovieConstants.VideoQuality;
import com.youtube.bigbang.R;
public class FilmPlayerFullScreenFragment extends FilmPlayerFragment {
	private MyPreferenceManager myPreferenceManager;
	private View viewHeaderController;
	private View btnVolume;
	private View viewQualityController;
	private VerticalSeekBar sbVolume;
	private TextView tvVolume;
	private Button btnQuality720;
	private Button btnQuality360;
	@Override
	protected void init(View view) {
		myPreferenceManager = new MyPreferenceManager(context);
		tvVolume = (TextView) view.findViewById(R.id.tvVolume);
		viewQualityController = view
				.findViewById(R.id.viewVideoQualityController);
		viewHeaderController = view.findViewById(R.id.viewHeaderController);
		view.findViewById(R.id.btnBack).setOnClickListener(this);
		btnQuality360 = (Button) view.findViewById(R.id.btnQuality360);
		btnQuality720 = (Button) view.findViewById(R.id.btnQuality720);
		btnVolume = view.findViewById(R.id.btnVolume);
		sbVolume = (VerticalSeekBar) view.findViewById(R.id.sbVolume);
		btnQuality360.setOnClickListener(this);
		btnQuality720.setOnClickListener(this);
		btnVolume.setOnClickListener(this);
		setupVolumeListener();
		super.init(view);
	}

	public void pausePlayingFilm() {
		pausePlayingVideo();
	}
	private void setupVolumeListener() {
		sbVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				float volume = seekBar.getProgress() / 100f;
				myPreferenceManager.setVolume(volume);
				tvVolume.setText("" + (int) (volume * 100));
				if (mService != null) {
					mService.setVolume(volume);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					tvVolume.setText("" + progress);
				}
			}
		});
	}

	private void hideCurrentVolumeBar() {
		sbVolume.setVisibility(View.GONE);
		tvVolume.setVisibility(View.GONE);
	}

	@Override
	protected void hideController() {
		super.hideController();
		if (viewHeaderController != null) {
			viewHeaderController.setVisibility(View.GONE);
			viewQualityController.setVisibility(View.GONE);
		}

		hideCurrentVolumeBar();
	}

	@Override
	protected void showController() {
		super.showController();
		if (viewHeaderController != null) {
			viewHeaderController.setVisibility(View.VISIBLE);
			btnVolume.setVisibility(View.VISIBLE);
			viewQualityController.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void showPlayControllerState() {
		super.showPlayControllerState();
		btnQuality360.setBackgroundResource(R.drawable.quality_360);
		btnQuality720.setBackgroundResource(R.drawable.quality_720);
		switch (mService.getVideoQuality()) {
		case High:
			btnQuality720.setBackgroundResource(R.drawable.quality_720_1);
			break;
		case Low:
		case Medium:
			btnQuality360.setBackgroundResource(R.drawable.quality_360_1);
		default:
			break;
		}
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.btnQuality360:
			if (mService != null) {
				mService.setVideoQuality(VideoQuality.Medium);
				btnQuality720.setBackgroundResource(R.drawable.quality_720);
				btnQuality360.setBackgroundResource(R.drawable.quality_360_1);
			}
			break;
		case R.id.btnQuality720:
			if (mService != null) {
				mService.setVideoQuality(VideoQuality.High);
				btnQuality720.setBackgroundResource(R.drawable.quality_720_1);
				btnQuality360.setBackgroundResource(R.drawable.quality_360);
			}
			break;
		case R.id.btnVolume:
			btnVolume.setVisibility(View.GONE);
			sbVolume.setVisibility(View.VISIBLE);
			tvVolume.setVisibility(View.VISIBLE);
			int volume = (int) (myPreferenceManager.getVolume() * 100);
			sbVolume.setProgress(volume);
			tvVolume.setText("" + volume);
			break;
		case R.id.btnBack:
			context.finish();
			break;

		default:
			super.onClick(arg0);
			break;
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_film_player_fullscreen;
	}

	@Override
	protected void onToggleScreenClicked() {
		context.finish();
	}

	private boolean isScreenOrientationChanged = false;

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		isScreenOrientationChanged = true;

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (isScreenOrientationChanged) {
			pausePlayingVideo();
		}
	}

	public static FilmPlayerFullScreenFragment newInstance(FilmYoutube film) {
		FilmPlayerFullScreenFragment filmPlayerFullScreenFragment = new FilmPlayerFullScreenFragment();
		filmPlayerFullScreenFragment.setmFilmYoutube(film);
		return filmPlayerFullScreenFragment;
	}

}
