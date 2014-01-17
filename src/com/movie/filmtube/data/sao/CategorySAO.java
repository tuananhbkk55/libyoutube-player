package com.movie.filmtube.data.sao;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.movie.filmtube.data.Category;
import com.movie.filmtube.helper.ServiceHelper;
import com.movie.filmtube.utils.ServiceConfig;
import com.youtube.bigbang.BuildConfig;

public class CategorySAO {
	public static final String TAG = "CategorySAO";
	public static List<Category> getAllCategories(Context context) {
		List<Category> list = new ArrayList<Category>();
		try {
			String respond = ServiceHelper.getInfo(ServiceConfig.URL_GET_ALL_CATEGORY, null);
		
			if (!TextUtils.isEmpty(respond)) {
				Gson gson = new Gson();
				Category[] array = gson.fromJson(respond, Category[].class);
				list = Arrays.asList(array);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		}
		return list;
	}

}
