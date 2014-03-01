package org.codefirex.cfxweather;

import java.util.Calendar;

import org.codefirex.utils.WeatherAdapter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ResultReceiver;

public class WeatherService extends Service {
	private static final String TAG = "WeatherService";

	static final String LOCATION_ACQUIRED_ACTION = "location_acquired";
	static final String LOCATION_UNAVAILABLE_ACTION = "location_unavailable";
	static final String LOCATION_REFRESHING = "location_refreshing";
	// coordinates sent to HttpService
	static final String LOCATION_EXTRA = "location_extra";

	// handler codes
	static final int INTERVAL_CHANGED = 1001;
	static final int LOCATION_MODE_CHANGED = 1002;
	static final int SCALE_CHANGED = 1003;
	static final int REFRESH_NOW = 1004;
	static final int PAUSE_SERVICE = 1005;
	static final int RESUME_SERVICE = 1006;

	static final String ALARM_TICKED = "cfx_weather_alarm_ticked";

	private LocationManager mLocationManager;
	private IntentFilter mFilter;
	private PendingIntent mAlarmPending;
	private boolean mEnabled = false;
	private int mFailCount = 0;
	private boolean mIsBooting = true;

	private Binder mBinder = new WeatherBinder();

	private static final double METERS_PER_MILE = 1609.34;
	private static final float MINIMUM_DISTANCE_THRESHOLD = (float) (METERS_PER_MILE * 5);
	private static final long MINUTE_MULTIPLE = (1000 * 60);

	private LocationListener mNetworkLocationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			sendPosition(location);
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status,
				Bundle extras) {
		}
	};	

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(ALARM_TICKED)) {
				resetLocationListener();
			} else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
//				cancelAlarm();
			} else if (action.equals(Intent.ACTION_SCREEN_ON)) {
//				resetAlarm();
//				resetLocationListener();
			}
		}
	};

	public class WeatherBinder extends Binder {
		WeatherService getService() {
			return WeatherService.this;
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case INTERVAL_CHANGED:
				resetAlarm();
				resetLocationListener();
				break;
			case LOCATION_MODE_CHANGED:
				resetLocationListener();
				break;
			case SCALE_CHANGED:
				sendAdapterBroadcast(WeatherAdapter.STATE_SCALE);
				break;
			case REFRESH_NOW:
				resetLocationListener();
				break;
			case PAUSE_SERVICE:
				mEnabled = false;
				sendAdapterBroadcast(WeatherAdapter.STATE_OFF);
				removeLocationListener();
				unregisterReceiver(mReceiver);
				cancelAlarm();
				break;
			case RESUME_SERVICE:
				mEnabled = true;
				sendAdapterBroadcast(WeatherAdapter.STATE_ON);
				registerReceiver(mReceiver, mFilter);
				resetAlarm();
				resetLocationListener();
				break;
			}
		}
	};

	private ResultReceiver mHttpReciever = new ResultReceiver(new Handler()) {
		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			int code = resultData.getInt(HttpService.RESULT_CODE_TAG);
			if (HttpService.RESULT_SUCCEED == code) {
				sendAdapterBroadcast(WeatherAdapter.STATE_UPDATED);
			} else if (HttpService.RESULT_FAIL == code) {

			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}

	@Override
	public void onCreate() {
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		mEnabled = WeatherPrefs.getEnabled(this);
		sendAdapterBroadcast(mEnabled ? WeatherAdapter.STATE_ON : WeatherAdapter.STATE_OFF);

		// create pending intent to fire when alarm is triggered
		mAlarmPending = PendingIntent.getBroadcast(this, 0, new Intent(
				ALARM_TICKED), PendingIntent.FLAG_CANCEL_CURRENT);

		mFilter = new IntentFilter();
		mFilter.addAction(ALARM_TICKED);
		mFilter.addAction(Intent.ACTION_SCREEN_ON);
		mFilter.addAction(Intent.ACTION_SCREEN_OFF);

		if (mEnabled) {
			registerReceiver(mReceiver, mFilter);
			resetAlarm();
			resetLocationListener();
		}
	}

	private void resetLocationListener() {
		removeLocationListener();
		mLocationManager.requestLocationUpdates(
				WeatherPrefs.getLocationMode(this), 0, 0,
				mNetworkLocationListener);
	}

	private boolean isSafeToUpdate() {
		return hasNetwork() && !isLocDisabled();
	}

	private boolean hasNetwork() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		return networkInfo != null && networkInfo.isConnected();
	}

	private void removeLocationListener() {
		sendAdapterBroadcast(WeatherAdapter.STATE_REFRESHING);
		mLocationManager.removeUpdates(mNetworkLocationListener);
	}

	private void resetAlarm() {
		long interval = Long.valueOf(WeatherPrefs.getInterval(this))
				* MINUTE_MULTIPLE;
		long START_TIME = Calendar.getInstance().getTimeInMillis() + interval;
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(mAlarmPending);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, START_TIME, interval,
				mAlarmPending);
	}

	private void startFailAlarm() {
		// we'll do 3 attempts at 30 second intervals if we are failing
		// after 3, try again at next primary interval
		long interval = MINUTE_MULTIPLE / 2;
		long START_TIME = Calendar.getInstance().getTimeInMillis() + interval;
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(mAlarmPending);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, START_TIME, interval,
				mAlarmPending);		
	}

	private void cancelAlarm() {
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(mAlarmPending);
	}

	private boolean isLocDisabled() {
		return !mLocationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER)
				&& !mLocationManager
						.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	// used when service initialized to quickly get a location
	private void sendFasttPosition() {
		String bestProvider = mLocationManager.getBestProvider(new Criteria(),
				true);
		Location local = mLocationManager.getLastKnownLocation(bestProvider);
		sendPosition(local);
	}

	// send coordinates to HttpService
	private void sendPosition(Location location) {
		if (!isSafeToUpdate()) {
			if (mFailCount == 0) {
				startFailAlarm();
				mFailCount++;
				return;
			} else if (mFailCount > 3) {
				// just try again at normal intervals
				mFailCount = 0;
				resetAlarm();
				return;
			}
			mFailCount++;
			return;
		}
		if (mFailCount > 0) {
			mFailCount = 0;
			resetAlarm();
		}
		Intent bestPosition = new Intent(this, HttpService.class);
		bestPosition.setAction(LOCATION_ACQUIRED_ACTION);
		bestPosition.putExtra(LOCATION_EXTRA, location);
		bestPosition.putExtra(HttpService.RESULT_TAG, mHttpReciever);
		startService(bestPosition);
		removeLocationListener();
	}

	private void sendAdapterBroadcast(int state) {
		Intent intent = new Intent();
		intent.setAction(WeatherAdapter.WEATHER_ACTION);
		intent.putExtra(WeatherAdapter.WEATHER_SERVICE_STATE, state);
		if (state == WeatherAdapter.STATE_SCALE) {
			intent.putExtra(WeatherAdapter.SCALE_TYPE, WeatherPrefs.getDegreeType(this));
		}
		sendStickyBroadcast(intent);
	}

	void sendMessage(Message m) {
		mHandler.sendMessage(m);
	}

	Message getMessage() {
		return mHandler.obtainMessage();
	}

	@Override
	public void onDestroy() {
		removeLocationListener();
		unregisterReceiver(mReceiver);
		cancelAlarm();
	}
}
