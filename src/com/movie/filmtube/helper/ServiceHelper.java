package com.movie.filmtube.helper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;


public class ServiceHelper {
	private static final int TIME_OUT_DEFAULT = 10000;

	public static String delete(String url, String token, int timeout)
			throws IOException {
		HttpDelete httpDelete = new HttpDelete(url);
		return getResultFromService(httpDelete);
	}

	public static String getInfo(String url, List<NameValuePair> params)
			throws IOException {
		String target = url;
		if (params != null) {
			target += parseParamsToUrl(params);
		}
		HttpGet httpGet = new HttpGet(target);
		return getResultFromService(httpGet);
	}
	public static String parseParamsToUrl(List<NameValuePair> params) {
		String combinedParams = "";
		if (!params.isEmpty()) {
			combinedParams += "?";
			for (NameValuePair p : params) {
				String paramString = "";
				try {
					paramString = p.getName() + "="
							+ URLEncoder.encode(p.getValue(), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				if (combinedParams.length() > 1) {
					combinedParams += "&" + paramString;
				} else {
					combinedParams += paramString;
				}
			}
		}
		return combinedParams;
	}

	public static String postInfo(String url, List<NameValuePair> params,
			String token, HttpEntity httpEntity) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		if (params != null && !params.isEmpty()) {
			try {
				httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		if (httpEntity != null) {
			httpPost.setEntity(httpEntity);
		}
		return getResultFromService(httpPost);
	}

	public static String postInfo(String url, MultipartEntity mtentity) {
		String result = "";
		try {
			HttpResponse response = null;
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(url);
			httppost.setEntity(mtentity);
			response = httpclient.execute(httppost);
			HttpEntity htentity = response.getEntity();
			if (htentity != null) {
				InputStream instream = null;
				try {
					instream = htentity.getContent();
					result = convertStreamToString(instream);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (IllegalStateException e1) {
					e1.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	private static String getResultFromService(HttpRequestBase httpRequestBase)
			throws IOException {
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, TIME_OUT_DEFAULT);
		HttpConnectionParams.setSoTimeout(httpParams, TIME_OUT_DEFAULT);

		HttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpResponse response;
		String result = null;
		response = httpclient.execute(httpRequestBase);
		InputStream instream = response.getEntity().getContent();
		result = convertStreamToString(instream);
		instream.close();
		return result;
	}
	private static String convertStreamToString(InputStream instream) {

		String line = null;
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					instream, "UTF-8"));

			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				instream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

}
