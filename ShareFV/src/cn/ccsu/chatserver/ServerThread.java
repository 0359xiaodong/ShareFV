package cn.ccsu.chatserver;  
  
import java.util.*;  
import java.io.*;  
import java.net.*;  

import android.R.bool;

  
public class ServerThread extends Thread  
// 服务器监听端口线程  
{  
    // 声明ServerSocket类对象  
    ServerSocket serverSocket;  
    // 指定服务器监听端口常量  
    public static final int PORT = 1234;  
  
    boolean flag=true;
    /** 
     * 创建一个Vector对象，用于存储客户端连接的ClientThread对象 , ClientThread类维持服务器与单个客户端的连接线程 
     * 负责接收客户端发来的信息，clients负责存储所有与服务器建立连接的客户端 
     **/  
  
    Vector<ClientThread> clients;  
    // 创建一个Vector对象，用于存储客户端发送过来的信息  
    Vector<Object> messages;  
    // BroadCast类负责服务器向客户端广播消息  
    BroadCast broadcast;  
  
    String ip;  
    InetAddress myIPaddress = null;  
  
    public ServerThread() {  
        /*** 
         * 创建两个Vector数组非常重要 ， clients负责存储所有与服务器建立连接的客户端， 
         * messages负责存储服务器接收到的未发送出去的全部客户端的信息 
         *  
         **/  
        clients = new Vector<ClientThread>();  
        messages = new Vector<Object>();  
  
        try {  
            // 创建ServerSocket类对象  
            serverSocket = new ServerSocket(PORT);  
        } catch (IOException E) {  
        }  
        // 获取本地服务器地址信息  
        try {  
            myIPaddress = InetAddress.getLocalHost();  
        } catch (UnknownHostException e) {  
            System.out.println(e.toString());  
        }  
        ip = myIPaddress.getHostAddress();  
//        Server.jTextArea1.append("服务器地址：" + ip + "端口号:"  
//                + String.valueOf(serverSocket.getLocalPort()) + "\n");  
      
        // 创建广播信息线程并启动  
        broadcast = new BroadCast(this);  
        broadcast.start();  
    }  
  
    /** 
     * 注意：一旦监听到有新的客户端创建即new Socket(ip, PORT)被执行, 
     * 就创建一个ClientThread来维持服务器与这个客户端的连接 
     **/  
    public void run() {  
        while (flag) {  
            try {  
                // 获取客户端连接，并返回一个新的Socket对象  
                Socket socket = serverSocket.accept();  
  
                System.out.println(socket.getInetAddress().getHostAddress());  
                // 创建ClientThread线程并启动,可以监听该连接对应的客户端是否发送来消息， 并获取消息  
                ClientThread clientThread = new ClientThread(socket, this);  
                clientThread.start();  
                if (socket != null) {  
                    synchronized (clients) {  
                        // 将客户端连接加入到Vector数组中保存  
                        clients.addElement(clientThread);  
                    }  
                }  
            } catch (IOException E) {  
                System.out.println("发生异常：" + E);  
            }  
        }
    }  
  public void setFlag(boolean f)
  {
	  flag=f;
  }
    public void finalize() {  
        try {  
            // 关闭serverSocket方法  
        	setFlag(false);
            serverSocket.close();  
        } catch (IOException E) {  
        }  
        serverSocket = null;  
    }  
}  