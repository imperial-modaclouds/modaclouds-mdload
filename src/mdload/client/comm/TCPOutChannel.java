package mdload.client.comm;

import java.io.*;
import java.net.*;

public class TCPOutChannel extends OutChannel
{
	public InetAddress ip;
	public int port;
	public Socket clientSocket;

	public TCPOutChannel(InetAddress machineDest, int destport) {
		super();
		port = destport;
		ip = machineDest;
		open();
	}

	@Override
	public void send(Signal msg)
	{		
		String strmsg =  new Long(msg.getSignal()).toString();
		try
		{
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			outToServer.writeBytes(strmsg + '\n');
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
	}

	public void close() {
		try
		{
			clientSocket.close();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
	}

	public void open() {
		try
		{
			clientSocket = new Socket(ip, port);
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
	}


}
