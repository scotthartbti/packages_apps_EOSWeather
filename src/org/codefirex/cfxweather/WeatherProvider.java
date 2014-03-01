package org.codefirex.cfxweather;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

public class WeatherProvider extends ContentProvider {
	private static final String[] conditionIcon = new String[] { "tornado",
			"heavy_rain", "rain_tornado", "rain_thunder", "rain_thunder",
			"rain_snow", "ice", "ice_snow", "ice", "rain", "ice", "heavy_rain",
			"heavy_rain", "snow", "rain_snow", "heavysnow", "snow", "ice",
			"ice_snow", "sunny", "foggy", "sunny", "heat", "sunny",
			"partly_cloudy", "cold", "cloudy", "cloudy_night", "cloudy",
			"cloudy_night", "partly_cloudy", "clear_night", "sunny",
			"clear_night", "sunny", "clear_night", "sunny", "ice_snow", "heat",
			"rain_thunder", "rain_thunder", "night_rain_thunder", "rain",
			"rain_snow", "rain_snow", "rain_snow", "partly_cloudy",
			"rain_thunder_sun", "snow_thunder_sun", "rain_thunder_sun", "sunny" };
	public static final String WEATHER_AUTH = "org.codefirex.cfxweather.icons";
	public static final String DATA_AUTH = "org.codefirex.cfxweather.data";
	public static final Uri ICON_URI = Uri.parse("content://" + WEATHER_AUTH
			+ "/icons/#");
	public static final Uri DATA_URI = Uri.parse("content://" + DATA_AUTH);

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new RuntimeException("WeatherIconProvider.delete not supported");
	}

	@Override
	public String getType(Uri uri) {
		return WEATHER_AUTH;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new RuntimeException("WeatherIconProvider.insert not supported");
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Object[] row;
		if (uri.equals(DATA_URI)) {
			MatrixCursor result1 = new MatrixCursor(projection);
			Bundle b = WeatherPrefs.getBundleFromPrefs(getContext());
			row = new Object[projection.length];
			for (int i = 0; i < projection.length; i++) {
				if (projection[i]
						.compareToIgnoreCase(MediaStore.MediaColumns.DISPLAY_NAME) == 0) {
					row[i] = "WeatherInfo";
				} else if (projection[i]
						.compareToIgnoreCase(MediaStore.MediaColumns.SIZE) == 0) {
					row[i] = 0;
				} else if (projection[i]
						.compareToIgnoreCase(MediaStore.MediaColumns.DATA) == 0) {
					row[i] = b;
				} else if (projection[i]
						.compareToIgnoreCase(MediaStore.MediaColumns.MIME_TYPE) == 0) {
					row[i] = DATA_AUTH;
				}
			}
			result1.addRow(row);
			result1.setExtras(b);
			return result1;
		} else {
			MatrixCursor result = new MatrixCursor(projection);
			long fileSize = 0;
			int index = Integer.parseInt((uri.getFragment()));
			File tempFile = getIconFromAssets(index);
			fileSize = tempFile.length();
			String iconName = conditionIcon[index];
			row = new Object[projection.length];
			for (int i = 0; i < projection.length; i++) {

				if (projection[i]
						.compareToIgnoreCase(MediaStore.MediaColumns.DISPLAY_NAME) == 0) {
					row[i] = iconName;
				} else if (projection[i]
						.compareToIgnoreCase(MediaStore.MediaColumns.SIZE) == 0) {
					row[i] = fileSize;
				} else if (projection[i]
						.compareToIgnoreCase(MediaStore.MediaColumns.DATA) == 0) {
					row[i] = tempFile;
				} else if (projection[i]
						.compareToIgnoreCase(MediaStore.MediaColumns.MIME_TYPE) == 0) {
					row[i] = WEATHER_AUTH;
				}
			}
			result.addRow(row);
			return result;
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		throw new RuntimeException("WeatherIconProvider.update not supported");
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws FileNotFoundException {
		int index = Integer.parseInt((uri.getFragment()));
		File tempFile = getIconFromAssets(index);
		return ParcelFileDescriptor.open(tempFile,
				ParcelFileDescriptor.MODE_READ_ONLY);
	}

	private File getIconFromAssets(int index) {
		// to-do better initial state handling here
		if (index == -1) index = 50;
		String filename = conditionIcon[index] + ".png";
		File f = new File(getContext().getCacheDir() + "/" + filename);
		InputStream is;
		try {
			if (!f.exists()) {
				is = getContext().getAssets().open(
						conditionIcon[index] + ".png");
				int size = is.available();
				byte[] buffer = new byte[size];
				is.read(buffer);
				is.close();
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(buffer);
				fos.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return f;
	}
}
