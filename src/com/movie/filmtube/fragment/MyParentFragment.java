package com.movie.filmtube.fragment;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
public abstract class MyParentFragment extends Fragment {
	protected Activity context;
	abstract protected void init(View view);
	abstract protected int getLayoutId();
	protected void onOrientationChanged(Bundle savedInstanceState) {
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			onOrientationChanged(savedInstanceState);
		}
		View view = inflater.inflate(getLayoutId(), container, false);
		init(view);
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.context = activity;
	}

}
