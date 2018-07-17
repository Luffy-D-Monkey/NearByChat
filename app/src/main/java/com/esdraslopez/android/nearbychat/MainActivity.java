package com.esdraslopez.android.nearbychat;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.esdraslopez.android.nearbychat.Service.MyService;
import com.esdraslopez.android.nearbychat.Service.ServiceBrocastType;
import com.esdraslopez.android.nearbychat.login.LoginActivity;
import com.example.livesocket.Protocol.DataProtocol;
//import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {




    protected MainActivity thisactivityholder;
    private static final String TAG = MainActivity.class.getSimpleName();

    private String username;
    public static  final String userUUID = UUID.randomUUID().toString();
    //private String geohashstring;
    private long loginTime;

    //private MessageListener messageListener;


    //private Message activeMessage;

    private AlertDialog.Builder logoutDialog;

    private MessageListAdapter messageListAdapter;

    private  String geoHashToString ;
    @BindView(android.R.id.content) ViewGroup container;
    @BindView(R.id.message_input) EditText messageInput;
    @BindView(R.id.message_list_recycler) RecyclerView messageListRecycler;
    @BindView(R.id.empty_view) Group chatHistoryEmptyView;

    public MainActivity() {
    }

    @OnClick(R.id.send_message_button)
    public void sendMessage()
    {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty())
        {
            long timestamp = System.currentTimeMillis();

            DeviceMessage deviceMessage = new DeviceMessage(userUUID, username, message, timestamp);

            DataProtocol dataprotocol = new DataProtocol();
            dataprotocol.setPattion(DataProtocol.getPattion_Broadcast());
            dataprotocol.setMsgId(1);
            dataprotocol.setDtype(DataProtocol.PROTOCOL_TYPE);
            dataprotocol.setData(message+" "+timestamp);

            //发送广播，正常消息
            Intent intent = new Intent(ServiceBrocastType.ServiceActionReceiver);
            intent.putExtra(ServiceBrocastType.TYPE,ServiceBrocastType.DATAPROTOCOL);
            intent.putExtra(ServiceBrocastType.DATAPROTOCOL,dataprotocol);
            // 发送广播，将被Service组件中的BroadcastReceiver接收到
            sendBroadcast(intent);

            //activeMessage = deviceMessage.getMessage();
            //Log.d(TAG, "Publishing message = " + new String(activeMessage.getContent()));

//            //这里是给附近的用户推送消息
//            Nearby.getMessagesClient(this).publish(activeMessage);

            messageListAdapter.add(deviceMessage);
            messageInput.setText("");
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        username = getIntent().getStringExtra(LoginActivity.KEY_USERNAME);
        geoHashToString = getIntent().getStringExtra(LoginActivity.KEY_GEOHASH);

        //位置信息转到service中获取了
        //geohashstring = getIntent().getStringExtra(LoginActivity.KEY_GEOHASH);

        loginTime = System.currentTimeMillis();

        messageListAdapter = new MessageListAdapter(this, userUUID);
        messageListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver()
        {
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateEmptyView();
            }

            @Override
            public void onChanged() {
                updateEmptyView();
            }

            //平滑移动
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateEmptyView();

                messageListRecycler.post(new Runnable() {
                    @Override
                    public void run()
                    {
                        messageListRecycler.smoothScrollToPosition(messageListAdapter.getItemCount() - 1);
                    }
                });
            }

            //空消息展示页面
            private void updateEmptyView()
            {
                boolean showEmptyView = messageListAdapter.getItemCount() == 0;
                chatHistoryEmptyView.setVisibility(showEmptyView ? View.VISIBLE : View.GONE);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //设置从底部对item更新，即消息从底部插入
        layoutManager.setStackFromEnd(true);
        messageListRecycler.setLayoutManager(layoutManager);
        messageListRecycler.setAdapter(messageListAdapter);


        //下面通过bindService程序会直接崩溃，暂时没找到原因，现在尝试使用广播

//        final Intent  bindIntent = new Intent(this, MyService.class);
//        new Thread(new Runnable()
// {
//            @Override
//            public void run()
// {
//                bindService(bindIntent, connection, BIND_AUTO_CREATE);
//            }
//        }).start();

        /*
        messageListener = new MessageListener()
        {
            @Override
            public void onFound(Message message)
            {
                Log.d(TAG, "Found message: " + new String(message.getContent()));
                DeviceMessage deviceMessage = DeviceMessage.Companion.fromNearbyMessage(message);
                if (deviceMessage.getCreationTime() < loginTime)
                {
                    Log.d(TAG, "Found message was sent before we logged in. Won't add it to chat history.");
                }
                else
                    {
                    //收到消息，进行处理（添加到recycleview
                    messageListAdapter.add(deviceMessage);
                    }
            }

            @Override
            public void onLost(Message message) {
                Log.d(TAG, "Lost sight of message: " + new String(message.getContent()));
            }
        };
        */


        logoutDialog = new AlertDialog.Builder(this);
        logoutDialog
                .setTitle("Are you sure you want to leave?")
                .setMessage("Your chat history will be deleted.")
                .setNegativeButton("No", null);

        Log.d("DDMainActivity ","onCreate");

        thisactivityholder = this;

        IntentFilter filter =  new IntentFilter();

        // 指定BroadcastReceiver监听的Action
        filter.addAction(ServiceBrocastType.PUSHBROADCAST);
        // 注册BroadcastReceiver
        registerReceiver(activityReceiver, filter);

//        //发送广播，更新Service中的属性
//        Intent intent = new Intent(ServiceBrocastType.ServiceActionReceiver);
//        intent.putExtra(ServiceBrocastType.TYPE,ServiceBrocastType.SETadditonal);
//        intent.putExtra(ServiceBrocastType.userName,username);
//        intent.putExtra(ServiceBrocastType.userUuid,userUUID);
//        // 发送广播，将被Service组件中的BroadcastReceiver接收到
//        sendBroadcast(intent);

        if(!isServiceRunning(this.getApplicationContext(), "com.esdraslopez.android.nearbychat.Service.MyService" ))
        {
            Intent intent2 = new Intent(this,MyService.class);
            // 启动后台Service
            intent2.putExtra(ServiceBrocastType.userName,username);
            intent2.putExtra(ServiceBrocastType.geoHashtoString,geoHashToString);
            intent2.putExtra(ServiceBrocastType.userUuid,userUUID);

            startService(intent2);
        }
        else
        {
            //Service要断开连接，使用新参数连接。
            Intent intent2 = new Intent(ServiceBrocastType.ServiceActionReceiver);
            intent2.putExtra(ServiceBrocastType.TYPE,ServiceBrocastType.ReLogin);
            intent2.putExtra(ServiceBrocastType.userName,username);
            intent2.putExtra(ServiceBrocastType.geoHashtoString,geoHashToString);
            intent2.putExtra(ServiceBrocastType.userUuid,userUUID);
            sendBroadcast(intent2);
        }



        Log.d("bugbugbug","  "+8);

    }

    //判断Sevice是否在运行
    public static boolean isServiceRunning(Context context, String ServiceName) {
        if (("").equals(ServiceName) || ServiceName == null)
            return false;
        ActivityManager myManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString()
                    .equals(ServiceName)) {
                return true;
            }
        }
        return false;
    }




    ActivityReceiver activityReceiver = new ActivityReceiver();

    // 自定义的BroadcastReceiver，负责监听从Service传回来的广播
    private class ActivityReceiver extends BroadcastReceiver
    {
        @Override
         public void onReceive(Context context , Intent intent)
        {
            String type = intent.getStringExtra(ServiceBrocastType.TYPE);
            switch (type)
            {
                case ServiceBrocastType.DATAPROTOCOL:
                    DataProtocol control = (DataProtocol) intent.getSerializableExtra(DataProtocol.PUSHDATAPLROTOCOL);
                    if(control != null)
                    {
                        DeviceMessage deviceMessage = DeviceMessageUtil.DataProToDeviceMessage(control);
                        if (deviceMessage.getCreationTime() < loginTime)
                        {
                            Log.d(TAG, "Found message was sent before we logged in. Won't add it to chat history.");
                        }
                        else
                        {
                            //收到消息，进行处理（添加到recycleview
                            messageListAdapter.add(deviceMessage);
                        }
                    }
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
//    private ServiceConnection connection=new ServiceConnection() {
//        /**
//         * 服务解除绑定时候调用
//         */
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            // TODO Auto-generated method stub
//
//        }
//        /**
//         * 绑定服务的时候调用
//         */
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            // TODO Auto-generated method stub
//            //myService=((DownLoadBinder) service).
//            binder=(MyService.MyBinder) service;
//            /*
//             * 调用DownLoadBinder的方法实现参数的传递
//             */
//            binder.setUsername(username);
//            binder.setUserUUID(userUUID);
//            binder.setActivituHolder(thisactivityholder);
//            if( binder.connectToServer())
//                Log.d("MainAc","connect successed");
//            else
//                Log.d("MainAc","connect failed");
//        }
//    };



    @Override
    public void onStart()
    {
        super.onStart();
        Log.d("DDMainActivity ","onStart");
        //Nearby.getMessagesClient(this).subscribe(messageListener);
    }

    @Override
    public void onStop() {
        //if (activeMessage != null)
            //Nearby.getMessagesClient(this).unpublish(activeMessage);

        //Nearby.getMessagesClient(this).unsubscribe(messageListener);

        super.onStop();
        Log.d("DDMainActivity ","onStop");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                logoutDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Util.clearSharedPreferences(MainActivity.this);
                        stopService( new Intent(getApplicationContext(),MyService.class));
                        finish();
                    }
                }).show();
                return true;
            case R.id.action_clear_chat_history:
                messageListAdapter.clear();
                loginTime = System.currentTimeMillis();
                return true;

                //设置广播距离范围
            case R.id.item_range_100m:

                Intent intent = new Intent(ServiceBrocastType.ServiceActionReceiver);
                intent.putExtra(ServiceBrocastType.TYPE,ServiceBrocastType.ResetGeoHashAccuracy);
                intent.putExtra(ServiceBrocastType.GeoHashAccuracy,7);
                // 发送广播，将被Service组件中的BroadcastReceiver接收到
                sendBroadcast(intent);
                return true;
            case R.id.item_range_500m:
                Intent intent2 = new Intent(ServiceBrocastType.ServiceActionReceiver);
                intent2.putExtra(ServiceBrocastType.TYPE,ServiceBrocastType.ResetGeoHashAccuracy);
                intent2.putExtra(ServiceBrocastType.GeoHashAccuracy,6);
                // 发送广播，将被Service组件中的BroadcastReceiver接收到
                sendBroadcast(intent2);

                return true;
            case R.id.item_range_2500m:
                Intent intent3 = new Intent(ServiceBrocastType.ServiceActionReceiver);
                intent3.putExtra(ServiceBrocastType.TYPE,ServiceBrocastType.ResetGeoHashAccuracy);
                intent3.putExtra(ServiceBrocastType.GeoHashAccuracy,5);
                // 发送广播，将被Service组件中的BroadcastReceiver接收到
                sendBroadcast(intent3);
                return true;
            case R.id.item_range_20000m:
                Intent intent4 = new Intent(ServiceBrocastType.ServiceActionReceiver);
                intent4.putExtra(ServiceBrocastType.TYPE,ServiceBrocastType.ResetGeoHashAccuracy);
                intent4.putExtra(ServiceBrocastType.GeoHashAccuracy,4);
                // 发送广播，将被Service组件中的BroadcastReceiver接收到
                sendBroadcast(intent4);
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        logoutDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Util.clearSharedPreferences(MainActivity.this);
                MainActivity.super.onBackPressed();
            }
        }).show();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d("DDMainActivity ","onResume");

    }


    @Override
    public void onPause()
    {
        super.onPause();
        Log.d("DDMainActivity ","onPause");

    }
    @Override
    public void onRestart()
    {
        super.onRestart();
        Log.d("DDMainActivity ","onRestart");

    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d("DDMainActivity ","onDestroy");
        stopService( new Intent(this,MyService.class));

    }

    public static void PushBroadcast(DataProtocol dataProtocol)
    {

    }
}
