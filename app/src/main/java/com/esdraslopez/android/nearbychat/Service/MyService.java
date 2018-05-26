package com.esdraslopez.android.nearbychat.Service;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.esdraslopez.android.nearbychat.GPS.GPSLocationManager;
import com.esdraslopez.android.nearbychat.GPS.GPSLocationManagerInService;
import com.esdraslopez.android.nearbychat.Location.GeoHash;
import com.esdraslopez.android.nearbychat.MainActivity;
import com.esdraslopez.android.nearbychat.MainActivity_ViewBinding;
import com.esdraslopez.android.nearbychat.login.LoginActivity;
import com.example.livesocket.Protocol.BasicProtocol;
import com.example.livesocket.Protocol.DataProtocol;
import com.example.livesocket.Protocol.PingAckProtocol;
import com.example.livesocket.SocketManager.ConnectionClient;
import com.example.livesocket.SocketManager.RequestCallBack;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class MyService extends Service
{
    GPSLocationManagerInService gpsLocationManagerInService;
    private int Location = 2;
    GeoHash geoHash= null;
    String geoLocationtoString = "";
    String [] locations = new String[3];
    int count = 0;
    private String username;
    private  String userUUID;
    private long loginTime;

    private Boolean isSetAdditionals = false;

    int debugcount = 0;

    private WeakReference<Activity> activityHolder ;
    //
    MyReceiver serviceReceiver;
    private Boolean  ISCONNEDT = false;
    private boolean quit;
    private  ConnectionClient client;
    // 定义onBinder方法所返回的对象
    private MyBinder binder = new MyBinder();
    // 通过继承Binder来实现IBinder类
    public class MyBinder extends Binder  // ①
    {
        public Boolean isSocketConnected()
        {
            // 获取Service的运行状态：count
            return ISCONNEDT;
        }

        //发送消息
        public Boolean sendData(DataProtocol dataProtocol)
        {
            Log.d("bugbugbug","  "+1);
            if(ISCONNEDT && username != null && userUUID != null && geoLocationtoString != null)
            {
                //String sourcedata = dataProtocol.getData();
                return client.addNewRequest(dataProtocol);

            }
            return false;
        }

        public void setUsername(String uname)
        {
            username = uname;
        }

        public void setActivituHolder(Activity holder)
        {
            Log.d("bugbugbug","  "+2);
            activityHolder =new WeakReference<>(holder) ;
        }

        public void setUserUUID(String uuid)
        {
            userUUID = uuid;

        }



    }


    public Boolean connectToServer()
    {
        int connecttimes = 0;

        while(++connecttimes<10)
        {
            if(geoLocationtoString != null && username != null && userUUID != null)
                break;
            try
            {
                Thread.sleep(5*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d("bugbugbug","  "+3);
        if(geoLocationtoString == null || username == null || userUUID == null)
            return false;
        //发送绑定位置的消息
        DataProtocol data   = new DataProtocol();
        data.setData(geoLocationtoString + " "+ username+ " " + userUUID);
        data.setDtype(DataProtocol.PROTOCOL_TYPE);
        data.setMsgId(1);
        data.setPattion(DataProtocol.getPattion_SocketFirstConnect());

        client.addNewRequest(data);
        isSetAdditionals = true;
        Log.d("bugbugbug","  "+4);
        return true;
    }
    // 必须实现的方法，绑定该Service时回调该方法
    @Override
    public IBinder onBind(Intent intent)
    {
        System.out.println("Service is Binded");
        Log.d("bugbugbug","  "+5);
        // 返回IBinder对象
        return binder;
    }

    public class MyReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(final Context context, Intent intent)
        {
            String type = intent.getStringExtra(ServiceBrocastType.TYPE);
            switch (type)
            {
                case ServiceBrocastType.DATAPROTOCOL:

                    DataProtocol control = (DataProtocol) intent.getSerializableExtra(ServiceBrocastType.DATAPROTOCOL);
                    control.setPattion(DataProtocol.getPattion_Broadcast());
                    if(!isSetAdditionals)
                    {
                        if (connectToServer())
                        {
                            client.addNewRequest(control);
                        }
                    }
                    else
                    {
                        client.addNewRequest(control);

                    }
                    Log.d("bugbugbug","  "+6);
                    break;


                case ServiceBrocastType.SETadditonal:
                    username = intent.getStringExtra(ServiceBrocastType.userName);
                    userUUID = intent.getStringExtra(ServiceBrocastType.userUuid);
                    break;

                case ServiceBrocastType.GPSSTATUESCHANGE:
//                    String locationtoString = intent.getStringExtra(com.esdraslopez.android.nearbychat.GPS.GPSProviderStatus.GPS_CHANGED);
//                    client.closeConnect();
//                    client = null;
//                    client = new ConnectionClient(new RequestCallBack()
//                    {
//
//                        @Override
//                        public void onSuccess(BasicProtocol msg) {
//                            ISCONNEDT = true;
//                            Log.d("RequestCallBack", "success");
//                        }
//
//                        @Override
//                        public void onFailed(int errorCode, String msg) {
//                            ISCONNEDT = false;
//                            Log.d("RequestCallBack", "failed");
//
//                        }
//
//                    });
//                    Log.d("MyService gps  changed ", locationtoString);
//                    break;

                default:

                    break;

            }

        }
    }


    // Service被创建时回调该方法
    @Override
    public void onCreate()
    {Log.d("bugbugbug","  "+7);
        super.onCreate();

        Log.d("吃屎吧",""+ MainActivity.userUUID);
        Log.d("吃屎吧",""+ MainActivity.userUUID);
        System.out.println("Service is Created");
        Log.d("service socket create", " in creating");



        client = new ConnectionClient(new RequestCallBack()
        {

            @Override
            public void onSuccess(BasicProtocol msg)
            {
                ISCONNEDT = true;
                if(msg.getProtocolType() == 3)
                {
//                    int pingid = ((PingAckProtocol)msg).getAckPingId();
//                    Intent sendIntent = new Intent(DataProtocol.SENDDATARESULT);
//                    sendIntent.putExtra("pingAckid", pingid);
                    // 发送广播，将被Activity组件中的BroadcastReceiver接收到
//                    sendBroadcast(sendIntent);
                    //Log.d("pingAckid","in MyService"+pingid);


                }
                else if(msg.getProtocolType() == DataProtocol.PROTOCOL_TYPE)
                {
                    Intent sendIntent = new Intent(ServiceBrocastType.PUSHBROADCAST);
                    sendIntent.putExtra(ServiceBrocastType.TYPE,ServiceBrocastType.DATAPROTOCOL);
                    sendIntent.putExtra(DataProtocol.PUSHDATAPLROTOCOL,(DataProtocol)msg);
                    // 发送广播，将被Activity组件中的BroadcastReceiver接收到


                    sendBroadcast(sendIntent);
                    Log.d("bugbugbuginService",((DataProtocol)msg).getData());
                }
                Log.d("RequestCallBack", "success");

            }

            @Override
            public void onFailed(int errorCode, String msg) {
                ISCONNEDT = false;
                Log.d("RequestCallBack", "failed");

            }

        });


        //注册广播接受者
        serviceReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ServiceBrocastType.ServiceActionReceiver);
        //filter.addAction(ServiceBrocastType.SETadditonal);
        registerReceiver(serviceReceiver,filter);

        gpsLocationManagerInService = GPSLocationManagerInService.getInstances(this);
        getAddress();
        Log.d("bugbugbug","  "+9);
    }


    private void getAddress()
    {
        gpsLocationManagerInService.start(locationListener);

    }

    LocationListener locationListener = new  LocationListener()
    {
        @Override
        public void onLocationChanged(android.location.Location location) {

            geoHash = GeoHash.fromLocation(location);//默认最高精确度
            locations[(count++)%3] =  geoHash.toString();
            if(locations != null)
                if(locations[0].equals(locations[1]) && locations[1].equals(locations[2]))
                {
                    gpsLocationManagerInService.stop();
                    if(locationListener != null)
                        locationListener = null;
                    geoLocationtoString = locations[0];


                }

            Log.d("bugbugbug","  "+10);
            Log.d("经度纬度", geoHash.toString()+count);

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    // Service被断开连接时回调该方法
    @Override
    public boolean onUnbind(Intent intent)
    {
        System.out.println("Service is Unbinded");
        Log.d("bugbugbug","  "+11);
        return true;
    }
    // Service被关闭之前回调该方法
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        this.quit = true;
        client.closeConnect();
        System.out.println("Service is Destroyed");
        Log.d("bugbugbug","  "+12);
    }




}
