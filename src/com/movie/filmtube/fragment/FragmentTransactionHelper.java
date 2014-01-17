package com.movie.filmtube.fragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
public class FragmentTransactionHelper {

	private static String TAG = "FragmentTransactionHelper";
	private FragmentActivity context;
	private int layoutId;

	private android.support.v4.app.FragmentManager mFragmentManager;

	public FragmentTransactionHelper(FragmentActivity activity, int id) {
		this.context = activity;
		this.layoutId = id;
		this.mFragmentManager = activity.getSupportFragmentManager();
	}

	public void addFragmentToView(Fragment f, boolean addToBackStack) {
		if (context.getSupportFragmentManager() != null) {
			if (addToBackStack) {
				mFragmentManager.beginTransaction().add(layoutId, f)
						.addToBackStack(null).commit();
			} else {
				Log.i(TAG, "addFragmentToView - not stack");
				mFragmentManager.beginTransaction().add(layoutId, f).commit();
			}
		}
	}
	public void replaceFragmentToView(Fragment f, boolean addToBackStack) {
		if (context.getSupportFragmentManager() != null) {
			if (addToBackStack) {
				mFragmentManager.beginTransaction().replace(layoutId, f)
						.addToBackStack(null).commit();
			} else {
				Log.i(TAG, "addFragmentToView - not stack");
				mFragmentManager.beginTransaction().replace(layoutId, f)
						.commit();
			}
		}
	}

	public void replaceFragmentWithTag(Fragment f, boolean addToBackStack,
			String tag) {
		if (context.getSupportFragmentManager() != null) {
			if (addToBackStack) {
				mFragmentManager.beginTransaction().replace(layoutId, f, tag)
						.addToBackStack(null).commit();
			} else {
				Log.i(TAG, "addFragmentToView - not stack");
				mFragmentManager.beginTransaction().replace(layoutId, f, tag)
						.commit();
			}
		}
	}

	public void popTopFragment() {
		if (context.getSupportFragmentManager() != null) {
			int count = mFragmentManager.getBackStackEntryCount();
			if (count > 0) {
				mFragmentManager.popBackStack();
			}
		}
	}

	public void showFragment(Fragment fragment) {
		mFragmentManager.beginTransaction().show(fragment).commit();
	}

	public void hideFragment(Fragment fragment) {
		mFragmentManager.beginTransaction().hide(fragment).commit();
	}

	public int getLayoutId() {
		return layoutId;
	}

	public void setLayoutId(int layoutId) {
		this.layoutId = layoutId;
	}

}
