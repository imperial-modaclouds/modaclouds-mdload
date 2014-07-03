package mdload.client.comm;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPInChannel extends InChannel
{
	public int port;
	public DatagramSocket serverSocket;
	
	public UDPInChannel(int listenport) {
		super();
		port = listenport;
	}
	
	@Override
	public Signal receive()
	{		
		byte[] receiveData = new byte[ 1024 ];
		DatagramPacket receivePacket = new DatagramPacket( receiveData,
				receiveData.length );
		
		try
		{
			serverSocket = new DatagramSocket( port );
		}
		catch( SocketException e )
		{
			e.printStackTrace();
		}
		
		try
		{
			serverSocket.receive( receivePacket );
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
		
		String str = null;
		try
		{
			str = new String(receivePacket.getData(), 0, receivePacket.getLength(), "US-ASCII");
		}
		catch( UnsupportedEncodingException e )
		{
			e.printStackTrace();
		}
		Signal msg = new Signal(Long.valueOf(str));
		close();

		return msg;
	}

	public void close() {
		serverSocket.close();
	}
	
	
}
