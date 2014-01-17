package com.movie.filmtube.data;

import com.google.gson.annotations.SerializedName;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table CATEGORY_AND_FILM.
 */
public class CategoryAndFilm {

	private Long id;
	@SerializedName("IdCat")
	private Long categoryId;
	@SerializedName("IdFilm")
	private Long filmId;

	public CategoryAndFilm() {
	}

	public CategoryAndFilm(Long id) {
		this.id = id;
	}

	public CategoryAndFilm(Long id, Long categoryId, Long filmId) {
		this.id = id;
		this.categoryId = categoryId;
		this.filmId = filmId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public Long getFilmId() {
		return filmId;
	}

	public void setFilmId(Long filmId) {
		this.filmId = filmId;
	}

}
