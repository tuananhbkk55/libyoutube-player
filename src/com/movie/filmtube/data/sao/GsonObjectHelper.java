package com.movie.filmtube.data.sao;
import java.util.List;
import com.google.gson.annotations.SerializedName;
import com.movie.filmtube.data.CategoryAndFilm;
import com.movie.filmtube.data.FilmYoutube;

public class GsonObjectHelper {
	public static class GsonGetAllFilms {
		@SerializedName("film")
		public FilmYoutube filmYoutube;
		@SerializedName("ListFilmCategory")
		public List<CategoryAndFilm> categoryAndFilms;
	}
}
