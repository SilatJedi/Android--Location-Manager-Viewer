package com.michaelfotiadis.locationmanagerviewer.datastore;

import static android.provider.Settings.System.AIRPLANE_MODE_ON;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import com.michaelfotiadis.locationmanagerviewer.containers.MyConstants;
import com.michaelfotiadis.locationmanagerviewer.containers.MyLocationData;
import com.michaelfotiadis.locationmanagerviewer.containers.MyNetworkStatus;
import com.michaelfotiadis.locationmanagerviewer.utils.Logger;

/**
 * Singleton Class
 * 
 * @since 07/07/2014
 * @author Michael Fotiadis
 * 
 */
public class Singleton implements LocationListener, NmeaListener, GpsStatus.Listener {

	private static volatile Singleton _instance = null;

	// The minimum distance to change Updates in meters
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;

	// The minimum time between updates in milliseconds
	private static final long MIN_TIME_BW_UPDATES = 500;

	/**
	 * 
	 * @return Instance of the Singleton
	 */
	public static Singleton getInstance() {
		if (_instance == null) {
			synchronized (Singleton.class) {
				if (_instance == null) {
					_instance = new Singleton();
				}
			}
		}
		return _instance;
	}

	/**
	 * Gets the state of Airplane Mode.
	 * 
	 * @param context
	 * @return true if enabled.
	 */
	@SuppressWarnings("deprecation")
	public static boolean isAirplaneModeOn(Context context) {
		ContentResolver contentResolver = context.getContentResolver();
		return Settings.System.getInt(contentResolver, AIRPLANE_MODE_ON, 0) != 0;
	}

	/**
	 * 
	 * @param instance
	 *            Sets the Instance of the Singleton
	 */
	public static void setInstance(Singleton instance) {
		Singleton._instance = instance;
	}

	private final String TAG = "Singleton";

	// **** Application Fields
	private Context mContext;

	// **** Location Manager Fields
	private LocationManager mLocationManager;
	// **** Location Data Fields
	private MyLocationData mGPSLocationData;
	private MyLocationData mNetworkLocationData;

	private MyLocationData mPassiveLocationData;

	// **** Network Fields
	private MyNetworkStatus mNetworkStatus;

	/**
	 * Singleton Constructor
	 */
	public Singleton() {
		// Initialise the location data fields
		setGPSData(new MyLocationData());
		setNetworkData(new MyLocationData());
		setPassiveData(new MyLocationData());

		setNetworkStatus(new MyNetworkStatus());
	}

	/**
	 * 
	 * @return Stored context
	 */
//	public Context getContext() {
//		return mContext;
//	}

	public MyLocationData getGPSData() {
		return mGPSLocationData;
	}

	public MyLocationData getNetworkData() {
		return mNetworkLocationData;
	}

	public MyNetworkStatus getNetworkStatus() {
		return mNetworkStatus;
	}

	public MyLocationData getPassiveData() {
		return mPassiveLocationData;
	}

	/**
	 * Modifies GPSData object and broadcasts the change
	 */
	public void notifyGPSDataChanged() {
		if (mLocationManager != null) {
			// Set the GPS Location according to the GPS Provider
			mGPSLocationData.setLocation(mLocationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER));
			// Broadcast that data has changed
			Intent broadcastIntent = new Intent(MyConstants.Broadcasts.BROADCAST_GPS_CHANGED.getString());
			mContext.sendBroadcast(broadcastIntent);
		}
	}

	/**
	 * 
	 */
	public void notifyNetworkDataChanged() {
		if (mLocationManager != null) {
			// Set the Network Location according to the Network Provider
			mNetworkLocationData.setLocation(mLocationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
			// Broadcast that data has changed
			Intent broadcastIntent = new Intent(MyConstants.Broadcasts.BROADCAST_NETWORK_CHANGED.getString());
			mContext.sendBroadcast(broadcastIntent);
		}
	}

	/**
	 * Modifies Network Status object and broadcasts the change
	 */
	public void notifyNetworkStateChanged() {
		if (mLocationManager != null) {
			mNetworkStatus.setGPSEnabled(mLocationManager.
					isProviderEnabled(LocationManager.GPS_PROVIDER));

			if (isAirplaneModeOn(mContext)) {
				mNetworkStatus.setCellNetworkEnabled(false);
			} else {
				mNetworkStatus.setCellNetworkEnabled(mLocationManager.
						isProviderEnabled(LocationManager.NETWORK_PROVIDER));
			}
			// Broadcast that data has changed
			Intent broadcastIntent = new Intent(MyConstants.Broadcasts.BROADCAST_NETWORK_STATE_CHANGED.getString());
			Logger.i(TAG, "Broadcasting Network State Changed");
			mContext.sendBroadcast(broadcastIntent);
		}
	}

	public void notifyGPSStateChanged() {
		// Broadcast that data has changed
		Intent broadcastIntent = new Intent(MyConstants.Broadcasts.BROADCAST_GPS_STATE_CHANGED.getString());
		mContext.sendBroadcast(broadcastIntent);
	}
	
	/**
	 * Broadcasts that the NMEA buffer has changed
	 */
	public void notifyNMEAChanged() {
		// Broadcast that data has changed
		Intent broadcastIntent = new Intent(MyConstants.Broadcasts.BROADCAST_NMEA_CHANGED.getString());
		mContext.sendBroadcast(broadcastIntent);
	}

	public void notifyPassiveDataChanged() {
		if (mLocationManager != null) {
			// Set the Network Location according to the Network Provider
			mPassiveLocationData.setLocation(mLocationManager
					.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
			// Broadcast that data has changed
			Intent broadcastIntent = new Intent(MyConstants.Broadcasts.BROADCAST_PASSIVE_CHANGED.getString());
			mContext.sendBroadcast(broadcastIntent);
		}
	}

	@Override
	public void onGpsStatusChanged(int event) {
		switch (event) {
		case GpsStatus.GPS_EVENT_STARTED:
			mGPSLocationData.setGPSEvent("GPS Started");
			break;

		case GpsStatus.GPS_EVENT_STOPPED:
			mGPSLocationData.setGPSEvent("GPS Stopped");
			break;

		case GpsStatus.GPS_EVENT_FIRST_FIX:
			mGPSLocationData.setGPSEvent("GPS First Fix");
			break;

		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			mGPSLocationData.setGPSEvent("Signal Detected");
			break;
		default:
			mGPSLocationData.setGPSEvent("Inactive");
			break;
		}
		
		List<GpsSatellite> satellites = new ArrayList<GpsSatellite>();
	    int countSatellitesInFix = 0;
	    for (GpsSatellite sat : mLocationManager.getGpsStatus(null).getSatellites()) {
	        if(sat.usedInFix()) {
	            countSatellitesInFix++;           
	        }
        	satellites.add(sat);
	    }
		mGPSLocationData.setSatellites(satellites);
		mGPSLocationData.setSatellitesInFix(countSatellitesInFix);
		notifyGPSDataChanged();
		notifyGPSStateChanged();
	}

	@Override
	public void onLocationChanged(Location location) {
		notifyGPSDataChanged();
		notifyNetworkDataChanged();
		notifyPassiveDataChanged();
	}

	@Override
	public void onNmeaReceived(long timestamp, String nmea) {

		String[] splitNMEA = nmea.split("$");

		for (String data : splitNMEA) {
			if (data.length() > 2) {
				mGPSLocationData.appendToNmea(data);
			}
		}


		notifyNMEAChanged();
	}

	@Override
	public void onProviderDisabled(String provider) {
		notifyNetworkStateChanged();
	}

	@Override
	public void onProviderEnabled(String provider) {
		notifyNetworkStateChanged();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		notifyNetworkStateChanged();
	}

	/**
	 * Notifies the location manager to request updates using predefined parameters
	 */
	public void requestGPSLocationUpdates() {
		Logger.d(TAG, "Requesting GPS Updates with time between updates " + MIN_TIME_BW_UPDATES 
				+ " and min distance change for updates " + MIN_DISTANCE_CHANGE_FOR_UPDATES);
		if (!mLocationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
			Toast.makeText(mContext, "GPS Provider not supported on this Device", 
					   Toast.LENGTH_LONG).show();
			Logger.e(TAG, "No GPS Provider");
			return;
		}
		mLocationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				MIN_TIME_BW_UPDATES,
				MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
		Logger.d(TAG, "GPS Updates Enabled");
	}

	/**
	 * Notifies the location manager to request updates using predefined parameters
	 */
	public void requestNetworkLocationUpdates() {
		Logger.d(TAG, "Requesting WiFi Updates with time between updates " + MIN_TIME_BW_UPDATES 
				+ " and min distance change for updates " + MIN_DISTANCE_CHANGE_FOR_UPDATES);
		if (!mLocationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
			Toast.makeText(mContext, "Network Provider not supported on this Device", 
					   Toast.LENGTH_LONG).show();
			Logger.e(TAG, "No Network Provider");
			return;
		}
		mLocationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER,
				MIN_TIME_BW_UPDATES,
				MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
		Logger.d(TAG, "Network Updates Enabled");
	} 

	public void requestNetworkUpdate() {
		Logger.d(TAG, "Requesting Network Status Update");
		if (mLocationManager != null) {
			notifyNetworkStateChanged();
		} else {
			mLocationManager = (LocationManager) mContext	.getSystemService(Context.LOCATION_SERVICE);
			notifyNetworkStateChanged();
		}
	}

	/**
	 * Notifies the location manager to request updates using predefined parameters
	 */
	public void requestPassiveLocationUpdates() {
		Logger.d(TAG, "Requesting Passive Updates with time between updates " + MIN_TIME_BW_UPDATES 
				+ " and min distance change for updates " + MIN_DISTANCE_CHANGE_FOR_UPDATES);
		if (!mLocationManager.getAllProviders().contains(LocationManager.PASSIVE_PROVIDER)) {
			Toast.makeText(mContext, "Passive Provider not supported on this Device", 
					   Toast.LENGTH_LONG).show();
			Logger.e(TAG, "No Passive Provider");
			return;
		}
		mLocationManager.requestLocationUpdates(
				LocationManager.PASSIVE_PROVIDER,
				MIN_TIME_BW_UPDATES,
				MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
		Logger.d(TAG, "Passive Updates Enabled");
	}

	public void setContext(Context mContext) {
		Logger.d(TAG, "Setting Singleton Context " + mContext.getApplicationContext().getPackageName());
		this.mContext = mContext.getApplicationContext();

	}

	public void setGPSData(MyLocationData gpsData) {
		this.mGPSLocationData = gpsData;
	}

	public void setNetworkData(MyLocationData mNetworkData) {
		this.mNetworkLocationData = mNetworkData;
	}

	public void setNetworkStatus(MyNetworkStatus networkStatus) {
		this.mNetworkStatus = networkStatus;
	}

	public void setPassiveData(MyLocationData mPassiveData) {
		this.mPassiveLocationData = mPassiveData;
	}

	public void startCollectingLocationData() {
		Logger.d(TAG, "Attempting to Start GPS");
		// Initialise the location manager for GPS collection
		mLocationManager = (LocationManager) mContext	.getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.addGpsStatusListener(this);
		mLocationManager.addNmeaListener(this);

		requestGPSLocationUpdates();
		requestNetworkLocationUpdates();
		requestPassiveLocationUpdates();
		requestNetworkUpdate();
	}

	public void stopCollectingLocationData() {
		Logger.d(TAG, "Attempting to Stop GPS");
		if(mLocationManager != null) {
			mLocationManager.removeUpdates(this);
			mLocationManager.removeGpsStatusListener(this);
			mLocationManager.removeNmeaListener(this);
		}
	}



}
