package spim.process.interestpointregistration.pairwise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.ViewDescription;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import spim.Threads;
import spim.fiji.spimdata.interestpoints.InterestPoint;

public class MatcherPairwiseTools
{
	public static < V, I extends InterestPoint > List< Pair< Pair< V, V >, PairwiseResult< I > > > computePairs(
			final List< Pair< V, V > > pairs,
			final Map< V, List< I > > interestpoints,
			final MatcherPairwise< I > matcher )
	{
		return computePairs( pairs, interestpoints, matcher, null );
	}

	public static void assignLoggingViewIdsAndDescriptions(
			final Collection< ? extends Pair< ? extends Pair< ? extends ViewId, ? extends ViewId >, ? extends PairwiseResult< ? > > > r,
			final SequenceDescription sd )
	{
		for ( final Pair< ? extends Pair< ? extends ViewId, ? extends ViewId >, ? extends PairwiseResult< ? > > p : r )
		{
			// just for logging the names and results of pairwise comparison
			final ViewDescription viewA = sd.getViewDescription( p.getA().getA() );
			final ViewDescription viewB = sd.getViewDescription( p.getA().getB() );
			final PairwiseResult< ? > pwr = p.getB();

			pwr.setViewIdA( viewA );
			pwr.setViewIdB( viewB );

			final String description =
					"[TP=" + viewA.getTimePoint().getName() +
					" ViewId=" + viewA.getViewSetup().getId() +
					" >>> TP=" + viewB.getTimePoint().getName() +
					" ViewId=" + viewB.getViewSetup().getId() + "]";

			pwr.setDescription( description );
		}
	}
	/*
	public static void assignGroupedLoggingViewIdsAndDescriptions(
			final Collection< ? extends Pair< ? extends Pair< ? extends Group< ? extends ViewId >, ? extends Group< ? extends ViewId > >, ? extends PairwiseResult > > r,
			final SequenceDescription sd )
	{
		for ( final Pair< ? extends Pair< ? extends Group< ? extends ViewId >, ? extends Group< ? extends ViewId > >, ? extends PairwiseResult > p : r )
		{
			// just for logging the names and results of pairwise comparison
			final ViewDescription viewA = sd.getViewDescription( p.getA().getA() );
			final ViewDescription viewB = sd.getViewDescription( p.getA().getB() );
			final PairwiseResult pwr = p.getB();

			pwr.setViewIdA( viewA );
			pwr.setViewIdB( viewB );

			final String description =
					"[TP=" + viewA.getTimePoint().getName() +
					" ViewId=" + viewA.getViewSetup().getId() +
					" >>> TP=" + viewB.getTimePoint().getName() +
					" ViewId=" + viewB.getViewSetup().getId() + "]";

			pwr.setDescription( description );
		}
	}
	*/
	public static < V, I extends InterestPoint > List< Pair< Pair< V, V >, PairwiseResult< I > > > computePairs(
			final List< Pair< V, V > > pairs,
			final Map< V, ? extends List< I > > interestpoints,
			final MatcherPairwise< I > matcher,
			final ExecutorService exec )
	{
		final ExecutorService taskExecutor;
		
		if ( exec == null )
			taskExecutor = Executors.newFixedThreadPool( Threads.numThreads() );
		else
			taskExecutor = exec;

		final ArrayList< Callable< PairwiseResult< I > > > tasks = new ArrayList<>(); // your tasks

		for ( final Pair< V, V > pair : pairs )
		{
			final List< I > listA = interestpoints.get( pair.getA() );
			final List< I > listB = interestpoints.get( pair.getB() );

			tasks.add( new Callable< PairwiseResult< I > >()
			{
				@Override
				public PairwiseResult< I > call() throws Exception
				{
					return matcher.match( listA, listB );
				}
			});
		}

		final List< Pair< Pair< V, V >, PairwiseResult< I > > > r = new ArrayList<>();

		try
		{
			// invokeAll() returns when all tasks are complete
			List< Future< PairwiseResult< I > > > futures = taskExecutor.invokeAll( tasks );

			for ( int i = 0; i < pairs.size(); ++i )
			{
				final PairwiseResult< I > pwr = futures.get( i ).get();
				final Pair< V, V > pair = pairs.get( i );
				r.add( new ValuePair< Pair< V, V >, PairwiseResult< I > >( pair, pwr ) );
			}
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}

		if ( exec == null )
			taskExecutor.shutdown();

		// TODO:
		return r;
	}
}
