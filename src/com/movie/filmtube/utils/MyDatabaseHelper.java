package com.movie.filmtube.utils;
import android.content.Context;
import com.movie.filmtube.data.DaoMaster;
import com.movie.filmtube.data.DaoMaster.DevOpenHelper;
import com.movie.filmtube.data.DaoSession;
public class MyDatabaseHelper {
	private DevOpenHelper mDevOpenHelper;
	private DaoMaster mDaoMaster;
	private DaoSession mSession;
	private static MyDatabaseHelper myDatabaseHelper;
	private MyDatabaseHelper(Context context) {
		mDevOpenHelper = new DevOpenHelper(context.getApplicationContext(),
				Constants.DATABASE_NAME, null);
		mDaoMaster = new DaoMaster(mDevOpenHelper.getWritableDatabase());
		mSession = mDaoMaster.newSession();
	}
	public static MyDatabaseHelper getInstance(Context context) {
		if (myDatabaseHelper == null) {
			myDatabaseHelper = new MyDatabaseHelper(context);
		}
		return myDatabaseHelper;
	}
	public DaoSession getmSession() {
		return mSession;
	}
	public DaoMaster getmDaoMaster() {
		return mDaoMaster;
	}
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		myDatabaseHelper = null;
		mDaoMaster = null;
		mDevOpenHelper.close();
		mDevOpenHelper = null;

	}
}
