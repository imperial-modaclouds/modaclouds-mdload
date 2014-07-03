package mdload.client.exploration;

public class Tuple< K, V >
{
	public K key;
	public V value;
	
	public Tuple( K key, V value )
	{
		this.key = key;
		this.value = value;
	}
	
	@Override
	public String toString()
	{
		return "(" + key.toString() + "," + value.toString() + ")";
	}
}
