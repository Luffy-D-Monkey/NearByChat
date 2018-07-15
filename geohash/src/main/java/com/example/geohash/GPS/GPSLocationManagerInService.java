package com.example.geohash.GPS;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.List;

public class GPSLocationManagerInService
{
    private static GPSLocationManagerInService gpsLocationManagerInService;
    private   WeakReference<Context> mContext;
    LocationManager locationManager;
    private static Object objLock = new Object();
    List<String> providerList ;
    private String provider;
    private static final int Location = 2;
    LocationListener locationListener;

    GPSLocationManagerInService(Context context)
    {
        this.mContext = new WeakReference<>(context) ;
        if (mContext.get() != null)
        {
            locationManager = (LocationManager) (mContext.get().getSystemService(Context.LOCATION_SERVICE));
            providerList = locationManager.getProviders(true);
        }
    }

    public Boolean start(LocationListener listener)
    {
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else {
//            Toast.makeText(MainActivity.this, "no Location provider to use",
//                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {

            if (ActivityCompat.checkSelfPermission(mContext.get(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Log.d("GPSManagerInService","failed");
                return false;
            }
        /*
        Location location = locationManager.getLastKnownLocation(provider);

        if (location != null) {
            //显示位置
            showLocations(location);

        }getBroadcast
        */
            locationListener = listener;
            locationManager.requestLocationUpdates(provider, 500, 0, listener);
        }
        return true;

    }



    public static GPSLocationManagerInService getInstances(Context context) {
        if (gpsLocationManagerInService == null) {
            synchronized (objLock) {
                if (gpsLocationManagerInService == null) {
                    gpsLocationManagerInService = new GPSLocationManagerInService(context);
                }
            }
        }
        return gpsLocationManagerInService;
    }


    public void stop()
    {

        locationManager.removeUpdates(locationListener);
        locationManager = null;
        if(locationListener != null)
            locationManager = null;
    }

}
