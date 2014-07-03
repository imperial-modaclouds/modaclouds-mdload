package mdload.client.comm;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPOutChannel extends OutChannel
{
	public InetAddress ip;
	public int port;
	
	public UDPOutChannel(InetAddress machineDest, int destport) {
		ip = machineDest;
		port = destport;
	}

	@Override
	public void send(Signal msg)
	{
		try{
			DatagramSocket clientSocket = new DatagramSocket();
			String strmsg =  new Long(msg.getSignal()).toString();
			byte[] sendData = new byte[ strmsg.length() ];
			DatagramPacket sendPacket = new DatagramPacket( strmsg.getBytes(),
					sendData.length, ip, port ); 
			clientSocket.send( sendPacket );
			clientSocket.close();
		}
		catch( Exception e )	{
			e.printStackTrace();
		}
	}
	
}
