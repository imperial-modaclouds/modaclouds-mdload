package mdload.client.comm;

import java.io.*;
import java.net.*;

public class TCPInChannel extends InChannel
{
	public int port;
	public ServerSocket serverSocket;
	public Socket connectionSocket;

	public TCPInChannel(int listenport) {
		super();
		port = listenport;
		open();
		connectionSocket = null;
}

	@Override
	public Signal receive()
	{		
		BufferedReader inFromClient;
		String str = null;
		try
		{
			if (connectionSocket == null) {
				connectionSocket = serverSocket.accept();
			}
			inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			str = inFromClient.readLine();
			//connectionSocket.close();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}

		Signal msg = new Signal(Long.valueOf(str));

		return msg;
	}

	public void close() {
		try
		{
			serverSocket.close();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
	}

	public void open() {
		try
		{
			serverSocket = new ServerSocket(port);
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
	}


}
