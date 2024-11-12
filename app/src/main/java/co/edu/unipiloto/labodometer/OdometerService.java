package co.edu.unipiloto.labodometer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class OdometerService extends Service {
    private static final String TAG = "OdometerService";
    private final IBinder binder = new OdometerBinder();
    private LocationListener listener;
    private LocationManager locManager;
    private static double distanceInMeters;
    private static Location lastLocation = null;

    public static final String PERMISSION_STRING = android.Manifest.permission.ACCESS_FINE_LOCATION;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        double precision = intent.getDoubleExtra("precision", 1.0);
        int updateInterval = intent.getIntExtra("updateInterval", 1) * 1000;

        Log.d(TAG, "Service started with precision: " + precision + " and updateInterval: " + updateInterval);

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (lastLocation == null) {
                    lastLocation = location;
                }
                distanceInMeters += location.distanceTo(lastLocation);
                lastLocation = location;
                Log.d(TAG, "Location changed: " + location.toString());
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(TAG, "Provider enabled: " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(TAG, "Provider disabled: " + provider);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(TAG, "Status changed: " + provider + " status: " + status);
            }
        };

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING) == PackageManager.PERMISSION_GRANTED) {
            String provider = locManager.getBestProvider(new Criteria(), true);
            if (provider != null) {
                locManager.requestLocationUpdates(provider, updateInterval, (float) precision, listener);
                Log.d(TAG, "Location updates requested with provider: " + provider);
            } else {
                Log.e(TAG, "No suitable provider found");
            }
        } else {
            Log.e(TAG, "Location permission not granted");
        }

        return START_STICKY;
    }

    public class OdometerBinder extends Binder {
        OdometerService getOdometer() {
            return OdometerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound");
        return binder;
    }

    public double getDistance() {
        return this.distanceInMeters;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locManager != null && listener != null) {
            if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING) == PackageManager.PERMISSION_GRANTED) {
                locManager.removeUpdates(listener);
            }
            locManager = null;
            listener = null;
        }
        Log.d(TAG, "Service destroyed");
    }
}