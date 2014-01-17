package com.movie.filmtube.adapters;
import java.util.List;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.movie.filmtube.data.Category;
import com.youtube.bigbang.R;
public class CategoryAdapter extends ArrayAdapter<Category> {
	private LayoutInflater mLayoutInflater;
	public CategoryAdapter(Activity context, List<Category> objects) {
		super(context, R.layout.item_category_view, objects);
		mLayoutInflater = context.getLayoutInflater();
	}
	private class ViewHoler {
		public TextView tvName;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHoler viewHoler = null;
		if (convertView == null) {
			convertView = mLayoutInflater.inflate(R.layout.item_category_view,
					null);
			viewHoler = new ViewHoler();
			viewHoler.tvName = (TextView) convertView
					.findViewById(R.id.tvCategory);
			convertView.setTag(viewHoler);
		} else {
			viewHoler = (ViewHoler) convertView.getTag();
		}
		Category category = getItem(position);
		viewHoler.tvName.setText(category.getName());
		return convertView;
	}
}
