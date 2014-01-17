package com.movie.filmtube.utils;

public class Constants {
	public static final String DATABASE_NAME = "filmbigbang.db";
	public static final String CATEGORY_ALL_FILMS_NAME = "All Films";
	public static final long CATEGORY_ALL_FILMS_ID = -1;
	public enum MediaState {
		IDLE, PREPARED, STARTED, PAUSED, STOP, END, ERROR
	}
	public static final String FORDER_DOWNLOAD_FILMS = "DownloadedFilms";
	public static final String TEMP_FILE = "temp";
	public static final String KEY_FILE_SIZE = "videoFileSize";
	public static final String KEY_CHAPTER_UPDATE = "chapterUpdate";
	public static final String KEY_PERCENT_UPDATE = "percentUpdate";
	public static final String KEY_STATUS_UPDATE = "statusUpdate";
	public static final String KEY_FILM_PICKED = "pickedFilm";
	public static final String KEY_FILMS_RELATED = "relatedFilms";
	public static final String KEY_BOOKMARKED_PICKED = "pickedBookmark";
	public static final String KEY_YOUTUBE_ID = "youtubeId";
	public static final String KEY_BOOKMARK_DELETED = "bookmarkedIdDeleted";
	public static final String KEY_BOOKMARK_ADDED = "bookmarkedIdAdded";
	public static final String ACTION_MEDIA_READY_TO_PLAY = "action.media.ready";
	public static final String ACTION_MEDIA_COMPLETE = "action.media.completed";
	public static final String ACTION_MEDIA_ERROR = "action.media.error";
	public static final String ACTION_MEDIA_BUFFERING_UPDATED = "action.media.buffering.updated";
	public static final String ACTION_MEDIA_PROGRESS_UPDATED = "action.media.progress.updated";
	public static final String ACTION_MEDIA_WAITING_FOR_LOADING = "action.media.waiting.for.loading";
	public static final String ACTION_MEDIA_VIDEO_CHANGED = "action.media.video.changed";
	public static final String ACTION_MEDIA_RELOAD_CONTROLLER = "action.media.controller.ui.reloaded";
	public static final String ACTION_DOWNLOAD_VIDEO_CANCELED = "action.download.video.canceled";
	public static final String ACTION_DOWNLOAD_VIDEO = "action.download.video";
	public static final String ACTION_RELOAD_LIST = "action.reload.list";
	public static final String ACTION_RELOAD_NAVIGATION_PANE = "action.reload.left.navigation.pane";
	public static final String ACTION_RELOAD_BOOKMARKS = "action.reload.bookmark";
	public static final String ACTION_UPDATE_FILM_FINISHED = "action.update.films.finished";
	public static final String ACTION_UPDATE_SURFACE_VIEW = "action.update.surface.view";
	public static final String PREFERENCE_NAME = "FilmTubePreferene";
	public static final String DATE_PATTERN = "dd-MM-yyyy";
	public static final int MEDIA_ERROR_CANT_FIND_LINK = -10;
	public static final String KEY_MEDIA_ERROR = "mediaErrorExtras";

}
