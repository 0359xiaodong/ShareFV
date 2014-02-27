package cn.ccsu.chatserver;  
  
import java.util.*;  
import java.io.*;  
import java.net.*;  

import android.R.bool;

  
public class ServerThread extends Thread  
// �����������˿��߳�  
{  
    // ����ServerSocket�����  
    ServerSocket serverSocket;  
    // ָ�������������˿ڳ���  
    public static final int PORT = 1234;  
  
    boolean flag=true;
    /** 
     * ����һ��Vector�������ڴ洢�ͻ������ӵ�ClientThread���� , ClientThread��ά�ַ������뵥���ͻ��˵������߳� 
     * ������տͻ��˷�������Ϣ��clients����洢������������������ӵĿͻ��� 
     **/  
  
    Vector<ClientThread> clients;  
    // ����һ��Vector�������ڴ洢�ͻ��˷��͹�������Ϣ  
    Vector<Object> messages;  
    // BroadCast�ฺ���������ͻ��˹㲥��Ϣ  
    BroadCast broadcast;  
  
    String ip;  
    InetAddress myIPaddress = null;  
  
    public ServerThread() {  
        /*** 
         * ��������Vector����ǳ���Ҫ �� clients����洢������������������ӵĿͻ��ˣ� 
         * messages����洢���������յ���δ���ͳ�ȥ��ȫ���ͻ��˵���Ϣ 
         *  
         **/  
        clients = new Vector<ClientThread>();  
        messages = new Vector<Object>();  
  
        try {  
            // ����ServerSocket�����  
            serverSocket = new ServerSocket(PORT);  
        } catch (IOException E) {  
        }  
        // ��ȡ���ط�������ַ��Ϣ  
        try {  
            myIPaddress = InetAddress.getLocalHost();  
        } catch (UnknownHostException e) {  
            System.out.println(e.toString());  
        }  
        ip = myIPaddress.getHostAddress();  
//        Server.jTextArea1.append("��������ַ��" + ip + "�˿ں�:"  
//                + String.valueOf(serverSocket.getLocalPort()) + "\n");  
      
        // �����㲥��Ϣ�̲߳�����  
        broadcast = new BroadCast(this);  
        broadcast.start();  
    }  
  
    /** 
     * ע�⣺һ�����������µĿͻ��˴�����new Socket(ip, PORT)��ִ��, 
     * �ʹ���һ��ClientThread��ά�ַ�����������ͻ��˵����� 
     **/  
    public void run() {  
        while (flag) {  
            try {  
                // ��ȡ�ͻ������ӣ�������һ���µ�Socket����  
                Socket socket = serverSocket.accept();  
  
                System.out.println(socket.getInetAddress().getHostAddress());  
                // ����ClientThread�̲߳�����,���Լ��������Ӷ�Ӧ�Ŀͻ����Ƿ�������Ϣ�� ����ȡ��Ϣ  
                ClientThread clientThread = new ClientThread(socket, this);  
                clientThread.start();  
                if (socket != null) {  
                    synchronized (clients) {  
                        // ���ͻ������Ӽ��뵽Vector�����б���  
                        clients.addElement(clientThread);  
                    }  
                }  
            } catch (IOException E) {  
                System.out.println("�����쳣��" + E);  
            }  
        }
    }  
  public void setFlag(boolean f)
  {
	  flag=f;
  }
    public void finalize() {  
        try {  
            // �ر�serverSocket����  
        	setFlag(false);
            serverSocket.close();  
        } catch (IOException E) {  
        }  
        serverSocket = null;  
    }  
}  