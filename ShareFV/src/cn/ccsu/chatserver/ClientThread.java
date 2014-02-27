package cn.ccsu.chatserver;

import java.net.*;
import java.io.*;

public class ClientThread extends Thread {
	/**
	 * ά�ַ������뵥���ͻ��˵������̣߳�������տͻ��˷�������Ϣ, ����һ���µ�Socket���� ���ڱ������������accept�����õ��Ŀͻ��˵�����
	 **/
	Socket clientSocket;
	boolean flag=true;
	// �������������д洢��Socket������������룯�����
	DataInputStream in = null;
	DataOutputStream out = null;

	// ����ServerThread����
	ServerThread serverThread;

	public ClientThread(Socket socket, ServerThread serverThread) {
		clientSocket = socket;
		this.serverThread = serverThread;
		try {
			// �������������������룯�����
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e2) {
			System.out.println("�����쳣" + e2);
			System.out.println("����I/Oͨ��ʧ�ܣ�");
			System.exit(3);
		}
	}

	// �÷������������Ӷ�Ӧ�ÿͻ����Ƿ�����Ϣ����
	public void run() {
		while (flag) {
			try {
				// ����ͻ��˷���������Ϣ
				String message = in.readUTF();
				String subMessage = message.substring(0, 5);
				if (subMessage == "record") {
					synchronized (serverThread.messages) {
						if (message != null) {
							// ���ͻ��˷��������ļ�������serverThread��messages������
							serverThread.messages.addElement(message);
							// �ڷ������˵��ı�������ʾ���ļ���
//							Server.jTextArea1.append(message + '\n');
							 DataOutputStream dos = null;
							 DataInputStream dis=null;
							// �Ѿ�������ļ���С  
						        int passedlength = 0;  
						        // �ļ��ܴ�С  
						        long length = 0;   
						        // �����С  
						        int bufferSize = 8192;   
						        // ����  
						        byte[] buf = new byte[bufferSize]; 
							try {    
					          dos = new DataOutputStream(new BufferedOutputStream(  
					                    new FileOutputStream("E:\\tmp\\"+message))); 
					          dis = new DataInputStream(new BufferedInputStream(  
					        		  clientSocket.getInputStream()));
					        } catch (FileNotFoundException e) {    
					            e.printStackTrace();    
					        }    
					        while (true) {    
					            int read = 0;    
					            if (dis != null) {    
					                try {    
					                    read = dis.read(buf);    
					                } catch (IOException e) {    
					                    e.printStackTrace();    
					                    }  
					            }    
					            passedlength += read;    
					            if (read == -1) {    
					                break;    
					            }            
					            // �����������Ϊͼ�ν����prograssBar���ģ���������Ǵ��ļ������ܻ��ظ���ӡ��һЩ��ͬ�İٷֱ�  
					            System.out.println("�ļ�������" + (passedlength * 100 / length) + "%\n");    
					            try {    
					                dos.write(buf, 0, read);    
					            } catch (IOException e) {    
					                e.printStackTrace();    
					            }    
						}
					}
					}
				} else {
					synchronized (serverThread.messages) {
						if (message != null) {
							// ���ͻ��˷���������Ϣ����serverThread��messages������
							serverThread.messages.addElement(message);
							// �ڷ������˵��ı�������ʾ����Ϣ
//							Server.jTextArea1.append(message + '\n');
						}
					}
				}
			} catch (IOException E) {
				break;
			}
		}
	}
	public void setFlag(boolean f)
	{
		flag=f;
	}
}