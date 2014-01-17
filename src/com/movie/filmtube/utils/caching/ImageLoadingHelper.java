package com.movie.filmtube.utils.caching;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.ByteArrayBuffer;

import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.youtube.bigbang.BuildConfig;

public class ImageLoadingHelper implements ComponentCallbacks {

	private static final String TAG = "ImageLoadingHelper";
	public static final boolean isDebug = false;
	// Only run 2 task parallel
	private static final int MAX_RUNNING_TASTKS = 28;

	private Context context;
	// Width and height of image should be load to cache
	private int width = 100;
	private int height = 100;

	private boolean isDiskCachEnable = false;

	/** Default image for imageview if loading fail */
	private int resIdDefault = 0;
	// Cache memory
	// private LruCache<Integer, Bitmap> mLruCache;
	private android.support.v4.util.LruCache<Integer, ImageDrawable> mLruCache;
	private SparseIntArray mBitmapReferenceCount = new SparseIntArray();

	// Directory of cach folder
	private File mCachingFolder;

	// number of current running task
	private int numTaskRunning = 0;

	private Resources mResources;

	// List of task holder
	private List<ImageViewTaskHolder> listOfLoadingTasks = new ArrayList<ImageLoadingHelper.ImageViewTaskHolder>();


	private static File getCachingFolder(Context context) {
		File file = new File(ImageUtility.getDownloadFolder(context)
				+ File.separator + ImageConstant.CACHING_FORLDER);
		if (!file.exists() || !file.isDirectory()) {
			file.mkdir();
		}
		return file;
	}


	private String getFilePathFromUrl(String url) {
		return mCachingFolder.getPath() + File.separator + url.hashCode();
	}


	private int getKeyFromPath(String path) {
		return path.hashCode();
	}


	private void hideProgressBar(final ImageView imageView) {
		Object tag = imageView.getTag();
		if (tag != null && tag instanceof ProgressBar) {
			((ProgressBar) tag).setVisibility(View.INVISIBLE);
		} else if (tag != null && tag instanceof ImageViewTaskHolder
				&& ((ImageViewTaskHolder) tag).getProgressbar() != null) {
			((ImageViewTaskHolder) tag).getProgressbar().setVisibility(
					View.INVISIBLE);
			// imageView.setTag(((ImageViewTaskHolder) tag).getProgressbar());
		}
	}


	private void showProgressBar(final ImageView imageView) {
		Object tag = imageView.getTag();
		if (tag != null && tag instanceof ProgressBar) {
			((ProgressBar) tag).setVisibility(View.VISIBLE);
		} else if (tag != null && tag instanceof ImageViewTaskHolder
				&& ((ImageViewTaskHolder) tag).getProgressbar() != null) {
			((ImageViewTaskHolder) tag).getProgressbar().setVisibility(
					View.VISIBLE);
		}
	}


	private void startNextTaskInStack() {
		int size = listOfLoadingTasks.size();
		if (numTaskRunning < MAX_RUNNING_TASTKS && size > 0) {
			ImageViewTaskHolder imageViewTaskHolder = listOfLoadingTasks
					.remove(size - 1);
			imageViewTaskHolder.task
					.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR);
		} else {
		
		}
	}

	private class DownloadAndDisplayBitmapTask extends
			AsyncTask<Void, Void, Boolean> {

		private WeakReference<ImageView> weakReference;
		private String imageUrl;
		private String filePath;
		private int key;
		private boolean isDownloaded = false;

		public DownloadAndDisplayBitmapTask(ImageView imageView, String url) {
			this.weakReference = new WeakReference<ImageView>(imageView);
			this.imageUrl = url;
			this.filePath = getFilePathFromUrl(imageUrl);
			key = getKeyFromPath(filePath);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			numTaskRunning++;
			if (getBitmapUrlFromCache(imageUrl) != null) {
				return true;
			} else if (weakReference.get() == null || isCancelled()) {
				return false;
			}
			File file = new File(filePath);
			if (file.exists()) {
				isDownloaded = true;
				return true;
			}
			if (isDiskCachEnable) {
				try {
					URL url = new URL(imageUrl); 
					URLConnection ucon = url.openConnection();
					InputStream is = ucon.getInputStream();
					BufferedInputStream bis = new BufferedInputStream(is);
					ByteArrayBuffer baf = new ByteArrayBuffer(5000);
					int current = 0;
					while ((current = bis.read()) != -1) {
						if (weakReference.get() == null || isCancelled()) {				
							is.close();
							bis.close();
							file.delete();
							return false;
						}
						baf.append((byte) current);
					}
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(baf.toByteArray());
					fos.flush();
					fos.close();
					return true;
				} catch (IOException e) {
					e.printStackTrace();
					file.delete();
				}
			} else {
				try {
					Bitmap bitmap = ImageHelper.decodeToSampleBitmap(imageUrl,
							width, height);
					if (bitmap == null) {					
						return false;
					}
					if (weakReference.get() == null || isCancelled()) {
						return false;
					}			
					
					mLruCache.put(key, new ImageDrawable(mResources, bitmap));
					return true;
				} catch (Exception e) {
					if (isDebug) {
						e.printStackTrace();
					}
					return false;
				}

			}
			return false;
		}

		@Override
		protected void onCancelled(Boolean result) {
			super.onCancelled(result);
			numTaskRunning--;
			startNextTaskInStack();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			numTaskRunning--;
			if (result && weakReference.get() != null) {			
				if (isDiskCachEnable || isDownloaded) {
					displayBitmapFromLocalPath(weakReference.get(), filePath);
				} else {
					displayImageViewWithCache(weakReference.get(), key);
					startNextTaskInStack();
				}
			} else if (weakReference.get() != null) {
				displayDefaultImage(weakReference.get());			
				startNextTaskInStack();
			}
		}
	}


	private BitmapDrawable getBitmapUrlFromCache(String url) {
		String path = getFilePathFromUrl(url);
		return getDrawableFromCache(getKeyFromPath(path));
	}

	private BitmapDrawable getDrawableFromCache(int key) {
		BitmapDrawable drawable = mLruCache.get(Integer.valueOf(key));
		if (drawable != null) {
			if (drawable.getBitmap().isRecycled()) {
				mLruCache.remove(Integer.valueOf(key));
				return null;
			}
		}
		return drawable;
	}
	private boolean displayImageViewWithCache(final ImageView imageView,
			final int key) {
		BitmapDrawable bitmapDrawable = getDrawableFromCache(key);
		if (bitmapDrawable == null) {
			mBitmapReferenceCount.put(key, 0);
			imageView.setImageBitmap(null);			
		} else {
			imageView.setImageDrawable(bitmapDrawable);
			hideProgressBar(imageView);
		}
		return false;
	}
	private void displayDefaultImage(final ImageView imageView) {	
		if (resIdDefault <= 0) {
			return;
		}
		loadImageView(imageView, resIdDefault);

	}
	private void displayBitmapFromUrl(final ImageView imageView, String url) {
		int key = getKeyFromPath(getFilePathFromUrl(url));
		if (displayImageViewWithCache(imageView, key)) {
			if (isDebug)
				Log.i(TAG, "url Bitmap is in cache: url = " + url);
		} else {
			pushTaskToStack(imageView, new DownloadAndDisplayBitmapTask(
					imageView, url), url);
		}
		startNextTaskInStack();
	}
	private class DisplayLocalImageTask extends AsyncTask<Void, Void, Boolean> {

		private WeakReference<ImageView> weakReference;
		private String filePath;
		private int key;

		public DisplayLocalImageTask(ImageView imageView, String filePath) {
			this.filePath = filePath;
			this.weakReference = new WeakReference<ImageView>(imageView);
			key = getKeyFromPath(filePath);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			numTaskRunning++;
			if (weakReference.get() == null || isCancelled()) {				
				return false;
			}
			if (getDrawableFromCache(key) != null) {
				return true;
			}
			if (isDebug)
			Options options = new Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filePath, options);
			int sampleSize = ImageHelper.getSample(options, width, height);
			if (weakReference.get() == null || isCancelled()) {
				return false;
			}
			options.inSampleSize = sampleSize;
			options.inJustDecodeBounds = false;
			Bitmap bitmap = null;
			try {
				bitmap = BitmapFactory.decodeFile(filePath, options);
			} catch (OutOfMemoryError error) {
				error.printStackTrace();
				clearCache();
			}
			if (bitmap == null) {
				try {
					bitmap = BitmapFactory.decodeFile(filePath, options);
				} catch (OutOfMemoryError error) {
					error.printStackTrace();
				}
			}

			if (bitmap == null) {			
				return false;
			}
			if (weakReference.get() == null || isCancelled()) {				
				return false;
			}
			mLruCache.put(Integer.valueOf(key), new ImageDrawable(mResources,
					bitmap));
			return true;
		}

		@Override
		protected void onCancelled(Boolean result) {
			super.onCancelled(result);
			numTaskRunning--;
			startNextTaskInStack();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			numTaskRunning--;
			startNextTaskInStack();
			if (result && weakReference.get() != null) {				
				displayImageViewWithCache(weakReference.get(), key);
			} else if (weakReference.get() != null) {
				displayDefaultImage(weakReference.get());				

		}
	}
	private void displayBitmapFromLocalPath(ImageView imageView,
			String localPath) {
		if (displayImageViewWithCache(imageView, getKeyFromPath(localPath))) {
		} else {
			pushTaskToStack(imageView, new DisplayLocalImageTask(imageView,
					localPath), localPath);
		}
		startNextTaskInStack();

	}


	private class DisplayResourceImageTask extends
			AsyncTask<Void, Void, Boolean> {
		private WeakReference<ImageView> weakReference;
		private int resId;

		public DisplayResourceImageTask(ImageView iv, int resId) {
			this.weakReference = new WeakReference<ImageView>(iv);
			this.resId = resId;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			numTaskRunning++;
			if (weakReference.get() == null || isCancelled()) {
				return false;
			}
			if (getDrawableFromCache(resId) != null) {
				return true;
			}
			Options options = new Options();
			options.inJustDecodeBounds = true;
			BitmapFactory
					.decodeResource(context.getResources(), resId, options);
			int sampleSize = ImageHelper.getSample(options, width, height);
			if (weakReference.get() == null || isCancelled()) {
				return false;
			}
			options.inSampleSize = sampleSize;
			options.inJustDecodeBounds = false;
			Bitmap bitmap = null;
			try {
				bitmap = BitmapFactory.decodeResource(context.getResources(),
						resId, options);
			} catch (OutOfMemoryError error) {
				error.printStackTrace();
				clearCache();
			}
			if (bitmap == null) {
				try {
					bitmap = BitmapFactory.decodeResource(
							context.getResources(), resId, options);
				} catch (OutOfMemoryError error) {
					error.printStackTrace();
				}
			}
			if (bitmap == null || weakReference.get() == null || isCancelled()) {
				return false;
			}
			mLruCache.put(Integer.valueOf(resId), new ImageDrawable(mResources,
					bitmap));
			return true;
		}

		@Override
		protected void onCancelled(Boolean result) {
			super.onCancelled(result);
			numTaskRunning--;
			startNextTaskInStack();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			numTaskRunning--;
			startNextTaskInStack();
			if (result && weakReference.get() != null) {
				displayImageViewWithCache(weakReference.get(), resId);
			} else if (weakReference.get() != null) {
				displayDefaultImage(weakReference.get());
			}
		}

	}
	private void displayBitmapFromDrawableResource(final ImageView imageView,
			int resId) {
		if (displayImageViewWithCache(imageView, resId)) {
		} else {
			pushTaskToStack(imageView, new DisplayResourceImageTask(imageView,
					resId), resId);
		}
		startNextTaskInStack();
	}
	private class DisplayStreamBitmap extends AsyncTask<Void, Void, Boolean> {

		private WeakReference<ImageView> weakReference;
		private String assetsPath;
		private int key;

		public DisplayStreamBitmap(ImageView imageView, String assetsPath) {
			weakReference = new WeakReference<ImageView>(imageView);
			this.assetsPath = assetsPath;
			this.key = getKeyFromPath(assetsPath);
		}
		@Override
		protected Boolean doInBackground(Void... params) {
			numTaskRunning++;
			if (weakReference.get() == null || isCancelled()) {				
				return false;
			}
			if (getDrawableFromCache(key) != null) {
				return true;
			}

			try {
				InputStream is = context.getAssets().open(assetsPath);				
				Options options = new Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(is, null, options);
				int sampleSize = ImageHelper.getSample(options, width, height);
				if (weakReference.get() == null || isCancelled()) {
					is.close();
					return false;
				}
				options.inSampleSize = sampleSize;
				options.inJustDecodeBounds = false;
				Bitmap bitmap = null;
				try {
					bitmap = BitmapFactory.decodeStream(is, null, options);
				} catch (OutOfMemoryError error) {
					error.printStackTrace();
					clearCache();
				}
				if (bitmap == null) {
					try {
						bitmap = BitmapFactory.decodeStream(is, null, options);
					} catch (OutOfMemoryError error) {
						error.printStackTrace();
					}
				}
				if (bitmap == null) {
					is.close();
					return false;
				}
				if (weakReference.get() == null || isCancelled()) {
					is.close();
					return false;
				}
				mLruCache.put(Integer.valueOf(key), new ImageDrawable(
						mResources, bitmap));
				is.close();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void onCancelled(Boolean result) {
			super.onCancelled(result);
			numTaskRunning--;
			startNextTaskInStack();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			numTaskRunning--;
			startNextTaskInStack();
			if (result && weakReference.get() != null) {
				displayImageViewWithCache(weakReference.get(), key);
			} else if (weakReference.get() != null) {
				displayDefaultImage(weakReference.get());
			

			}
		}

	}

	private void pushTaskToStack(ImageView imageView,
			AsyncTask<Void, Void, Boolean> task, Object object) {
		ProgressBar progressBar = null;
		if (imageView.getTag() instanceof ProgressBar) {
			progressBar = (ProgressBar) imageView.getTag();
		} else if (imageView.getTag() instanceof ImageViewTaskHolder) {
			ImageViewTaskHolder taskHolder = ((ImageViewTaskHolder) imageView
					.getTag());
			if (taskHolder.task.getStatus() == AsyncTask.Status.RUNNING) {
				if (taskHolder.object instanceof Integer
						&& object instanceof Integer
						&& (Integer) taskHolder.object == (Integer) object) {
					return;
				} else if (taskHolder.object instanceof String
						&& object instanceof String
						&& ((String) taskHolder.object).equals((String) object)) {
					return;
				} else {
					taskHolder.task.cancel(true);
				}
			}
			progressBar = taskHolder.getProgressbar();
			listOfLoadingTasks.remove(taskHolder);
		}

		ImageViewTaskHolder imageViewTaskHolder = new ImageViewTaskHolder();
		imageViewTaskHolder.setProgressbar(progressBar);
		imageViewTaskHolder.task = task;
		imageViewTaskHolder.object = object;
		imageView.setTag(imageViewTaskHolder);
		listOfLoadingTasks.add(imageViewTaskHolder);		
	}

	private static class ImageViewTaskHolder {
		public Object object;
		public AsyncTask<Void, Void, Boolean> task;
		private WeakReference<ProgressBar> progressBarWeakReference;
		public void setProgressbar(ProgressBar progressBar) {
			progressBarWeakReference = new WeakReference<ProgressBar>(
					progressBar);
		}
		public ProgressBar getProgressbar() {
			return progressBarWeakReference.get();
		}

	}

	public void loadImageView(final ImageView imageView, Object object) {
		showProgressBar(imageView);
		if (object instanceof Integer) {
			displayBitmapFromDrawableResource(imageView, (Integer) object);
		} else if (object instanceof String) {
			String url = (String) object;
			if (url.startsWith("http")) {
				displayBitmapFromUrl(imageView, url);
			} else {
				displayBitmapFromLocalPath(imageView, url);
			}
		} else {
			if (BuildConfig.DEBUG) {
				throw new IllegalArgumentException(
						"loadImageView - Fail to load image because of wrong object argument passed: "
								+ object.getClass().getName());
			} else {
				Log.e(TAG,
						"loadImageView - Fail to load image because of wrong object argument passed: "
								+ object.getClass().getName());
			}
		}
	}

	public void loadImageStream(final ImageView imageView,
			final String assetsImagePath) {
		// decrease reference of this image view
		// decreaseBitmapRefer(getKeyFromPreviousTask(imageView));
		showProgressBar(imageView);
		int key = getKeyFromPath(assetsImagePath);
		if (displayImageViewWithCache(imageView, key)) {
			// Load bit map from memory successfully
		} else {
			pushTaskToStack(imageView, new DisplayStreamBitmap(imageView,
					assetsImagePath), assetsImagePath);
		}
		startNextTaskInStack();
	}

	public void setDefaultLoadedImageId(int resId) {
		this.resIdDefault = resId;
	}
	public void clearCache() {
		
		for (ImageViewTaskHolder imageViewTaskHolder : listOfLoadingTasks) {
			imageViewTaskHolder.task.cancel(true);
		}
		listOfLoadingTasks.clear();
		mLruCache.evictAll();
	}
	private void initCaching(int partOfMemoryUsed) {
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cachSize = maxMemory / partOfMemoryUsed;
		mLruCache = new LruCache<Integer, ImageDrawable>(cachSize) {
			@Override
			protected int sizeOf(Integer key, ImageDrawable value) {
				int size = getBitmapSize(value.getBitmap()) / 1024;
				return size;
			}
			@Override
			protected void entryRemoved(boolean evicted, Integer key,
					ImageDrawable oldValue, ImageDrawable newValue) {
				super.entryRemoved(evicted, key, oldValue, newValue);
				oldValue.setCached(false);
				if (oldValue.getRefCount() <= 0 && !ImageUtility.hasHoneycomb()) {
					oldValue.getBitmap().recycle();
					oldValue = null;
				}
			}

		};
		mCachingFolder = getCachingFolder(context);
	}
	private int getBitmapSize(Bitmap value) {
		if (ImageUtility.hasHoneycombMR1()) {
			return value.getByteCount();
		}
		return value.getRowBytes() * value.getHeight();
	}
	public ImageLoadingHelper(Context context, int partOfMemoryUsed) {
		this.context = context.getApplicationContext();
		this.mResources = context.getResources();
		initCaching(partOfMemoryUsed);
	}

	public ImageLoadingHelper(Context context) {
		this.context = context.getApplicationContext();
		this.mResources = context.getResources();
		initCaching(4);
	}

	public ImageLoadingHelper(Context context, int partOfMemoryUsed, int width,
			int height) {
		this.context = context;
		this.width = width;
		this.height = height;
		this.mResources = context.getResources();
		initCaching(partOfMemoryUsed);
	}

	public void setWidth(int width) {
		this.width = width;
	}
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

	}

	@Override
	public void onLowMemory() {

	}
	public void setDiskCachEnable(boolean isDiskCachEnable) {
		this.isDiskCachEnable = isDiskCachEnable;
	}
	public static class ImageDrawable extends BitmapDrawable {

		public ImageDrawable(Resources res, Bitmap bitmap) {
			super(res, bitmap);
		}

		private boolean isCached = true;
		private int refCount = 0;

		public void increaseRef() {
			refCount++;
		}
		public void decreaseRefAndRecycleIfNeed() {
			if (refCount > 0) {
				refCount--;
			}
			if (refCount <= 0 && !isCached && !getBitmap().isRecycled()
					&& !ImageUtility.hasHoneycomb()) {				
				getBitmap().recycle();
			}
		}

		public int getRefCount() {
			return refCount;
		}

		public boolean isCached() {
			return isCached;
		}

		public void setCached(boolean isCached) {
			this.isCached = isCached;
		}

	}
}
