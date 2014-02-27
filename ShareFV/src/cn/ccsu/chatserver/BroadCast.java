package cn.ccsu.chatserver;  
  
import java.io.*;  
  
public class BroadCast extends Thread { // ��������ͻ��˹㲥�߳�  
    ClientThread clientThread;  
    // ����ServerThread����  
    ServerThread serverThread;  
    String str;  
  
    public BroadCast(ServerThread serverThread) {  
        this.serverThread = serverThread;  
    }  
  
    // �÷����������ǲ�ͣ�������пͻ��˷�������Ϣ  
    public void run() {  
        while (true) {  
            try {  
                // �߳�����200 ms  
                Thread.sleep(200);  
            } catch (InterruptedException E) {  
            }  
  
            // ͬ����serverThread.messages  
            synchronized (serverThread.messages) {  
                // �ж��Ƿ���δ������Ϣ  
                if (serverThread.messages.isEmpty()) {  
                    continue;  
                }  
                // ��ȡ�������˴洢����Ҫ���͵ĵ�һ��������Ϣ  
                str = (String) this.serverThread.messages.firstElement();  
            }  
            // ͬ����serverThread.clients  
            synchronized (serverThread.clients) {  
                // ����ѭ����ȡ�������д洢�����н�������ͻ��˵�����  
                for (int i = 0; i < serverThread.clients.size(); i++) {  
                    clientThread = (ClientThread) serverThread.clients  
                            .elementAt(i);  
                    try {  
                        // ���¼��ÿһ���ͻ��˷���������Ϣ  
                        clientThread.out.writeUTF(str);  
                    } catch (IOException E) {  
                    }  
                }  
                // ��Vector������ɾ���Ѿ����͹�������������Ϣ  
                this.serverThread.messages.removeElement(str);  
            }  
        }  
    }  
}  