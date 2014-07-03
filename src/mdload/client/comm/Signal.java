package mdload.client.comm;

public class Signal
{
	private long sgn;
	
	public final static long DISPATCHER_READY = 10;
	public final static long DISPATCHER_WARMUP_START = 11;
	public final static long DISPATCHER_WARMUP_END = 12;
		
	public Signal(long signal) {
		setSignal(signal);
	}
	
	public long getSignal()
	{
		return sgn;
	}
	
	public void setSignal( long msg )
	{
		this.sgn = msg;
	}
}
