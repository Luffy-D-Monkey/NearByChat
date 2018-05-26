package com.example.livesocket.SocketManager;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.livesocket.Protocol.BasicProtocol;
import com.example.livesocket.Protocol.Config;
import com.example.livesocket.Protocol.DataAckProtocol;
import com.example.livesocket.Protocol.DataProtocol;
import com.example.livesocket.Protocol.PingAckProtocol;
import com.example.livesocket.Protocol.PingProtocol;
import com.example.livesocket.Protocol.SocketUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.SocketFactory;

public class ClientRequestTask implements Runnable
{
    private static final int SUCCESS = 100;
    private static final int FAILED = -1;

    private boolean isLongConnection = true;
    private Handler mHandler;
    private SendTask mSendTask;
    private ReciveTask mReciveTask;
    private HeartBeatTask mHeartBeatTask;
    private Socket mSocket;

    private boolean isSocketAvailable;
    private boolean closeSendTask;
    private int HEARTDATA = 0xFF;

    protected volatile ConcurrentLinkedQueue<BasicProtocol> dataQueue = new ConcurrentLinkedQueue<>();

    public ClientRequestTask(RequestCallBack requestCallBacks) {
        mHandler = new MyHandler(requestCallBacks);
    }

    @Override
    public void run()
    {
        try {
            try {
                mSocket = SocketFactory.getDefault().createSocket(Config.ADDRESS, Config.PORT);
//                mSocket.setSoTimeout(10);
            } catch (ConnectException e) {
                failedMessage(-1, "服务器连接异常，请检查网络");
                return;
            }

            isSocketAvailable = true;

            //开启接收线程
            mReciveTask = new ReciveTask();
            mReciveTask.inputStream = mSocket.getInputStream();
            mReciveTask.start();

            //开启发送线程
            mSendTask = new SendTask();
            mSendTask.outputStream = mSocket.getOutputStream();
            mSendTask.start();

            //开启心跳线程
            if (isLongConnection)
            {
                mHeartBeatTask = new HeartBeatTask();
                mHeartBeatTask.outputStream = mSocket.getOutputStream();
                mHeartBeatTask.start();
            }
        } catch (IOException e) {
            failedMessage(-1, "网络发生异常，请稍后重试");
            e.printStackTrace();
        }
    }

    public void addRequest(DataProtocol data) {
        dataQueue.add(data);
        toNotifyAll(dataQueue);//有新增待发送数据，则唤醒发送线程
    }

    public synchronized void stop()
    {

        //关闭接收线程
        closeReciveTask();

        //关闭发送线程
        closeSendTask = true;
        toNotifyAll(dataQueue);

        //关闭心跳线程
        closeHeartBeatTask();

        //关闭socket
        closeSocket();

        //清除数据
        clearData();

        failedMessage(-1, "断开连接");
    }

    /**
     * 关闭接收线程
     */
    private void closeReciveTask() {
        if (mReciveTask != null) {
            mReciveTask.interrupt();
            mReciveTask.isCancle = true;
            if (mReciveTask.inputStream != null) {
                try {
                    if (isSocketAvailable && !mSocket.isClosed() && mSocket.isConnected()) {
                        mSocket.shutdownInput();//解决java.net.SocketException问题，需要先shutdownInput
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SocketUtil.closeInputStream(mReciveTask.inputStream);
                mReciveTask.inputStream = null;
            }
            mReciveTask = null;
        }
    }
    /**
     * 关闭发送线程
     */
    private void closeSendTask()
    {
        if (mSendTask != null) {
            mSendTask.isCancle = true;
            mSendTask.interrupt();
            if (mSendTask.outputStream != null) {
                synchronized (mSendTask.outputStream) {//防止写数据时停止，写完再停
                    SocketUtil.closeOutputStream(mSendTask.outputStream);
                    mSendTask.outputStream = null;
                }
            }
            mSendTask = null;
        }
    }

    /**
     * 关闭心跳线程
     */
    private void closeHeartBeatTask()
    {
        if (mHeartBeatTask != null) {
            mHeartBeatTask.isCancle = true;
            if (mHeartBeatTask.outputStream != null) {
                SocketUtil.closeOutputStream(mHeartBeatTask.outputStream);
                mHeartBeatTask.outputStream = null;
            }
            mHeartBeatTask = null;
        }
    }
    /**
     * 关闭socket
     */
    private void closeSocket() {
        if (mSocket != null) {
            try {
                mSocket.close();
                isSocketAvailable = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 清除数据
     */
    private void clearData() {
        dataQueue.clear();
        isLongConnection = false;
    }

    private void toWait(Object o)
    {
        synchronized (o) {
            try {
                o.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * notify()调用后，并不是马上就释放对象锁的，而是在相应的synchronized(){}语句块执行结束，自动释放锁后
     *
     * @param o
     */
    protected void toNotifyAll(Object o) {
        synchronized (o) {
            o.notifyAll();
        }
    }

    private void failedMessage(int code, String msg) {
        Message message = mHandler.obtainMessage(FAILED);
        message.what = FAILED;
        message.arg1 = code;
        message.obj = msg;
        mHandler.sendMessage(message);
    }

    private void successMessage(BasicProtocol protocol) {
        Message message = mHandler.obtainMessage(SUCCESS);
        message.what = SUCCESS;
        message.obj = protocol;
        mHandler.sendMessage(message);
    }

    //根据socket是否被关闭来进行stop
    private boolean isConnected()
    {
        //如果socket没有关闭则返回true
        if (mSocket.isClosed() || !mSocket.isConnected()) {
            ClientRequestTask.this.stop();
            return false;
        }
        return true;
    }

    /**
     * 服务器返回处理，主线程运行
     */
    public class MyHandler extends Handler
    {

        private RequestCallBack mRequestCallBack;

        public MyHandler(RequestCallBack callBack)
        {
            super(Looper.getMainLooper());
            this.mRequestCallBack = callBack;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case SUCCESS:
                    mRequestCallBack.onSuccess((BasicProtocol) msg.obj);
                    break;
                case FAILED:
                    mRequestCallBack.onFailed(msg.arg1, (String) msg.obj);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 数据接收线程
     */
    public class ReciveTask extends Thread
    {

        private boolean isCancle = false;
        private InputStream inputStream;

        @Override
        public void run()
        {
            //死循环子线程
            while (!isCancle)
            {
                //判断的原始依据是socket是否被关闭或者断开连接
                if (!isConnected())
                {
                    break;
                }

                if (inputStream != null)
                {
                    BasicProtocol reciverData = SocketUtil.readFromStream(inputStream);
                    if (reciverData != null)
                    {
                        if (reciverData.getProtocolType() == 1 )
                        {
                            successMessage(reciverData);
                        }
                        else if(reciverData.getProtocolType() == 3)
                        {
                            //ping信息可能在发布的版本不作处理
                            Log.d("pingResopnse ask id",""+((PingAckProtocol)reciverData).getAckPingId()+" unused:"+((PingAckProtocol)reciverData).getUnused());
                            successMessage(reciverData);
                        }
                        else if(reciverData.getProtocolType() == DataProtocol.PROTOCOL_TYPE)
                        {
                            Log.d("接受到附近的人消息",""+((DataProtocol)reciverData).getData());
                            successMessage(reciverData);
                        }
                    } else {
                        break;
                    }
                }
            }

            SocketUtil.closeInputStream(inputStream);//循环结束则退出输入流
        }
    }

    /**
     * 数据发送线程
     * 当没有发送数据时让线程等待
     */
    public class SendTask extends Thread {

        private boolean isCancle = false;
        private OutputStream outputStream;

        @Override
        public void run() {
            while (!isCancle)
            {
                if (!isConnected()) {
                    break;
                }

                BasicProtocol dataContent = dataQueue.poll();//取队列首元素返回，空队列则返回null
                if (dataContent == null)
                {
                    toWait(dataQueue);//没有发送数据则等待,调用对象wait
                    if (closeSendTask)
                    {
                        closeSendTask();//notify()调用后，并不是马上就释放对象锁的，所以在此处中断发送线程
                    }
                } else if (outputStream != null)
                {
                    synchronized (outputStream) {
                        SocketUtil.write2Stream(dataContent, outputStream);
                    }
                }
            }

            SocketUtil.closeOutputStream(outputStream);//循环结束则退出输出流
        }
    }

    /**
     * 心跳实现，频率0.5秒
     * Created by meishan on 16/12/1.
     */
    public class HeartBeatTask extends Thread
    {


        private static final int REPEATTIME = 1000;
        private boolean isCancle = false;//内部定义一个isCancle变量
        private OutputStream outputStream;
        private int pingId;

        @Override
        public void run()
        {
            pingId = 1;
            while (!isCancle)
            {
                if (!isConnected()) {
                    break;
                }

                try
                {
                    mSocket.sendUrgentData(HEARTDATA);
                } catch (IOException e)
                {
                    isSocketAvailable = false;
                    ClientRequestTask.this.stop();
                    break;
                }

                //如果outputstream存在则发送ping
                if (outputStream != null)
                {
                    PingProtocol pingProtocol = new PingProtocol();

                    pingProtocol.setPingId(pingId);
                    pingProtocol.setUnused("ping...");
                    SocketUtil.write2Stream(pingProtocol, outputStream);
                    pingId = pingId + 2;
                }

                try {
                    Thread.sleep(REPEATTIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            SocketUtil.closeOutputStream(outputStream);
        }
    }
}
