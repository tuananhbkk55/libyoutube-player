package com.movie.filmtube.utils.movie.world.helper;

public class MovieConstants {

	/**
	 * 3 Types of Video quality: <code>high, medium, low</code>
	 */
	public enum VideoQuality {
		High, Medium, Low
	}

	/** ID for searching film */
	public static final String APP_BROWSER_ID = "keyhere";

	/**
	 * Get all comment of video id. You must you
	 * {@link String#format(String, Object...)} to use this link.
	 */
	public static final String URL_GET_ALL_COMMENTS = "https://gdata.youtube.com/feeds/api/videos/%s/comments?orderby=published";
}
