package mdload.client.exploration;

import java.util.PriorityQueue;

public class Selector< V >
{
	public V select( double probability, PriorityQueue< Tuple< Double, V > > queue )
	{
		double cumulative = 0.0;
		
		while( !queue.isEmpty() )
		{
			Tuple< Double, V > tuple = queue.remove();
			
			cumulative += tuple.key;
			
			if( probability <= cumulative )
			{
				return tuple.value;
			}
		}
		
		// something went wrong
		return null;
	}
}
