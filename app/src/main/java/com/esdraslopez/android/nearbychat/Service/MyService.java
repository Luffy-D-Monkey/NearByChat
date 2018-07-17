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
import android.widget.Toast;
import com.example.geohash.GPS.GPSLocationManagerInService;
import com.example.geohash.Location.GeoHash;
import com.example.livesocket.Protocol.BasicProtocol;
import com.example.livesocket.Protocol.DataProtocol;
import com.example.livesocket.Protocol.PingAckProtocol;
import com.example.livesocket.Protocol.PingProtocol;
import com.example.livesocket.SocketManager.ConnectionClient;
import com.example.livesocket.SocketManager.RequestCallBack;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import butterknife.BindView;

import static java.lang.Thread.sleep;

public class MyService extends Service
{

    String geoLocationtoString = "";
    String geoLocationtoStringtem = "";


    //心跳信号
    //检查心跳变化：
    private int heartValue ;//当前心跳值

    private String username;
    private  String userUUID;

    private Boolean isSetAdditionals = false;

    private WeakReference<Activity> activityHolder ;
    //
    MyReceiver serviceReceiver;
    private Boolean  ISCONNEDT = false;
    private boolean quit = false;
    private  ConnectionClient client;
    // 定义onBinder方法所返回的对象
    private MyBinder binder = new MyBinder();
    // 通过继承Binder来实现IBinder类
    //binder的作用是给绑定的Activity返回binder，让Activity通过binder调用Service的方法
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


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //用户的参数是有MainActivity在启动的时候通过启动广播的时候通过Intent传送
        username = intent.getStringExtra(ServiceBrocastType.userName);
        userUUID = intent.getStringExtra(ServiceBrocastType.userUuid);
        geoLocationtoString = intent.getStringExtra(ServiceBrocastType.geoHashtoString);
        connectToServer(GeoHash.DEFAULT_ACCURACY);
        return START_NOT_STICKY;//app退出后Service不重启
    }
//accuracy 是广播距离
    public Boolean connectToServer(int accuracy)
    {
        client = new ConnectionClient(createMyRequestCallBack());

        Log.d("bugbugbug","  "+3);
        if(geoLocationtoString == null || username == null || userUUID == null)
            return false;

        //发送绑定位置的消息
        Log.d("geolength"," = "+geoLocationtoString.length() + "  "+accuracy+"geoLocationtoString is null = "+(geoLocationtoString == null)+geoLocationtoString);

        geoLocationtoStringtem = geoLocationtoString.substring(0,accuracy - 1);
        DataProtocol data   = new DataProtocol();
        data.setData(geoLocationtoStringtem + " "+ username+ " " + userUUID);
        data.setDtype(DataProtocol.PROTOCOL_TYPE);
        data.setMsgId(1);
        data.setPattion(DataProtocol.getPattion_SocketFirstConnect());
        client.addNewRequest(data);
        isSetAdditionals = true;
        Log.d("bugbugbug","  "+4);
        return true;
    }
    // 必须实现的方法，绑定该Service时回调该方法
    //IBinder 是Binder 的接口
    @Override
    public IBinder onBind(Intent intent)
    {
        System.out.println("Service is Binded");

        return binder;
    }

    //BroadcastReceiver是抽象类
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
                            client.addNewRequest(control);
                            Toast.makeText(context,"正在获取位置信息。。。",Toast.LENGTH_SHORT).show();

                    }
                    else
                    {
                        client.addNewRequest(control);
                        //Toast.makeText(context,"位置信息获取失败",Toast.LENGTH_SHORT).show();

                    }
                    Log.d("bugbugbug","  "+6);
                    break;


                case ServiceBrocastType.SETadditonal:
                    username = intent.getStringExtra(ServiceBrocastType.userName);
                    userUUID = intent.getStringExtra(ServiceBrocastType.userUuid);
                    break;

                    //位置改变
                case ServiceBrocastType.GPSSTATUESCHANGE:
                    //重新设置位置
                case ServiceBrocastType.ResetGeoHashAccuracy:

                    //断开然后再次连接服务器
                    ISCONNEDT = false;
                    isSetAdditionals = false;
                    client.closeConnect();
                    client = null;
                    int accuracy = intent.getIntExtra(ServiceBrocastType.GeoHashAccuracy,-1);
                    //重新获取位置，并且设置广播范围
                    connectToServer(accuracy);
                    break;

                case ServiceBrocastType.ReLogin:
                    //断开然后再次连接服务器
                    ISCONNEDT = false;
                    isSetAdditionals = false;
                    client.closeConnect();
                    client = null;
                    username = intent.getStringExtra(ServiceBrocastType.userName);
                    userUUID = intent.getStringExtra(ServiceBrocastType.userUuid);
                    geoLocationtoString = intent.getStringExtra(ServiceBrocastType.geoHashtoString);

                    //重新获取位置，并且设置广播范围
                    connectToServer(GeoHash.DEFAULT_ACCURACY);

                default:

                    break;

            }

        }
    }


//启动Socket的回调函数，也即对Server发送过来的消息进行处理
    private RequestCallBack createMyRequestCallBack()
    {
        return new RequestCallBack() {
            @Override
            public void onSuccess(BasicProtocol msg) {
                ISCONNEDT = true;
                //收到ping心跳回复
                if (msg.getProtocolType() == PingAckProtocol.PROTOCOL_TYPE)
                {
                    heartValue = ((PingAckProtocol) msg).getAckPingId();
                    Log.d("pingAckid", "in MyService" + heartValue);

                }
                //接收到正常消息数据
                else if (msg.getProtocolType() == DataProtocol.PROTOCOL_TYPE)
                {
                    Intent sendIntent = new Intent(ServiceBrocastType.PUSHBROADCAST);
                    sendIntent.putExtra(ServiceBrocastType.TYPE, ServiceBrocastType.DATAPROTOCOL);
                    sendIntent.putExtra(DataProtocol.PUSHDATAPLROTOCOL, (DataProtocol) msg);
                    // 发送广播，将被Activity组件中的BroadcastReceiver接收到

                    sendBroadcast(sendIntent);
                    Log.d("bugbugbuginService", ((DataProtocol) msg).getData());
                }
                Log.d("RequestCallBack", "success");
            }

            //发起Socket连接失败
            @Override
            public void onFailed(int errorCode, String msg) {
                ISCONNEDT = false;
                Log.d("RequestCallBack", "failed");

            }
        };
    }

    // Service被创建时回调该方法
    @Override
    public void onCreate()
    {    //Log.d("bugbugbug","  "+7);
        super.onCreate();
        System.out.println("Service is Created");
        Log.d("service socket create", " in creating");

        //注册广播接受者
        serviceReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ServiceBrocastType.ServiceActionReceiver);
        //filter.addAction(ServiceBrocastType.SETadditonal);
        //注册广播需要传入广播接收者和广播过滤器
        registerReceiver(serviceReceiver,filter);
        //gpsLocationManagerInService = GPSLocationManagerInService.getInstances(this);


        //getAddress();
        Log.d("bugbugbug","  "+9);
    }


//    private void getAddress()
//    {
//        gpsLocationManagerInService.start(locationListener);
//
//    }


//    LocationListener locationListener = new  LocationListener()
//    {
//
//        //这里为了节能，一旦获取到位置后就不再自动更新位置，始终使用第一个位置gpsLocationManagerInService.stop();
//        @Override
//        public void onLocationChanged(android.location.Location location) {
//
//            geoHash = GeoHash.fromLocation(location);//默认最高精确度
//            locations[(count++)%3] =  geoHash.toString();
//            if(locations != null)
//                if(locations[0].equals(locations[1]) && locations[1].equals(locations[2]))
//                {
//                    gpsLocationManagerInService.stop();
//                    if(locationListener != null)
//                        locationListener = null;
//                    geoLocationtoString = locations[0];
//                    Log.d("gotgeohahs", geoLocationtoString);
//                    //发送绑定消息name，id，location
//                }
//
//            Log.d("bugbugbug","  "+10);
//            Log.d("经度纬度", geoHash.toString()+count);
//
//        }
//
//        @Override
//        public void onStatusChanged(String provider, int status, Bundle extras) {
//
//        }
//
//        @Override
//        public void onProviderEnabled(String provider) {
//
//        }
//
//        @Override
//        public void onProviderDisabled(String provider) {
//
//        }
//    };

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
