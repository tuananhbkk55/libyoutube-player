package com.movie.filmtube.utils.movie.world.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Videos;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.VideoListResponse;
import com.movie.filmtube.data.FilmYoutube;
import com.movie.filmtube.helper.ServiceHelper;
import com.movie.filmtube.utils.movie.world.helper.MovieConstants.VideoQuality;
import com.youtube.bigbang.BuildConfig;

/**
 * Use this {@link MovieSearchingHelper} class to Search for video in Youtube.
 * 
 * @author kingfisher
 * 
 */
public class MovieSearchingHelper {
	private static final String TAG = "MovieWorldSearchingHelper";

	private static final String YOUTUBE_PRE_URL_LINK = "http://www.youtube.com/watch?v=";

	/** Get recent comments of video with String.format(url, videoid) */
	private static final String YOUTUBE_GET_RECENT_COMMENTS = "http://gdata.youtube.com/feeds/api/videos/%s/comments?v=2&alt=json&orderby=published";
	/** Get avatar image url from user id */
	private static final String YOUTUBE_AVATAR_IMAGE_URL = "https://i2.ytimg.com/i/%s/1.jpg";

	/** Global instance of the HTTP transport. */
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();

	/**
	 * Global instance of the max number of videos we want returned (50 = upper
	 * limit per page).
	 */
	private static final long NUMBER_OF_VIDEOS_RETURNED = 40;

	/** Global instance of Youtube object to make all API requests. */
	private static YouTube youtube;
	private YouTube.Search.List mSearch;

	// token to search next page
	private String nextPageToken = "";
	private int totalSearchResults = 0;

	public MovieSearchingHelper() {
		/*
		 * The YouTube object is used to make all API requests. The last
		 * argument is required, but because we don't need anything initialized
		 * when the HttpRequest is initialized, we override the interface and
		 * provide a no-op function.
		 */
		youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				new HttpRequestInitializer() {
					public void initialize(HttpRequest request)
							throws IOException {
					}
				}).setApplicationName("FilmTube").build();
	}

	/**
	 * Search film in youtube by key.
	 * 
	 * @param keyword
	 *            to search.
	 * @return {@link List} of {@link FilmYoutube} or empty list.
	 */
	public List<FilmYoutube> searchByKey(final String keyword) {
		List<FilmYoutube> list = new ArrayList<FilmYoutube>();
		try {

			// Get query term from user.
			mSearch = youtube.search().list("id,snippet");
			/*
			 * It is important to set your developer key from the Google
			 * Developer Console for non-authenticated requests (found under the
			 * API Access tab at this link: code.google.com/apis/). This is good
			 * practice and increased your quota.
			 */
			mSearch.setQ(keyword);
			mSearch.setKey(MovieConstants.APP_BROWSER_ID);
			/*
			 * We are only searching for videos (not playlists or channels). If
			 * we were searching for more, we would add them as a string like
			 * this: "video,playlist,channel".
			 */
			mSearch.setType("video");
			/*
			 * This method reduces the info returned to only the fields we need
			 * and makes calls more efficient.
			 */
			mSearch.setFields("pageInfo,nextPageToken,items(id/kind,id/videoId,snippet/title,snippet/publishedAt)");
			mSearch.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
			SearchListResponse searchResponse = mSearch.execute();
			// Set next page token
			this.nextPageToken = searchResponse.getNextPageToken();
			this.totalSearchResults = searchResponse.getPageInfo()
					.getTotalResults();

			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Token: " + nextPageToken);
				Log.i(TAG, "total results: " + totalSearchResults);
			}

			List<SearchResult> searchResultList = searchResponse.getItems();

			if (searchResultList != null) {
				for (SearchResult singleVideo : searchResultList) {
					ResourceId rId = singleVideo.getId();
					// Double checks the kind is video.
					if (rId.getKind().equals("youtube#video")) {
						// Thumbnail thumbnail = (Thumbnail) singleVideo
						// .getSnippet().getThumbnails().get("default");
						FilmYoutube filmYoutube = new FilmYoutube();
						filmYoutube.setYoutubeId(rId.getVideoId());
						filmYoutube
								.setName(singleVideo.getSnippet().getTitle());
						list.add(filmYoutube);
						if (BuildConfig.DEBUG) {
							Log.d(TAG, " Video Id" + filmYoutube.getYoutubeId());
							Log.d(TAG, " Title: " + filmYoutube.getName());
							Log.d(TAG, "\n---------------------------------\n");
						}
					}
				}
			}

		} catch (GoogleJsonResponseException e) {
			if (BuildConfig.DEBUG) {
				System.err.println("There was a service error: "
						+ e.getDetails().getCode() + " : "
						+ e.getDetails().getMessage());
			}
		} catch (IOException e) {
			if (BuildConfig.DEBUG) {
				System.err.println("There was an IO error: " + e.getCause()
						+ " : " + e.getMessage());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return list;
	}

	/**
	 * Search for next page after {@link #searchByKey(String)}. To use this
	 * function, you must call {@link #searchByKey(String)} first.
	 * 
	 * @return {@link List} of {@link FilmYoutube} or empty list.
	 */
	public List<FilmYoutube> searchNextPage() {
		if (BuildConfig.DEBUG) {
			Log.i(TAG, "Next page");
		}
		List<FilmYoutube> list = new ArrayList<FilmYoutube>();
		mSearch.setPageToken(nextPageToken);
		try {
			SearchListResponse searchResponse = mSearch.execute();
			this.nextPageToken = searchResponse.getNextPageToken();
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Token: " + nextPageToken);
				Log.i(TAG, "results/ page: "
						+ searchResponse.getPageInfo().getResultsPerPage());
			}
			List<SearchResult> searchResultList = searchResponse.getItems();
			if (searchResultList != null) {
				for (SearchResult singleVideo : searchResultList) {
					ResourceId rId = singleVideo.getId();
					// Double checks the kind is video.
					if (rId.getKind().equals("youtube#video")) {
						FilmYoutube filmYoutube = new FilmYoutube();
						filmYoutube.setYoutubeId(rId.getVideoId());
						filmYoutube
								.setName(singleVideo.getSnippet().getTitle());
						list.add(filmYoutube);
						if (BuildConfig.DEBUG) {
							Log.d(TAG, " Video Id" + filmYoutube.getYoutubeId());
							Log.d(TAG, " Title: " + filmYoutube.getName());
							Log.d(TAG, "\n--------------------------------\n");
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * Get video information from {@link FilmYoutube}
	 * 
	 * @param videoId
	 */
	public static void getVideoInformation(FilmYoutube filmYoutube) {
		try {

			// Get query term from user.
			Videos.List videosSearch = youtube.videos().list(
					"id,snippet,statistics");
			/*
			 * It is important to set your developer key from the Google
			 * Developer Console for non-authenticated requests (found under the
			 * API Access tab at this link: code.google.com/apis/). This is good
			 * practice and increased your quota.
			 */
			videosSearch.setKey(MovieConstants.APP_BROWSER_ID);
			videosSearch.setId(filmYoutube.getYoutubeId());
			/*
			 * This method reduces the info returned to only the fields we need
			 * and makes calls more efficient.
			 */
			videosSearch
					.setFields("items(snippet(publishedAt,title,description),statistics)");
			videosSearch.setMaxResults(1l);
			VideoListResponse searchResponse = videosSearch.execute();

			List<com.google.api.services.youtube.model.Video> searchResultList = searchResponse
					.getItems();

			if (searchResultList != null && searchResultList.size() > 0) {
				com.google.api.services.youtube.model.Video video = searchResultList
						.get(0);
				filmYoutube.setViewCount(video.getStatistics().getViewCount());
				filmYoutube.setLikeCount(video.getStatistics().getLikeCount());
				filmYoutube.setDislikeCount(video.getStatistics()
						.getDislikeCount());
				filmYoutube.setInformation(video.getSnippet().getDescription());
				filmYoutube.setName(video.getSnippet().getTitle());
			}
			if (BuildConfig.DEBUG) {
				Log.i(TAG, filmYoutube.toString());
			}
		} catch (GoogleJsonResponseException e) {
			if (BuildConfig.DEBUG) {
				System.err.println("There was a service error: "
						+ e.getDetails().getCode() + " : "
						+ e.getDetails().getMessage());
			}
		} catch (IOException e) {
			if (BuildConfig.DEBUG) {
				System.err.println("There was an IO error: " + e.getCause()
						+ " : " + e.getMessage());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Get comments of video following by published date.
	 * 
	 * @param videoId
	 *            : video id of Youtube video.
	 * @return list or empty {@link List}
	 */
	public static List<MovieComment> getComments(String videoId) {
		List<MovieComment> list = new ArrayList<MovieComment>();
		try {
			String response = ServiceHelper.getInfo(
					String.format(YOUTUBE_GET_RECENT_COMMENTS, videoId), null);
			// if (BuildConfig.DEBUG) {
			// Log.d(TAG, "newest comments: " + response);
			// }
			if (!TextUtils.isEmpty(response)) {
				JSONObject jsonObject = new JSONObject(response);
				JSONObject feed = jsonObject.getJSONObject("feed");
				JSONArray entries = feed.getJSONArray("entry");
				int length = entries.length();

				for (int i = 0; i < length; i++) {
					JSONObject commentObject = entries.getJSONObject(i);
					JSONArray authors = commentObject.getJSONArray("author");
					JSONObject authorObject = authors.getJSONObject(0);
					JSONObject nameObject = authorObject.getJSONObject("name");
					JSONObject idObject = authorObject
							.getJSONObject("yt$userId");
					JSONObject titleObject = commentObject
							.getJSONObject("title");
					JSONObject publishedObject = commentObject
							.getJSONObject("published");
					JSONObject contentObject = commentObject
							.getJSONObject("content");

					MovieComment youtubeComment = new MovieComment();
					youtubeComment.setComment(contentObject.getString("$t"));
					youtubeComment.setPublishedDate(publishedObject
							.getString("$t"));
					youtubeComment.setTitle(titleObject.getString("$t"));
					youtubeComment.setUserName(nameObject.getString("$t"));
					youtubeComment
							.setAvatarUrl(String.format(
									YOUTUBE_AVATAR_IMAGE_URL,
									idObject.getString("$t")));

					list.add(youtubeComment);
				}
				if (BuildConfig.DEBUG) {
					Log.i(TAG, "Comment's number: " + list.size());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * This is working as of 14, Nov, 2013. The following is Android code to
	 * extract the available streaming URIs and return the all
	 * {@link MovieVideo} available in 3 quality: high, medium, low with 2 type:
	 * .mp4, .3gp.
	 * 
	 * mp4 high quaity -> 3gp high quality -> mp4 medium quality -> 3gp medium
	 * quality -> mp4 low quality -> 3gp low quality <br>
	 * 
	 * <br>
	 * 
	 * @param videoId
	 *            : example <code>NNBHzBxPsAs</code>
	 * @return {@link List} of {@link MovieVideo} or empty list.
	 */
	public static List<MovieVideo> getVideoUrlFromYoutubeVideoId(String videoId) {
		String ytUrl = YOUTUBE_PRE_URL_LINK + videoId;
		if (BuildConfig.DEBUG) {
			Log.i(TAG, "Url: " + ytUrl);
		}
		List<MovieVideo> list = new ArrayList<MovieVideo>();
		try {
			ArrayList<Video> videos = getStreamingUrisFromYouTubePage(ytUrl);
			if (videos != null && !videos.isEmpty()) {
				String urlHQ = "";
				String urlMedium = "";
				String urlLow = "";
				for (Video video : videos) {
					// If not find any streaming HQ video before
					if (TextUtils.isEmpty(urlHQ)
							&& video.ext.toLowerCase().contains("mp4")
							&& video.type.toLowerCase().contains("high")) {
						urlHQ = video.url;
						list.add(new MovieVideo(VideoQuality.High, video.url));
					} else if (TextUtils.isEmpty(urlHQ)
							&& video.ext.toLowerCase().contains("3gp")
							&& video.type.toLowerCase().contains("high")) {
						urlHQ = video.url;
						list.add(new MovieVideo(VideoQuality.High, video.url));
					} else if (TextUtils.isEmpty(urlMedium)
							&& video.ext.toLowerCase().contains("mp4")
							&& video.type.toLowerCase().contains("medium")) {
						urlMedium = video.url;
						list.add(new MovieVideo(VideoQuality.Medium, video.url));
					} else if (TextUtils.isEmpty(urlMedium)
							&& video.ext.toLowerCase().contains("3gp")
							&& video.type.toLowerCase().contains("medium")) {
						list.add(new MovieVideo(VideoQuality.Medium, video.url));
						urlMedium = video.url;
					} else if (TextUtils.isEmpty(urlLow)
							&& video.ext.toLowerCase().contains("mp4")
							&& video.type.toLowerCase().contains("low")) {
						urlLow = video.url;
						list.add(new MovieVideo(VideoQuality.Low, video.url));
					} else if (TextUtils.isEmpty(urlLow)
							&& video.ext.toLowerCase().contains("3gp")
							&& video.type.toLowerCase().contains("low")) {
						urlLow = video.url;
						list.add(new MovieVideo(VideoQuality.Low, video.url));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (BuildConfig.DEBUG) {
			Log.d(TAG, list.toString());
		}
		return list;
	}

	/**
	 * Get all video availble on youtube video link.
	 * 
	 * @param ytUrl
	 * @return
	 * @throws IOException
	 */
	private static ArrayList<Video> getStreamingUrisFromYouTubePage(String ytUrl)
			throws IOException {
		if (ytUrl == null) {
			return null;
		}
		// Remove any query params in query string after the watch?v=<vid> in
		// e.g.
		// http://www.youtube.com/watch?v=0RUPACpf8Vs&feature=youtube_gdata_player
		int andIdx = ytUrl.indexOf('&');
		if (andIdx >= 0) {
			ytUrl = ytUrl.substring(0, andIdx);
		}

		// Get the HTML response
		String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:8.0.1)";
		HttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
				userAgent);
		HttpGet request = new HttpGet(ytUrl);
		// HttpGet request = new HttpGet(
		// "https://www.youtube.com/watch?v=mWT0UbtSGFE");
		HttpResponse response = client.execute(request);
		String html = "";
		InputStream in = response.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder str = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			str.append(line.replace("\\u0026", "&"));
		}
		in.close();
		html = str.toString();
		// if (BuildConfig.DEBUG) {
		// Log.i(TAG, "Html page: " + html);
		// }
		// Parse the HTML response and extract the streaming URIs
		if (html.contains("verify-age-thumb")) {
			if (BuildConfig.DEBUG) {
				Log.w(TAG,
						"YouTube is asking for age verification. We can't handle that sorry.");
			}
			return null;
		}

		if (html.contains("das_captcha")) {
			if (BuildConfig.DEBUG) {
				Log.w(TAG,
						"Captcha found, please try with different IP address.");
			}
			return null;
		}

		Pattern p = Pattern.compile("stream_map\": \"(.*?)?\"");
		// Pattern p = Pattern.compile("/stream_map=(.[^&]*?)\"/");
		Matcher m = p.matcher(html);
		List<String> matches = new ArrayList<String>();
		while (m.find()) {
			matches.add(m.group());
		}

		if (matches.size() != 1) {
			if (BuildConfig.DEBUG) {
				Log.w(TAG,
						"Found zero or too many stream maps: " + matches.size());
			}
			return null;
		}

		String urls[] = matches.get(0).split(",");
		HashMap<String, String> foundArray = new HashMap<String, String>();
		for (String ppUrl : urls) {
			String url = URLDecoder.decode(ppUrl, "UTF-8");

			Pattern p1 = Pattern.compile("itag=([0-9]+?)[&]");
			Matcher m1 = p1.matcher(url);
			String itag = null;
			if (m1.find()) {
				itag = m1.group(1);
			}

			Pattern p2 = Pattern.compile("sig=(.*?)[&]");
			Matcher m2 = p2.matcher(url);
			String sig = null;
			if (m2.find()) {
				sig = m2.group(1);
			}

			Pattern p3 = Pattern.compile("url=(.*?)[&]");
			Matcher m3 = p3.matcher(ppUrl);
			String um = null;
			if (m3.find()) {
				um = m3.group(1);
			}

			if (itag != null && sig != null && um != null) {
				foundArray.put(itag, URLDecoder.decode(um, "UTF-8") + "&"
						+ "signature=" + sig);
			}
		}

		if (foundArray.size() == 0) {
			if (BuildConfig.DEBUG) {
				Log.w(TAG,
						"Couldn't find any URLs and corresponding signatures");
			}
			return null;
		}

		HashMap<String, Meta> typeMap = new HashMap<String, Meta>();
		typeMap.put("13", new Meta("13", "3GP", "Low Quality - 176x144"));
		typeMap.put("17", new Meta("17", "3GP", "Medium Quality - 176x144"));
		typeMap.put("36", new Meta("36", "3GP", "High Quality - 320x240"));
		typeMap.put("5", new Meta("5", "FLV", "Low Quality - 400x226"));
		typeMap.put("6", new Meta("6", "FLV", "Medium Quality - 640x360"));
		typeMap.put("34", new Meta("34", "FLV", "Medium Quality - 640x360"));
		typeMap.put("35", new Meta("35", "FLV", "High Quality - 854x480"));
		typeMap.put("43", new Meta("43", "WEBM", "Low Quality - 640x360"));
		typeMap.put("44", new Meta("44", "WEBM", "Medium Quality - 854x480"));
		typeMap.put("45", new Meta("45", "WEBM", "High Quality - 1280x720"));
		typeMap.put("18", new Meta("18", "MP4", "Medium Quality - 480x360"));
		typeMap.put("22", new Meta("22", "MP4", "High Quality - 1280x720"));
		typeMap.put("37", new Meta("37", "MP4", "High Quality - 1920x1080"));
		typeMap.put("33", new Meta("38", "MP4", "High Quality - 4096x230"));

		ArrayList<Video> videos = new ArrayList<Video>();

		for (String format : typeMap.keySet()) {
			Meta meta = typeMap.get(format);

			if (foundArray.containsKey(format)) {
				Video newVideo = new Video(meta.ext, meta.type,
						foundArray.get(format));
				videos.add(newVideo);
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "YouTube Video streaming details: ext:"
							+ newVideo.ext + ", type:" + newVideo.type
							+ ", url:" + newVideo.url);
				}
			}
		}

		return videos;
	}

	/**
	 * Get total search results.
	 */
	public int getTotalSearchResults() {
		return totalSearchResults;
	}

	/**
	 * Meta data
	 */
	private static class Meta {
		public String num;
		public String type;
		public String ext;

		Meta(String num, String ext, String type) {
			this.num = num;
			this.ext = ext;
			this.type = type;
		}
	}

	/**
	 * temp video
	 */
	private static class Video {
		public String ext = "";
		public String type = "";
		public String url = "";

		Video(String ext, String type, String url) {
			this.ext = ext;
			this.type = type;
			this.url = url;
		}

	}

}
