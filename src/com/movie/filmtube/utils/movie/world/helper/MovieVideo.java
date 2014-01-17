package com.movie.filmtube.utils.movie.world.helper;

import com.movie.filmtube.utils.movie.world.helper.MovieConstants.VideoQuality;

/**
 * Youtube video got after geting Streaming video.
 * 
 * 
 */
public class MovieVideo {
	private VideoQuality videoQuality;
	private String streamingUrl;

	public MovieVideo(VideoQuality videoQuality, String streamingUrl) {
		super();
		this.videoQuality = videoQuality;
		this.streamingUrl = streamingUrl;
	}

	public VideoQuality getVideoQuality() {
		return videoQuality;
	}

	public void setVideoQuality(VideoQuality videoQuality) {
		this.videoQuality = videoQuality;
	}

	public String getStreamingUrl() {
		return streamingUrl;
	}

	public void setStreamingUrl(String streamingUrl) {
		this.streamingUrl = streamingUrl;
	}

	@Override
	public String toString() {
		return "YoutubeVideo [videoQuality=" + videoQuality + ", streamingUrl="
				+ streamingUrl + "]";
	}

}
