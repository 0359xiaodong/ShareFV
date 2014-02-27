package cn.ccsu.netdiscovery;

import java.net.*;
import java.util.Vector;

public class Receive  implements Runnable
{ 
  int port;                                        
  InetAddress group=null;                          
  MulticastSocket socket=null;     
  boolean flag=true;
 public Vector ipVector;
  public Receive()
   { 
     port=5000;  
     ipVector=new Vector();
     try{
         group=InetAddress.getByName("239.255.0.0");  
         socket=new MulticastSocket(port);            
         socket.joinGroup(group);           
                                            
       }
    catch(Exception e)
       {
       }                             
   }
  public void run()
  {
   
    while(flag)
    {
       byte data[]=new byte[8192];
       DatagramPacket packet=null;
       packet=new DatagramPacket(data,data.length,group,port);  
       try
           {  
             socket.receive(packet);
             String message=new String(packet.getData(),0,packet.getLength());
            if(!ipVector.contains(message))
            {
            	ipVector.add(message);
            }           
           }
      catch(Exception e)
           {
           } 
    }
  }
  public Vector getIp()
  {
	  return ipVector;
  }
 public void setFlag(boolean f)
 {
	 flag=f;
 }
} 
