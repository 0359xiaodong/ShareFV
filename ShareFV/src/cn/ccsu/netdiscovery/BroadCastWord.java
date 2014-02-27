package cn.ccsu.netdiscovery;


import java.io.*;
import java.net.*;

import cn.ccsu.ShareFV.IJetty;

 
@SuppressWarnings("serial")
public class BroadCastWord  implements Runnable
{   
   int port;                                        
   InetAddress group=null;                          
   MulticastSocket socket=null;                                                   
   File file=null;                                  
   String FileDir=null,fileName=null;
   FileReader in=null;                              
   BufferedReader bufferIn=null;   
   boolean flag=true;
  public BroadCastWord()
  {

   try 
      {
       port=5000;                                   
       group=InetAddress.getByName("239.255.0.0");  
       socket=new MulticastSocket(port);            
       socket.setTimeToLive(1);                     
       socket.joinGroup(group); 
       
                                                
      } 
  catch(Exception e)
       {
         System.out.println("Error: "+ e);          
       }

 }                                        
// public static void main(String[] args) 
//   {
//      BroadCastWord broad=new BroadCastWord();
//      Thread t=new Thread(broad);
//      t.start();
//   }
@Override
public void run() {
	// TODO Auto-generated method stub
	while(flag){
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DatagramPacket packet=null;   
		String message=IJetty.hostip+"and"+IJetty.mLat+"and"+IJetty.mLon+"and"+IJetty.VideoName;
		//String message=IJetty.hostip+"and"+27.718300370250046+"and"+112.00947137621269+"and"+IJetty.VideoName;
	     byte data[]=message.getBytes(); 
	     packet=new DatagramPacket(data,data.length,group,port); 
	     try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	} 
}

	public void setFlag(boolean f)
	{
		flag=f;
	}
}
