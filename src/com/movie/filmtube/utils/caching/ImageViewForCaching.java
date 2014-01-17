package com.movie.filmtube.utils.caching;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import com.movie.filmtube.utils.caching.ImageLoadingHelper.ImageDrawable;
public class ImageViewForCaching extends ImageView {
	private static String TAG = "ImageViewForCaching";

	public ImageViewForCaching(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public ImageViewForCaching(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ImageViewForCaching(Context context) {
		super(context);
	}
	private void unRefPreDrawable() {
		Drawable preDrawable = getDrawable();
		if (preDrawable instanceof ImageDrawable) {			
			((ImageDrawable) preDrawable).decreaseRefAndRecycleIfNeed();
		}
	}
	@Override
	public void setImageDrawable(Drawable drawable) {
		unRefPreDrawable();
		super.setImageDrawable(drawable);
		if (drawable instanceof ImageDrawable) {			
			((ImageDrawable) drawable).increaseRef();
		}
	}
	@Override
	public void setImageBitmap(Bitmap bm) {
		unRefPreDrawable();
		super.setImageBitmap(bm);
	}
	@Override
	public void setImageResource(int resId) {
		unRefPreDrawable();
		super.setImageResource(resId);
	}
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		unRefPreDrawable();
	}

}
