package Protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import Util.MessageBroadcast;
/**
 * Created by meishan on 16/12/1.
 */
public class ServerResponseTask implements Runnable {

    private ReciveTask reciveTask;
    private SendTask sendTask;
    private Socket socket;
    private ResponseCallback tBack;
    
    private String userName;
    private String userUUID;
    
    //检查心跳变化：
    private int heartValue ;//当前心跳值
    private int prevheartValue ;//当前心跳值

    
    
    
    private String GEOlocation;
    private MessageBroadcast broadcastHolder;//发送消息的holder
    
	//private IdentityHashMap<String,Socket> SocketMap =new IdentityHashMap<String,Socket>(); 


    private volatile ConcurrentLinkedQueue<BasicProtocol> dataQueue = new ConcurrentLinkedQueue<>();
    private static ConcurrentHashMap<String, Socket> onLineClient = new ConcurrentHashMap<>();

    private String userIP;

    public String getUserIP() {
        return userIP;
    }

    public ServerResponseTask(Socket socket,  ResponseCallback tBack) 
    {
        this.socket = socket;
        this.tBack = tBack;
        //this.SocketMap = map;
        this.userIP = socket.getInetAddress().getHostAddress();
        //System.out.println("用户IP地址：" + userIP);
        //System.out.println("socket statues 1**" + socket.isClosed());
        broadcastHolder = MessageBroadcast.getInstance();
    }

    @Override
    public void run() {
        try {
            //开启接收线程
            reciveTask = new ReciveTask();
            reciveTask.inputStream = new DataInputStream(socket.getInputStream());
            
            reciveTask.start();
            ////System.out.println("socket statues 2**" + socket.isClosed());

            //开启发送线程
            sendTask = new SendTask();
            sendTask.outputStream = new DataOutputStream(socket.getOutputStream());
            sendTask.start();
            //System.out.println("socket statues 3**" + socket.isClosed());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sttop() 
    {
        //System.out.println("socket statues 8**" + socket.isClosed());

        broadcastHolder.removeQueue(GEOlocation);//从广播队列中删除队列
        if (reciveTask != null) {
            reciveTask.isCancle = true;
            if (reciveTask.inputStream != null) {
                SocketUtil.closeInputStream(reciveTask.inputStream);
                reciveTask.inputStream = null;
            }

            reciveTask = null;
        }

        if (sendTask != null) {
            sendTask.isCancle = true;
            if (sendTask.outputStream != null) {
                synchronized (sendTask.outputStream) {//防止写数据时停止，写完再停
                    sendTask.outputStream = null;
                }
            }
            
            
            sendTask.interrupt();
         
            sendTask = null;
        }
        
    }

    public void addMessage(BasicProtocol data) {
        if (!isConnected()) {
            return;
        }

        dataQueue.offer(data);
        toNotifyAll(dataQueue);//有新增待发送数据，则唤醒发送线程
    }

    public Socket getConnectdClient(String clientID) {
        return onLineClient.get(clientID);
    }

    /**
     * 打印已经链接的客户端
     */
    public static void printAllClient() {
        if (onLineClient == null) {
            return;
        }
        Iterator<String> inter = onLineClient.keySet().iterator();
        while (inter.hasNext()) {
            //System.out.println("client:" + inter.next());
        }
    }

    public void toWaitAll(Object o) {
        synchronized (o) {
            try {
                o.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void toNotifyAll(Object obj) {
        synchronized (obj) {
            obj.notifyAll();
        }
    }

    private boolean isConnected() 
    {
        if (socket.isClosed() || !socket.isConnected()) {
            onLineClient.remove(userIP);
            ServerResponseTask.this.sttop();
            //System.out.println("socket.sttop 3...");

            //System.out.println("socket.sttop 1...");

            //System.out.println("socket closed...");
            
            //System.out.println("socket statues 4**" + socket.isClosed());

            return false;
        }
        return true;
    }

    public class ReciveTask extends Thread {

        private DataInputStream inputStream;
        private boolean isCancle;

        @Override
        public void run()
        {
            while (!isCancle) 
            {
                if (!isConnected()) 
                {
                    isCancle = true;
                    //System.out.println("isConnected **1");
                    break;
                }

                BasicProtocol clientData = SocketUtil.readFromStream(inputStream);
                ////System.out.println("inputstream is close? ");

                if (clientData != null) 
                {
                    if (clientData.getProtocolType() == DataProtocol.PROTOCOL_TYPE)
                    {
                        System.out.println("dtype: " + ((DataProtocol) clientData).getDtype() + ", pattion: " + ((DataProtocol) clientData).getPattion() + ", msgId: " + ((DataProtocol) clientData).getMsgId() + ", data: " + ((DataProtocol) clientData).getData());

                        int pation =  ((DataProtocol) clientData).getPattion();
                        String data = ((DataProtocol) clientData).getData();
                        
                        //设置用户名等信息请求
                        if(pation == DataProtocol.getPattion_SocketFirstConnect())
                        {
                        	//SocketMap.put(data, socket);

                        	String[] a = data.split("\\ ");
                        	
                        	GEOlocation = new String(a[0]);
                
                        	broadcastHolder.addQueue(GEOlocation, dataQueue);
                        	
                        	userName = "";
                        	//这里是处理名字中有空格的情况，前提是geohash和uuid没有空格
                        	for( int i = 1; i<a.length - 1; i++)
                        		userName = userName+a[i];
                            userUUID = a[a.length - 1];
                            
                            //System.out.println(GEOlocation+"<<<<<"+GEOlocation.hashCode()+">>>>>>>>>>>-----------------getPattion_SocketFirstConnect");
                  
                        }
                        //接受广播请求
                        else if(pation == DataProtocol.getPattion_Broadcast())
                        {
                        	DataProtocol updateData = (DataProtocol)clientData;
                        	updateData.setPattion(DataProtocol.getPattion_PushMessage());//设置业务类型
                        	String updated = userName+" "+ userUUID+ " "+updateData.getData();//userName+" "+ userUUID+ " " + " " String_message + " "+ timestamp
                        	updateData.setData(updated);
                        	broadcastHolder.SendBroadcast(updateData, dataQueue, GEOlocation);
                        	//System.out.println(GEOlocation+updated);
                        }
                        
                        DataAckProtocol dataAck = new DataAckProtocol();
                        dataAck.setUnused("收到消息：" + ((DataProtocol) clientData).getData());
                        dataQueue.offer(dataAck);
                        toNotifyAll(dataQueue); //唤醒发送线程
                        tBack.targetIsOnline(userIP);
                        //心跳与心跳失活处理
                    } else if (clientData.getProtocolType() == 2) {
                        //System.out.println("pingId: " + ((PingProtocol) clientData).getPingId());

                        PingAckProtocol pingAck = new PingAckProtocol();
                        pingAck.setUnused("收到心跳");
                        pingAck.setAckPingId(((PingProtocol) clientData).getPingId()+1);
                        dataQueue.offer(pingAck);
                        toNotifyAll(dataQueue); //唤醒发送线程
                        tBack.targetIsOnline(userIP+" " + ((PingProtocol) clientData).getPingId());

                        heartValue = ((PingProtocol) clientData).getPingId();
                        //启动定时器 40秒心跳不更新则断开连接
                        if( heartValue == 1)
                        {	prevheartValue = 0;
                        	Timer timer = new Timer();
	                	     timer.schedule(new TimerTask() {
	                	       public void run() {
	                	    	   //心跳没更新
	                	    	   if(prevheartValue == heartValue)
	                	    	   {
	                	    		   //断开连接
	                	    		   sttop();
	                	               //System.out.println("socket.sttop 2..."+"  pre="+prevheartValue+ "  now="+heartValue);

	                	    		   //System.out.println("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
	                	    		   ////System.exit(-1);
	                	    		   this.cancel();
	                	    	   }
	                	    	   else
	                	    		   prevheartValue = heartValue;
	                	    	   
	                	    	   //System.out.println("Timer is running = "+this.hashCode());
	                	       }
	                	     }, 0, PingProtocol.HEART_FREQUENCY);     
                        	     
                        }
                        
               	     //System.out.println("在线人数；"+broadcastHolder.getUsersCount());
  
                    }
                } 
                else 
                {
                	////System.out.println("receive a null data");
                	
                    //System.out.println("client is offline...");
                    break;
                }
            }
           
            broadcastHolder.removeQueue(GEOlocation);//从广播队列中删除队列
            //System.out.println("ccccccccccccccccccccc RecvTask is stop holders = "+ broadcastHolder.getUsersCount());
            SocketUtil.closeInputStream(inputStream);
        }

		
    }

    public class SendTask extends Thread {

        private DataOutputStream outputStream;
        private boolean isCancle;

        @Override
        public void run() 
        {
            while (!isCancle) {
                if (!isConnected())
                {
                    isCancle = true;
                    break;
                }

                BasicProtocol procotol = dataQueue.poll();
                if (procotol == null) {
                    toWaitAll(dataQueue);
                } 
                else if (outputStream != null) 
                {
                    synchronized (outputStream) 
                    {
                        SocketUtil.write2Stream(procotol, outputStream);
                        
                    }
                }
            }
            SocketUtil.closeOutputStream(outputStream);
        }
    }
}