package com.example.livesocket.SocketManager;

import com.example.livesocket.Protocol.DataProtocol;

public class ConnectionClient
{
    private boolean isClosed;

    private ClientRequestTask mClientRequestTask;

    public ConnectionClient(RequestCallBack requestCallBack)
    {
        mClientRequestTask = new ClientRequestTask(requestCallBack);
        new Thread(mClientRequestTask).start();
    }

    public Boolean addNewRequest(DataProtocol data)
    {
        if (mClientRequestTask != null && !isClosed) {
            mClientRequestTask.addRequest(data);
            return true;
        }
        return false;
    }

    public void closeConnect()
    {
        isClosed = true;
        mClientRequestTask.stop();
    }
}
