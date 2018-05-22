package com.example.livesocket.SocketManager;

import com.example.livesocket.Protocol.BasicProtocol;

public interface RequestCallBack
{
    void onSuccess(BasicProtocol msg);

    void onFailed(int errorCode, String msg);
}
