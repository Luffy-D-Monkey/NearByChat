package com.esdraslopez.android.nearbychat.Service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.esdraslopez.android.nearbychat.login.LoginActivity;
import com.example.livesocket.Protocol.BasicProtocol;
import com.example.livesocket.Protocol.DataProtocol;
import com.example.livesocket.SocketManager.ConnectionClient;
import com.example.livesocket.SocketManager.RequestCallBack;

public class MyService extends Service
{
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
            if(ISCONNEDT)
                return client.addNewRequest(dataProtocol);
            return false;
        }
    }
    // 必须实现的方法，绑定该Service时回调该方法
    @Override
    public IBinder onBind(Intent intent)
    {
        System.out.println("Service is Binded");
        // 返回IBinder对象
        return binder;
    }

    public class MyReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(final Context context, Intent intent)
        {
            DataProtocol control = (DataProtocol) intent.getSerializableExtra(DataProtocol.SENDDATAREQUEST);

            Boolean su = client.addNewRequest(control);

            Intent sendIntent = new Intent(DataProtocol.SENDDATARESULT);
            sendIntent.putExtra(DataProtocol.SENDDATARESULT, su);
            // 发送广播，将被Activity组件中的BroadcastReceiver接收到
            sendBroadcast(sendIntent);
        }
    }


    // Service被创建时回调该方法
    @Override
    public void onCreate()
    {
        super.onCreate();
        System.out.println("Service is Created");
        // 启动一条线程，动态地修改count状态值
        new Thread()
        {
            @Override
            public void run()
            {


                    client = new ConnectionClient(new RequestCallBack()
                    {

                        @Override
                        public void onSuccess(BasicProtocol msg)
                        {
                            ISCONNEDT = true;
                            Log.d("service socket create","success");
                        }

                        @Override
                        public void onFailed(int errorCode, String msg)
                        {
                            ISCONNEDT = false;
                            Log.d("service socket create","failed");

                        }
                    });

            }
        }.start();

        //注册广播接受者
        serviceReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DataProtocol.SENDDATAREQUEST);
        registerReceiver(serviceReceiver,filter);
    }
    // Service被断开连接时回调该方法
    @Override
    public boolean onUnbind(Intent intent)
    {
        System.out.println("Service is Unbinded");
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
    }
}
