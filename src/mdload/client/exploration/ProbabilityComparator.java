package mdload.client.exploration;

import mdload.client.workload.Request;
import mdload.client.exploration.Tuple;

import java.util.Comparator;

public class ProbabilityComparator implements Comparator< Tuple< Double, Request > >
{
	@Override
	public int compare( Tuple< Double, Request > o1,
			Tuple< Double, Request > o2 )
	{
		if( o1.key > o2.key ) return -1;
		else if( o1.key < o2.key ) return 1;
		else return 0;
	}
}
