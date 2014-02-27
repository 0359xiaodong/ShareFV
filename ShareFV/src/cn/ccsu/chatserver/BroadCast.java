package cn.ccsu.chatserver;  
  
import java.io.*;  
  
public class BroadCast extends Thread { // 服务器向客户端广播线程  
    ClientThread clientThread;  
    // 声明ServerThread对象  
    ServerThread serverThread;  
    String str;  
  
    public BroadCast(ServerThread serverThread) {  
        this.serverThread = serverThread;  
    }  
  
    // 该方法的作用是不停地向所有客户端发送新消息  
    public void run() {  
        while (true) {  
            try {  
                // 线程休眠200 ms  
                Thread.sleep(200);  
            } catch (InterruptedException E) {  
            }  
  
            // 同步化serverThread.messages  
            synchronized (serverThread.messages) {  
                // 判断是否有未发的消息  
                if (serverThread.messages.isEmpty()) {  
                    continue;  
                }  
                // 获取服务器端存储的需要发送的第一条数据信息  
                str = (String) this.serverThread.messages.firstElement();  
            }  
            // 同步化serverThread.clients  
            synchronized (serverThread.clients) {  
                // 利用循环获取服务器中存储的所有建立的与客户端的连接  
                for (int i = 0; i < serverThread.clients.size(); i++) {  
                    clientThread = (ClientThread) serverThread.clients  
                            .elementAt(i);  
                    try {  
                        // 向记录的每一个客户端发送数据信息  
                        clientThread.out.writeUTF(str);  
                    } catch (IOException E) {  
                    }  
                }  
                // 从Vector数组中删除已经发送过的那条数据信息  
                this.serverThread.messages.removeElement(str);  
            }  
        }  
    }  
}  