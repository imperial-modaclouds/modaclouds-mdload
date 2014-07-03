package mdload.client.comm;


public abstract class OutChannel
{
	public OutChannel() {
		
	}
	
	public abstract void send(Signal msg);
}
