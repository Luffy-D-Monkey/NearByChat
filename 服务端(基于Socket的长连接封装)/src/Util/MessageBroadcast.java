package Util;
import java.util.IdentityHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import Protocol.BasicProtocol;
import Protocol.DataProtocol;

public class MessageBroadcast 
{
	private static IdentityHashMap<String,ConcurrentLinkedQueue<BasicProtocol>> dataqueueMap =new IdentityHashMap<String,ConcurrentLinkedQueue<BasicProtocol>>();  

	//发送广播
	public void SendBroadcast(DataProtocol dataprotocol, ConcurrentLinkedQueue<BasicProtocol> dataQueue, String gEOlocation)
	{
		String location = gEOlocation;
		for (Entry<String, ConcurrentLinkedQueue<BasicProtocol>> entry : dataqueueMap.entrySet()) 
		  {  
			//接受广播地址比发送广播的要长（位置更精确）,这时给接收方地址前面字符段与location相同的接收方发送消息
		      if(entry.getKey().length() > location.length() || entry.getKey().length() == location.length())
		      {
		    	  String str1 = entry.getKey().substring(0,location.length());
		    	  if(str1.equals(location))
		    	  {
		    		  if(entry.getValue() == dataQueue)
		    			  continue;
		    		  entry.getValue().offer(dataprotocol);
		    		  synchronized (entry.getValue()) 
		    		  {
		    			  entry.getValue().notifyAll();
		    		  }
		  
		    	  }
		      }
		      //接收方的地址比发送方的要短，则匹配接收方地址长度的字符段与发送发的是否一致。
		      else 
		      {
		    	  String str1 = location.substring(0,entry.getKey().length());
		    	  if(str1.equals(location))
		    	  {
		    		  
		    		  entry.getValue().offer(dataprotocol);
		    		  synchronized (entry.getValue()) 
		    		  {
		    			  entry.getValue().notifyAll();
		    		  }
		    	  }
		      }
		      //System.out.println(entry.getKey() + "  " +entry.getValue());  
		  }  
	}
	
	//增加
	
	public static MessageBroadcast getInstance()
	{
		return SingletonHolder.instance;
	}
	
	private static class SingletonHolder
	{
		private static final MessageBroadcast instance = new MessageBroadcast();
	}
	
	
	public void removeQueue(String skey)
	{
		dataqueueMap.remove(skey);
	}
	
	public void addQueue(String locationKey,ConcurrentLinkedQueue<BasicProtocol> queue)
	{
		dataqueueMap.put(locationKey, queue);
		int i= 0;
		for (Entry<String, ConcurrentLinkedQueue<BasicProtocol>> entry : dataqueueMap.entrySet()) 
		  {  
		      //System.out.println(++i + "  "+ entry.getKey().toString() + "  " +entry.getValue().hashCode()+"dataqueueMapdataqueueMapdataqueueMapdataqueueMap");  
		  }  
	}
	
	public int getUsersCount()
	{
		return dataqueueMap.size();
	}

}
