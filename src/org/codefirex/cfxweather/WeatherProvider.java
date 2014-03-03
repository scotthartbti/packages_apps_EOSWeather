package org.codefirex.cfxweather;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

public class WeatherProvider extends ContentProvider {
    private static final Map<Integer, ResInfo> weather_map = new HashMap<Integer, ResInfo>();

	static {
		weather_map.put(0, new ResInfo(R.string.tornado, "tornado"));
		weather_map.put(1, new ResInfo(R.string.tropical_storm, "heavy_rain"));
		weather_map.put(2, new ResInfo(R.string.hurricane, "rain_tornado"));
		weather_map.put(3, new ResInfo(R.string.severe_thunderstorms, "rain_thunder"));
		weather_map.put(4, new ResInfo(R.string.thunderstorms, "rain_thunder"));
		weather_map.put(5, new ResInfo(R.string.mixed_rain_snow, "rain_snow"));
		weather_map.put(6, new ResInfo(R.string.mixed_rain_sleet, "ice"));
		weather_map.put(7, new ResInfo(R.string.mixed_snow_sleet, "ice_snow"));
		weather_map.put(8, new ResInfo(R.string.freezing_drizzle, "ice"));		
		weather_map.put(9, new ResInfo(R.string.drizzle, "rain"));
		weather_map.put(10, new ResInfo(R.string.freezing_rain, "ice"));
		weather_map.put(11, new ResInfo(R.string.showers, "heavy_rain"));
		weather_map.put(12, new ResInfo(R.string.showers, "heavy_rain"));
		weather_map.put(13, new ResInfo(R.string.snow_flurries, "snow"));
		weather_map.put(14, new ResInfo(R.string.light_snow_showers, "rain_snow"));
		weather_map.put(15, new ResInfo(R.string.blowing_snow, "heavy_snow"));
		weather_map.put(16, new ResInfo(R.string.snow, "snow"));
		weather_map.put(17, new ResInfo(R.string.hail, "ice"));
		weather_map.put(18, new ResInfo(R.string.sleet, "ice_snow"));
		weather_map.put(19, new ResInfo(R.string.dust, "sunny"));
		weather_map.put(20, new ResInfo(R.string.foggy, "foggy"));
		weather_map.put(21, new ResInfo(R.string.haze, "heat"));
		weather_map.put(22, new ResInfo(R.string.smoky, "heat"));
		weather_map.put(23, new ResInfo(R.string.blustery, "sunny"));		
		weather_map.put(24, new ResInfo(R.string.windy, "partly_cloudy"));
		weather_map.put(25, new ResInfo(R.string.cold, "cold"));
		weather_map.put(26, new ResInfo(R.string.cloudy, "cloudy"));
		weather_map.put(27, new ResInfo(R.string.mostly_cloudy, "cloudy_night"));
		weather_map.put(28, new ResInfo(R.string.mostly_cloudy, "cloudy"));		
		weather_map.put(29, new ResInfo(R.string.partly_cloudy, "cloudy_night"));
		weather_map.put(30, new ResInfo(R.string.partly_cloudy, "partly_cloudy"));
		weather_map.put(31, new ResInfo(R.string.clear, "clear_night"));
		weather_map.put(32, new ResInfo(R.string.sunny, "sunny"));		
		weather_map.put(33, new ResInfo(R.string.fair, "clear_night"));
		weather_map.put(34, new ResInfo(R.string.fair, "sunny"));		
		weather_map.put(35, new ResInfo(R.string.mixed_rain_hail, "ice"));
		weather_map.put(36, new ResInfo(R.string.hot, "heat"));
		weather_map.put(37, new ResInfo(R.string.isolated_thunderstorms, "rain_thunder"));
		weather_map.put(38, new ResInfo(R.string.scattered_thunderstorms, "rain_thunder"));		
		weather_map.put(39, new ResInfo(R.string.scattered_thunderstorms, "rain_thunder"));
		weather_map.put(40, new ResInfo(R.string.scattered_showers, "rain_thunder"));
		weather_map.put(41, new ResInfo(R.string.heavy_snow, "night_rain_thunder"));
		weather_map.put(42, new ResInfo(R.string.scattered_snow_showers, "rain_snow"));		
		weather_map.put(43, new ResInfo(R.string.heavy_snow, "rain_snow"));
		weather_map.put(44, new ResInfo(R.string.partly_cloudy, "partly_cloudy"));
		weather_map.put(45, new ResInfo(R.string.thundershowers, "rain_thunder"));
		weather_map.put(46, new ResInfo(R.string.snow_showers, "rain_snow"));
		weather_map.put(47, new ResInfo(R.string.isolated_thundershowers, "rain_thunder"));
		weather_map.put(48, new ResInfo(R.string.not_available, "sunny"));
	}

	private static class ResInfo {
		int textRes;
		String iconName;

		public ResInfo(int textRes, String iconName) {
			this.textRes = textRes;
			this.iconName = iconName;
		}
	}

	public static final String PACKAGE_NAME = "org.codefirex.cfxweather";
	public static final String WEATHER_AUTH = PACKAGE_NAME + ".icons";
	public static final String DATA_AUTH = PACKAGE_NAME + ".data";
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
			String iconName = getIconName(index);
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

	private String getIconName(int conditionCode) {
		if (conditionCode == -1 || conditionCode == 3200) {
			conditionCode = 48;
		}
		ResInfo res = (ResInfo) weather_map.get(conditionCode);
		return res.iconName + ".png";
	}

	static String getConditionText(Context context, int conditionCode) {
		if (conditionCode == -1 || conditionCode == 3200) {
			conditionCode = 48;
		}
		ResInfo res = (ResInfo) weather_map.get(conditionCode);
		return context.getString(res.textRes);
	}

	private File getIconFromAssets(int index) {
		if (index == -1 || index == 3200) {
			index = 48;
		}
		String filename = getIconName(index);
		File f = new File(getContext().getCacheDir() + "/" + filename);
		InputStream is;
		try {
			if (!f.exists()) {
				is = getContext().getAssets().open(filename);
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
