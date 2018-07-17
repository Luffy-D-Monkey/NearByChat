
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Protocol.Config;
import Protocol.DataProtocol;
import Protocol.ResponseCallback;
import Protocol.ServerResponseTask;

/**
 * Edited at 2018.07.13.
 */
public class ConnectionServer 
{

    private static boolean isStart = true;
    private static ServerResponseTask serverResponseTask;

    public ConnectionServer() {

    }

    public static void main(String[] args) {

        ServerSocket serverSocket = null;
        ExecutorService executorService = Executors.newCachedThreadPool();
        try 
        {
            serverSocket = new ServerSocket(Config.PORT);
            while (isStart) 
            {
                Socket socket = serverSocket.accept();
                //System.out.println("socket statues 6**" + socket.isClosed());

                serverResponseTask = new ServerResponseTask(socket,
                        new ResponseCallback() {

                            @Override
                            public void targetIsOffline(DataProtocol reciveMsg)

                            {// 对方不在线
                                if (reciveMsg != null) {
                                    //System.out.println(reciveMsg.getData());
                                }
                            }

                            @Override
                            public void targetIsOnline(String clientIp) {
                                System.out.println(clientIp + " is onLine");
                                System.out.println("-----------------------------------------");
                                
                            
                            
                            }
                        });

                if (socket.isConnected()) 
                {
                    executorService.execute(serverResponseTask);
                    //System.out.println("socket statues 7**" + socket.isClosed());

                }
            }
            

            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    isStart = false;
                    serverSocket.close();
                    if (serverSocket != null)
                        serverResponseTask.sttop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
