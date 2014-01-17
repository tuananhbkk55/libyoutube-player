package com.movie.filmtube.utils.caching;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;
import org.apache.http.util.ByteArrayBuffer;
import android.R.integer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;

public class ImageHelper {

	private static HttpURLConnection getConnectionStream(String imageUrl) {
		try {
			URL url = new URL(imageUrl);
			if (imageUrl.startsWith("https")) {
				HttpsURLConnection connection = (HttpsURLConnection) url
						.openConnection();
				connection.connect();
				return connection;
			} else {
				HttpURLConnection connection = (HttpURLConnection) url
						.openConnection();
				connection.setDoInput(true);
				connection.connect();
				return connection;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static int getSample(Options options, int expectedWidth,
			int expectedHeight) {
		int widthRatio = Math.round((float) options.outWidth / expectedWidth);
		int heightRatio = Math
				.round((float) options.outHeight / expectedHeight);
		int sample = widthRatio > heightRatio ? heightRatio : widthRatio;
		if (sample > 0) {
			return sample;
		} else {
			return 1;
		}
	}
	public static boolean downloadImageFromUrl(final String imageUrl,
			final String filePath) {
		try {
			URL url = new URL(imageUrl);
			File file = new File(filePath);
			URLConnection ucon = url.openConnection();
			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			ByteArrayBuffer baf = new ByteArrayBuffer(5000);
			int current = 0;
			while ((current = bis.read()) != -1) {
				baf.append((byte) current);
			}
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(baf.toByteArray());
			fos.flush();
			fos.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	public static Bitmap rotateBitmap(Bitmap source, float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(),
				source.getHeight(), matrix, true);
	}
	public static Bitmap decodeToSampleBitmap(final Object object,
			final int width, final int height) {
		boolean isNotDownSample = width == 0 || height == 0 ? true : false;

		if (object instanceof String) {
			String path = (String) object;
			Options options = new Options();
			if (path.startsWith("http")) {
				Bitmap bitmap = null;
				HttpURLConnection connection = null;
				try {
					connection = getConnectionStream(path);
					InputStream inputStream = connection.getInputStream();
					if (isNotDownSample) {
						bitmap = BitmapFactory.decodeStream(inputStream);
						inputStream.close();
					} else {
						options.inJustDecodeBounds = true;
						BitmapFactory.decodeStream(inputStream, null, options);
						inputStream.close();
						connection.disconnect();

						connection = getConnectionStream(path);
						int sample = getSample(options, width, height);
						options.inJustDecodeBounds = false;
						options.inSampleSize = sample;
						inputStream = connection.getInputStream();
						bitmap = BitmapFactory.decodeStream(inputStream, null,
								options);
						inputStream.close();
						connection.disconnect();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (connection != null) {
						connection.disconnect();
					}
				}
				return bitmap;
			} else {
				if (isNotDownSample) {
					return BitmapFactory.decodeFile(path);
				}
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(path, options);
				int sampleRatio = getSample(options, width, height);
				options.inSampleSize = sampleRatio;
				options.inJustDecodeBounds = false;
				return BitmapFactory.decodeFile(path, options);
			}
		}
		return null;
	}

}
