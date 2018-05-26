package com.esdraslopez.android.nearbychat.Service;

public final class ServiceBrocastType
{
    public static final String TYPE = "com.esdraslopez.android.nearbychat.Service";

    public static final String DATAPROTOCOL = "com.esdraslopez.android.nearbychat.Service.DATAPROTOCOL";
    public static final String GPSSTATUESCHANGE = "com.esdraslopez.android.nearbychat.Service.GPSSTATUESCHANGE";
    public static final String RECEPUSHMESSAGE = "com.esdraslopez.android.nearbychat.Service.RECEPUSHMESSAGE";

    public static final String SETadditonal = "com.esdraslopez.android.nearbychat.Service.SETadditonal";//Activity发送username，id，等消息给Service设置相应属性
    public static final String ServiceActionReceiver = "com.example.livesocket.Protocol.ServiceActionReceiver";//在Service中注册监听的Action


    public static final String userName = "com.example.livesocket.Protocol.userName";//在Service中注册监听的Action

    public static final String userUuid = "com.example.livesocket.Protocol.useruuid";
    public static final String PUSHBROADCAST = "com.example.livesocket.Protocol.PUSHBROADCAST";//在Activity中注册监听的PUSH的Action


}
