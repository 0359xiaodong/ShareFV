package cn.ccsu.chatserver;

import java.net.*;
import java.io.*;

public class ClientThread extends Thread {
	/**
	 * 维持服务器与单个客户端的连接线程，负责接收客户端发来的信息, 声明一个新的Socket对象， 用于保存服务器端用accept方法得到的客户端的连接
	 **/
	Socket clientSocket;
	boolean flag=true;
	// 声明服务器端中存储的Socket对象的数据输入／输出流
	DataInputStream in = null;
	DataOutputStream out = null;

	// 声明ServerThread对象
	ServerThread serverThread;

	public ClientThread(Socket socket, ServerThread serverThread) {
		clientSocket = socket;
		this.serverThread = serverThread;
		try {
			// 创建服务器端数据输入／输出流
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e2) {
			System.out.println("发生异常" + e2);
			System.out.println("建立I/O通道失败！");
			System.exit(3);
		}
	}

	// 该方法监听该连接对应得客户端是否有消息发送
	public void run() {
		while (flag) {
			try {
				// 读入客户端发送来的信息
				String message = in.readUTF();
				String subMessage = message.substring(0, 5);
				if (subMessage == "record") {
					synchronized (serverThread.messages) {
						if (message != null) {
							// 将客户端发送来得文件名存于serverThread的messages数组中
							serverThread.messages.addElement(message);
							// 在服务器端的文本框中显示新文件名
//							Server.jTextArea1.append(message + '\n');
							 DataOutputStream dos = null;
							 DataInputStream dis=null;
							// 已经传输的文件大小  
						        int passedlength = 0;  
						        // 文件总大小  
						        long length = 0;   
						        // 缓存大小  
						        int bufferSize = 8192;   
						        // 缓存  
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
					            // 下面进度条本为图形界面的prograssBar做的，这里如果是打文件，可能会重复打印出一些相同的百分比  
					            System.out.println("文件接收了" + (passedlength * 100 / length) + "%\n");    
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
							// 将客户端发送来得信息存于serverThread的messages数组中
							serverThread.messages.addElement(message);
							// 在服务器端的文本框中显示新消息
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