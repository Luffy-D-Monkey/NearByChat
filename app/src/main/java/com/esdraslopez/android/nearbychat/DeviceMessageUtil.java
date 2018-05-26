package com.esdraslopez.android.nearbychat;

import com.example.livesocket.Protocol.DataProtocol;

public class DeviceMessageUtil
{
    public static DeviceMessage DataProToDeviceMessage(DataProtocol protocol)
    {
        if(protocol.getPattion() == DataProtocol.getPattion_PushMessage())
        {
            //Pattion_PushMessage的消息格式是：userName+" "+ userUUID+ " " + " " String_message + " "+ timestamp
            String []splitmess = protocol.getData().split("\\ ");

            String mess = "";
            for(int i = 2; i<splitmess.length -1 ; i++)
                mess += splitmess[i];
//val userUUID: String, val username: String, val messageBody: String, val creationTime: Long
            DeviceMessage dmessage = new DeviceMessage(splitmess[1],splitmess[0],mess,Long.parseLong(splitmess[splitmess.length - 1]));
            return dmessage;
        }
        else
            return  null;
    }
}
