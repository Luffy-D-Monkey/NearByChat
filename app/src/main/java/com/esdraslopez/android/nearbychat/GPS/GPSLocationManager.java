package com.esdraslopez.android.nearbychat.GPS;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import java.lang.ref.WeakReference;
import java.util.List;

public class GPSLocationManager
{
    private static GPSLocationManager gpsLocationManager;
    private WeakReference<Activity> mContext;
    LocationManager locationManager;
    private static Object objLock = new Object();
    List<String> providerList ;
    private String provider;
    private static final int Location = 2;

    GPSLocationManager(Activity context)
    {
        this.mContext = new WeakReference<>(context);
        if (mContext.get() != null)
        {
            locationManager = (LocationManager) (mContext.get().getSystemService(Context.LOCATION_SERVICE));

            providerList = locationManager.getProviders(true);
        }
    }

    public Boolean start(LocationListener listener) {
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
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, Location);


            if (ActivityCompat.checkSelfPermission(mContext.get(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return false;
            }
        /*
        Location location = locationManager.getLastKnownLocation(provider);

        if (location != null) {
            //显示位置
            showLocations(location);

        }
        */
            locationManager.requestLocationUpdates(provider, 500, 0, listener);
        }
        return true;

    }

    //shouldShowRequestPermissionRationale主要用于给用户一个申请权限的解释，该方法只有在用户在上一次已经拒绝过你的这个权限申请。也就是说，用户已经拒绝一次了，你又弹个授权框，你需要给用户一个解释，为什么要授权，则使用该方法。
    private void requestPermission(String permission, int requestCode)
    {

        if (!isGranted(permission)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(mContext.get(), permission)) {

            } else {
                ActivityCompat.requestPermissions(mContext.get(), new String[]{permission}, requestCode);
            }
        } else {
            //直接执行相应操作了

        }

    }

    //是否需要申请权限
    public boolean isGranted(String permission) {
        return !isMarshmallow() || isGranted_(permission);//版本低于23或者已经获得权限了
    }

    private boolean isGranted_(String permission)
    {
        int checkSelfPermission = ActivityCompat.checkSelfPermission(mContext.get(), permission);
        return checkSelfPermission == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isMarshmallow()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }



    public static GPSLocationManager getInstances(Activity context) {
        if (gpsLocationManager == null) {
            synchronized (objLock) {
                if (gpsLocationManager == null) {
                    gpsLocationManager = new GPSLocationManager(context);
                }
            }
        }
        return gpsLocationManager;
    }




}
