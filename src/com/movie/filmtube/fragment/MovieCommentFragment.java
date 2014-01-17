package com.movie.filmtube.fragment;
import java.util.ArrayList;
import java.util.List;
import android.view.View;
import android.widget.ListView;
import com.movie.filmtube.adapters.CommentsAdapter;
import com.movie.filmtube.utils.movie.world.helper.MovieComment;
import com.youtube.bigbang.R;
public class MovieCommentFragment extends MyParentFragment {
	private ListView lvList;
	private CommentsAdapter mAdapter;
	private ArrayList<MovieComment> comments = new ArrayList<MovieComment>();
	@Override
	protected void init(View view) {
		lvList = (ListView) view.findViewById(R.id.lvList);
		if (mAdapter == null) {
			mAdapter = new CommentsAdapter(context, comments);
		}
		lvList.setAdapter(mAdapter);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.fragment_comments;
	}
	public static MovieCommentFragment newInstance(List<MovieComment> list) {
		MovieCommentFragment commentFragment = new MovieCommentFragment();
		commentFragment.setComments(list);
		return commentFragment;
	}
	public void setComments(List<MovieComment> comments) {
		this.comments.clear();
		for (MovieComment comment : comments) {
			this.comments.add(comment);
		}
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}
}
