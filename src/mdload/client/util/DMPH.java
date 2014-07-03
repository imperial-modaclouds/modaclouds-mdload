package mdload.client.util;

import mdload.client.ClientRandomStream;
import Jama.Matrix;

public class DMPH extends Distribution
{
	private Matrix D[];
	private Matrix iD0;
	private Matrix P;
	private Matrix pie;
	private Matrix jumpCDF;
	private Matrix hT; // holdingtimes

	private int nBatches;
	private int nStates;
	public int curState;

	public DMPH( int states )
	{
		curState = -1;
		nBatches = 1;
		nStates = states;
		iD0 = null;
		P = null;
		pie = null;
		hT = new Matrix( 1, nStates );
		D = new Matrix[ nBatches + 1 ];
		for( int k = 0; k <= nBatches; k++ )
			D[ k ] = new Matrix( nStates, nStates );
	}

	public double getRate( int batch, int fromState, int toState )
	{
		return D[ batch ].get( fromState - 1, toState - 1 );
	}

	public void setRate( int batch, int fromState, int toState, double rate )
	{
		D[ batch ].set( fromState - 1, toState - 1, rate );
		curState = -1;
		if( batch >= 1 || fromState != toState ) normalize();
	}

	public int getOrder()
	{
		return nStates;
	}

	public double getMeanHoldingTime( int state )
	{
		return ( double ) hT.get( 0, state - 1 );
	}

	public void scale( double meanRate )
	{
		double EX = mean();
		for( int i = 1; i <= nStates; i++ )
			for( int j = 1; j <= nStates; j++ )
			{
				setRate( 0, i, j, getRate( 0, i, j ) * EX * meanRate );
				setRate( 1, i, j, getRate( 1, i, j ) * EX * meanRate );
			}
	}

	public void normalize()
	{
		double cumulative = 0.0;
		for( int j = 1; j <= nStates; j++ )
		{
			cumulative = 0.0;
			for( int i = 0; i <= nBatches; i++ )
				for( int k = 1; k <= nStates; k++ )
					if( i >= 1 || j != k ) // skip diagonal of D[0]
						cumulative += getRate( i, j, k );
			setRate( 0, j, j, -cumulative );
			hT.set( 0, j - 1, -1.0 / getRate( 0, j, j ) );

		}
		iD0 = null;
		P = null;
		jumpCDF = null;
		curState = -1;
	}

	public Matrix getInverse()
	{
		if( iD0 == null ) iD0 = D[ 0 ].inverse().uminus();
		return iD0;
	}

	public Matrix getEmbedded()
	{
		if( P == null ) P = getInverse().times( D[ 1 ] );
		return P;
	}

	public double mean()
	{
		Matrix e = new Matrix( nStates, 1 );
		for( int i = 0; i < nStates; i++ )
		{
			e.set( i, 0, 1.0 );
		}
		return getPie().times( getInverse().times( e ) ).get( 0, 0 );
	}

	public Matrix getPie()
	{
		if( pie == null )
		{
			Matrix I = new Matrix( nStates, nStates );
			for( int i = 0; i < nStates; i++ )
			{
				I.set( i, i, 1.0 );
			}

			Matrix Q = getEmbedded().transpose().minus( I );
			Matrix b = new Matrix( nStates, 1 );
			b.set( nStates - 1, 0, 1 );
			for( int i = 0; i < nStates; i++ )
			{
				Q.set( nStates - 1, i, 1.0 );
			}

			pie = Q.solve( b ).transpose();
		}

		return pie;
	}

	public void print()
	{
		System.out.println( nStates );
		System.out.println( nBatches );
		System.out.println( " " );
		for( int i = 0; i <= nBatches; i++ )
			D[ i ].print( nBatches, 16 );
	}

	/* initialize state according to getPie() interval-stationary vector */
	public void initState()
	{
		double r = ClientRandomStream.random.nextDouble();
		curState = 0;
		while( r > getPie().getMatrix( 0, 0, 0, curState ).normInf() )
		{
			curState++;
		}
	}

	/* jump to next state */
	public boolean nextState()
	{
		double r = ClientRandomStream.random.nextDouble();
		int newState;

		if( jumpCDF == null )
		{
			jumpCDF = new Matrix( nStates, ( nBatches + 1 ) * nStates );
			for( int i = 0; i < nStates; i++ )
			{

				jumpCDF.setMatrix( i, i, 0, nStates - 1, D[ 0 ].getMatrix( i, i, 0,
						nStates - 1 ).times( getMeanHoldingTime( 1 + i ) ) );

				jumpCDF.setMatrix( i, i, nStates, ( nBatches + 1 ) * nStates - 1, D[ 1 ]
						.getMatrix( i, i, 0, nStates - 1 ).times(
								getMeanHoldingTime( 1 + i ) ) );
				jumpCDF.set( i, i, 0.0 );
			}
		}

		Matrix row = jumpCDF.getMatrix( curState, curState, 0, ( nBatches + 1 )
				* nStates - 1 );

		double rowCumSum = row.get( 0, 0 );
		newState = 0;
		while( r > rowCumSum )
		{
			newState++;
			if( newState == ( nBatches + 1 ) * nStates - 1 )
			{
				break;
			}
			rowCumSum = rowCumSum + row.get( 0, newState );
		}
		curState = newState % nStates;
		if( newState > nStates - 1 )
		{
			return false; // hidden transition?
		}
		else
		{
			return true; // hidden transition?
		}

	}

	public double next()
	{
		if( curState == -1 )
		{
			initState();
		}
		double sample = 0.0;

		do
		{
			double r = ClientRandomStream.random.nextDouble();

			sample = sample
					+ ( -( getMeanHoldingTime( curState + 1 ) ) * Math.log( 1.0 - r ) );
		}
		while( nextState() );

		return sample;
	}

	public static void main( String args[] )
	{
		DMPH m = new DMPH( 2 );

		/* erlang-2 */
		m.setRate( 0, 1, 2, 0.2 );
		m.setRate( 0, 2, 1, 0.0 );
		m.setRate( 1, 1, 1, 0.0 );
		m.setRate( 1, 2, 1, 1.0 );
		m.setRate( 1, 2, 2, 0.0 );
		m.print(); // print D0 and D1 matrices

		/* compute 3rd power moment, should be 24 */
		int n = 1000000; // 1e6
		double e3 = 0.0;
		for( int i = 0; i < n; i++ )
		{
			double x = m.next();
			e3 = e3 + x * x * x;
		}
		System.out.println( e3 / n );
		System.out.println( m.mean() );
		m.scale( 2 );
		System.out.println( m.mean() );

	}
}
