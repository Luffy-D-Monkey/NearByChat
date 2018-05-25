package com.esdraslopez.android.nearbychat.login

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import butterknife.BindView
import com.esdraslopez.android.nearbychat.GPS.GPSLocationManager
import com.esdraslopez.android.nearbychat.Location.GeoHash
import com.esdraslopez.android.nearbychat.MainActivity
import com.esdraslopez.android.nearbychat.R
import com.esdraslopez.android.nearbychat.Service.MyService
import com.esdraslopez.android.nearbychat.Service.ServiceBrocastType
import com.esdraslopez.android.nearbychat.Util

import com.example.livesocket.Protocol.DataProtocol

import kotlinx.android.synthetic.main.activity_login.*
import java.util.*

class LoginActivity : AppCompatActivity()
{

    val TAG: String = LoginActivity::class.java.simpleName
    var gpsLocationManager : GPSLocationManager? = null
    private val Location = 2
    var geoHash:GeoHash? = null

    var geoLocationtoString:String? = null


    var activityReceiver : ActivityReceiver? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Util.clearSharedPreferences(this)

       /* username_input.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
                true
            } else {
                false
            }
        }*/

        login_button.setOnClickListener { login() }
        about_button.setOnClickListener { Util.startActivity(this, AboutActivity::class.java) }
        feedback_button.setOnClickListener { FeedbackBottomDialogFragment.newInstance().show(supportFragmentManager, "add_photo_dialog_fragment") }

        gpsLocationManager = GPSLocationManager.getInstances(this);
        getAddress()



        activityReceiver = ActivityReceiver()


        // 创建IntentFilter
		val filter =  IntentFilter();
		// 指定BroadcastReceiver监听的Action
		filter.addAction(DataProtocol.SENDDATARESULT);
		// 注册BroadcastReceiver
		registerReceiver(activityReceiver, filter);

		val intent = Intent(this, MyService().javaClass)
		// 启动后台Service
        startService(intent)
    }
/*
    //shouldShowRequestPermissionRationale主要用于给用户一个申请权限的解释，该方法只有在用户在上一次已经拒绝过你的这个权限申请。也就是说，用户已经拒绝一次了，你又弹个授权框，你需要给用户一个解释，为什么要授权，则使用该方法。
    private fun requestPermission(permission: String, requestCode: Int) {

        if (!isGranted(permission)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {

            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            }
        } else {
            //直接执行相应操作了
            getAddress()
        }

    }

    //是否需要申请权限
    fun isGranted(permission: String): Boolean {
        return !isMarshmallow() || isGranted_(permission)//版本低于23或者已经获得权限了
    }

    private fun isGranted_(permission: String): Boolean {
        val checkSelfPermission = ActivityCompat.checkSelfPermission(this, permission)
        return checkSelfPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun isMarshmallow(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == Location) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getAddress()
            } else {
                // Permission Denied
                Toast.makeText(this@LoginActivity, "您没有授权该权限，请在设置中打开授权", Toast.LENGTH_SHORT).show()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
*/

    fun login()
    {
        login_button.clearFocus()
        login_button.isEnabled = false
        Util.hideKeyboard(this)

        //判断网络连接
        if (Util.isConnected(this@LoginActivity))
        {

            var username = username_input.text.toString()
            if (username.isEmpty()) username = "Anonymous"

            val userUUID = UUID.randomUUID().toString()
            if(geoHash != null)
            {

                Util.getSharedPreferences(this).edit {
                    putString(KEY_USER_UUID, userUUID)
                    putString(KEY_USERNAME, username)
                    putString(KEY_GEOHASH,geoHash.toString())
                }

                Log.i(TAG, "Logging in user.")
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra(KEY_USERNAME, username)
                        .putExtra(KEY_USER_UUID, userUUID)
                        .putExtra(KEY_GEOHASH,geoHash.toString())
                startActivity(intent)
            }
            else
            {
                Snackbar.make(container, "位置获取失败", Snackbar.LENGTH_SHORT).show()
            }
        } else
            Snackbar.make(container, "No Internet Connection", Snackbar.LENGTH_SHORT).show()
    }

    private fun getAddress() {


        gpsLocationManager?.start(locationListener)

    }

    override fun onResume() {
        super.onResume()

        login_button.isEnabled = true;
    }

    companion object {
        const val KEY_USERNAME = "username"
        const val KEY_USER_UUID = "user-uuid"
        const val KEY_GEOHASH = "locationToHash"
    }

    internal var locationListener: LocationListener = object : LocationListener
    {
        override fun onLocationChanged(location: Location) {

//            geoLocationtoString = GeoHash.fromLocation(location).toString()
//            //通知Service更新当前位置
//            // 创建Intent
//            val intent = Intent(DataProtocol.SENDDATAREQUEST);
//            intent.putExtra(ServiceBrocastType.TYPE,ServiceBrocastType.GPSSTATUESCHANGE)
//            intent.putExtra(com.esdraslopez.android.nearbychat.GPS.GPSProviderStatus.GPS_CHANGED,geoLocationtoString);
//            sendBroadcast(intent);
            showLocations(location);

        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle)
        {

        }

        override fun onProviderEnabled(provider: String) {

        }

        override fun onProviderDisabled(provider: String) {

        }
    }

    private fun showLocations(location: Location) {
//        val currentposition = "纬度 is" + location.latitude
//        val currentposition2 = "经度 is" + location.longitude
//        val heightStr = "高度 is" + location.altitude + "米"
        Log.d("聊天的经纬度", location.toString())
        geoHash = GeoHash.fromLocation(location)//默认最高精确度
        location_textview.setText(geoHash.toString())
    }

    public fun testConnect(view : View)
    {

        val data : DataProtocol = DataProtocol()
        data.data = "吃屎吧Kiluta"
        data.dtype = 0
        data.msgId = 1
        data.pattion = 2

       // 创建Intent
		val intent = Intent(DataProtocol.SENDDATAREQUEST);
		intent.putExtra(DataProtocol.SENDDATAREQUEST,data);
        intent.putExtra(ServiceBrocastType.TYPE,ServiceBrocastType.DATAPROTOCOL)
		// 发送广播，将被Service组件中的BroadcastReceiver接收到
		sendBroadcast(intent);

//        if(geoLocationtoString != null) {
//            val intent2 = Intent(DataProtocol.SENDDATAREQUEST);
//            intent2.putExtra(ServiceBrocastType.TYPE, ServiceBrocastType.GPSSTATUESCHANGE)
//            intent2.putExtra(com.esdraslopez.android.nearbychat.GPS.GPSProviderStatus.GPS_CHANGED, geoLocationtoString);
//            // 发送广播，将被Service组件中的BroadcastReceiver接收到
//            sendBroadcast(intent);
//        }
    }



// 自定义的BroadcastReceiver，负责监听从Service传回来的广播
    class ActivityReceiver : BroadcastReceiver()
	{

        override fun onReceive( context: Context,   intent : Intent)
		{
			// 获取Intent中的update消息，update代表播放状态
			var update:Int  = intent.getIntExtra("update", -1);
			// 获取Intent中的current消息，current代表当前正在播放的歌曲
			var current:Int = intent.getIntExtra("current", -1);

		}
	}


}
