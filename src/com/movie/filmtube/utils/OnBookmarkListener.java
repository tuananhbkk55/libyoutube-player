package com.movie.filmtube.utils;
import com.movie.filmtube.data.FilmYoutube;
public interface OnBookmarkListener {	
	public void onBookmarkedSucceed(FilmYoutube filmYoutube);
	public void onBookmarkDeleted(FilmYoutube filmYoutube);
}
