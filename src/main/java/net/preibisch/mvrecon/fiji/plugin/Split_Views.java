/*-
 * #%L
 * Software for the reconstruction of multi-view microscopic acquisitions
 * like Selective Plane Illumination Microscopy (SPIM) Data.
 * %%
 * Copyright (C) 2012 - 2024 Multiview Reconstruction developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package net.preibisch.mvrecon.fiji.plugin;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;

import fiji.util.gui.GenericDialogPlus;
import ij.ImageJ;
import ij.plugin.PlugIn;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;
import net.imglib2.Dimensions;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import net.preibisch.legacy.io.IOFunctions;
import net.preibisch.mvrecon.fiji.plugin.queryXML.GenericLoadParseQueryXML;
import net.preibisch.mvrecon.fiji.plugin.queryXML.LoadParseQueryXML;
import net.preibisch.mvrecon.fiji.plugin.util.GUIHelper;
import net.preibisch.mvrecon.fiji.spimdata.SpimData2;
import net.preibisch.mvrecon.fiji.spimdata.XmlIoSpimData2;
import net.preibisch.mvrecon.fiji.spimdata.explorer.ViewSetupExplorer;
import net.preibisch.mvrecon.process.splitting.SplittingTools;

public class Split_Views implements PlugIn
{
	public static long defaultImgX = 500;
	public static long defaultImgY = 500;
	public static long defaultImgZ = 200;

	public static long defaultOverlapX = 60;
	public static long defaultOverlapY = 60;
	public static long defaultOverlapZ = 20;

	public static boolean defaultOptimize = true;

	public static String defaultPath = null;

	public static int defaultChoice = 0;
	private static final String[] resultChoice = new String[] { "Display", "Save & Close" };

	@Override
	public void run(String arg)
	{
		final LoadParseQueryXML xml = new LoadParseQueryXML();

		if ( !xml.queryXML( "splitting/subdiving of views", false, false, false, false, false ) )
			return;

		final SpimData2 data = xml.getData();

		split( data, xml.getXMLFileName() );
	}

	public static boolean split(
			final SpimData2 data,
			final String saveAs,
			final long[] targetSize,
			final long[] overlap,
			final long[] minStepSize,
			final boolean optimize,
			final boolean display )
	{
		final SpimData2 newSD = SplittingTools.splitImages( data, overlap, targetSize, minStepSize, optimize );

		if ( display )
		{
			final ViewSetupExplorer< SpimData2 > explorer = new ViewSetupExplorer<>( newSD, saveAs, new XmlIoSpimData2( "" ) );
			explorer.getFrame().toFront();
		}
		else
		{
			SpimData2.saveXML( data, saveAs, "" );
		}

		return true;
	}

	public static boolean split( final SpimData2 data, final String fileName )
	{
		final long[] minStepSize = findMinStepSize( data );

		final Pair< HashMap< String, Integer >, long[] > imgSizes = collectImageSizes( data );

		IOFunctions.println( "Current image sizes of dataset :");

		for ( final String size : imgSizes.getA().keySet() )
			IOFunctions.println( imgSizes.getA().get( size ) + "x: " + size );

		final GenericDialogPlus gd = new GenericDialogPlus( "Dataset splitting/subdividing" );

		defaultImgX = closestLargerLongDivisableBy( defaultImgX, minStepSize[ 0 ] );
		defaultImgY = closestLargerLongDivisableBy( defaultImgY, minStepSize[ 1 ] );
		defaultImgZ = closestLargerLongDivisableBy( defaultImgZ, minStepSize[ 2 ] );

		defaultOverlapX = closestLargerLongDivisableBy( defaultOverlapX, minStepSize[ 0 ] );
		defaultOverlapY = closestLargerLongDivisableBy( defaultOverlapY, minStepSize[ 1 ] );
		defaultOverlapZ = closestLargerLongDivisableBy( defaultOverlapZ, minStepSize[ 2 ] );

		gd.addSlider( "Target_Image_Size_X", 100, 2000, defaultImgX, minStepSize[ 0 ] );
		gd.addSlider( "Target_Image_Size_Y", 100, 2000, defaultImgY, minStepSize[ 1 ] );
		gd.addSlider( "Target_Image_Size_Z", 100, 2000, defaultImgZ, minStepSize[ 2 ] );

		gd.addCheckbox( "Optimize_image_sizes per view", defaultOptimize );

		gd.addMessage( "Note: new sizes will be adjusted to be divisible by " + Arrays.toString( minStepSize ), GUIHelper.mediumstatusfont, Color.RED );
		gd.addMessage( "" );

		gd.addSlider( "Overlap_X", 10, 200, defaultOverlapX, minStepSize[ 0 ] );
		gd.addSlider( "Overlap_Y", 10, 200, defaultOverlapY, minStepSize[ 1 ] );
		gd.addSlider( "Overlap_Z", 10, 200, defaultOverlapZ, minStepSize[ 2 ] );

		gd.addMessage( "Note: overlap will be adjusted to be divisible by " + Arrays.toString( minStepSize ), GUIHelper.mediumstatusfont, Color.RED );
		gd.addMessage( "Minimal image sizes per dimension: " + Util.printCoordinates( imgSizes.getB() ), GUIHelper.mediumstatusfont, Color.DARK_GRAY );

		gd.addMessage( "" );

		IOFunctions.println( fileName );
		if ( defaultPath == null || defaultPath.trim().length() == 0 )
		{
			final int index = fileName.indexOf( ".xml");
			if ( index > 0 )
				defaultPath = fileName.substring( 0, index ) + ".split.xml";
			else
				defaultPath = fileName + ".split.xml";
		}

		gd.addFileField("New_XML_File", defaultPath);
		gd.addChoice( "Split_Result", resultChoice, resultChoice[ defaultChoice ] );

		gd.showDialog();

		if ( gd.wasCanceled() )
			return false;

		final long sx = defaultImgX = closestLargerLongDivisableBy( Math.round( gd.getNextNumber() ), minStepSize[ 0 ] );
		final long sy = defaultImgY = closestLargerLongDivisableBy( Math.round( gd.getNextNumber() ), minStepSize[ 1 ] );
		final long sz = defaultImgZ = closestLargerLongDivisableBy( Math.round( gd.getNextNumber() ), minStepSize[ 2 ] );

		final boolean optimize = defaultOptimize = gd.getNextBoolean();

		final long ox = defaultOverlapX = closestLargerLongDivisableBy( Math.round( gd.getNextNumber() ), minStepSize[ 0 ] );
		final long oy = defaultOverlapY = closestLargerLongDivisableBy( Math.round( gd.getNextNumber() ), minStepSize[ 1 ] );
		final long oz = defaultOverlapZ = closestLargerLongDivisableBy( Math.round( gd.getNextNumber() ), minStepSize[ 2 ] );

		final String saveAs = defaultPath = gd.getNextString();
		final int choice = defaultChoice = gd.getNextChoiceIndex();

		System.out.println( sx + ", " + sy + ", " + sz + ", " + ox  + ", " + oy  + ", " + oz );

		if ( ox > sx || oy > sy || oz > sz )
		{
			IOFunctions.println( "overlap cannot be bigger than size" );

			return false;
		}

		return split( data, saveAs, new long[]{ sx, sy, sz }, new long[]{ ox, oy, oz }, minStepSize, optimize, choice == 0 );
	}

	public static Pair< HashMap< String, Integer >, long[] > collectImageSizes( final AbstractSpimData< ? > data )
	{
		final HashMap< String, Integer > sizes = new HashMap<>();

		long[] minSize = null;

		for ( final BasicViewSetup vs : data.getSequenceDescription().getViewSetupsOrdered() )
		{
			final Dimensions dim = vs.getSize();

			String size = Long.toString( dim.dimension( 0 ) );
			for ( int d = 1; d < dim.numDimensions(); ++d )
				size += "x" + dim.dimension( d );

			if ( sizes.containsKey( size ) )
				sizes.put( size, sizes.get( size ) + 1 );
			else
				sizes.put( size, 1 );

			if ( minSize == null )
			{
				minSize = new long[ dim.numDimensions() ];
				dim.dimensions( minSize );
			}
			else
			{
				for ( int d = 0; d < dim.numDimensions(); ++d )
					minSize[ d ] = Math.min( minSize[ d ], dim.dimension( d ) );
			}
		}

		return new ValuePair<HashMap<String,Integer>, long[]>( sizes, minSize );
	}

	public static long greatestCommonDivisor( long a, long b )
	{
		while (b > 0)
		{
			long temp = b;
			b = a % b;
			a = temp;
		}
		return a;
	}

	public static long lowestCommonMultiplier( long a, long b )
	{
		return a * (b / greatestCommonDivisor(a, b));
	}

	public static long closestSmallerLongDivisableBy( final long a, final long b )
	{
		if ( a == b || a == 0 || a % b == 0  )
			return a;
		else
			return a - (a % b);
	}

	public static long closestLargerLongDivisableBy( final long a, final long b )
	{
		if ( a == b || a == 0 || a % b == 0 )
			return a;
		else
			return (a + b) - (a % b);
	}

	public static long closestLongDivisableBy( final long a, final long b)
	{
		final long c1 = closestSmallerLongDivisableBy( a, b );//a - (a % b);
		final long c2 = closestLargerLongDivisableBy( a, b ); //(a + b) - (a % b);

		if (a - c1 > c2 - a)
			return c2;
		else
			return c1;
	}

	public static long[] findMinStepSize( final AbstractSpimData< ? > data )
	{
		final BasicImgLoader imgLoader = data.getSequenceDescription().getImgLoader();

		final long[] minStepSize = new long[] { 1, 1, 1 };

		if ( MultiResolutionImgLoader.class.isInstance( imgLoader ) )
		{
			IOFunctions.println( "We have a multi-resolution image loader: " + imgLoader.getClass().getName() + ", finding resolution steps");

			final MultiResolutionImgLoader mrImgLoader = ( MultiResolutionImgLoader ) imgLoader;

			for ( final BasicViewSetup vs : data.getSequenceDescription().getViewSetupsOrdered() )
			{
				final double[][] mipmapResolutions = mrImgLoader.getSetupImgLoader( vs.getId() ).getMipmapResolutions();

				IOFunctions.println( "ViewSetup: " + vs.getName() + " (id=" + vs.getId() + "): " + Arrays.deepToString( mipmapResolutions ) );

				// lowest resolution defines the minimal steps size 
				final double[] lowestResolution = mipmapResolutions[ mipmapResolutions.length - 1 ];

				for ( int d = 0; d < minStepSize.length; ++d )
				{
					if ( lowestResolution[ d ] % 1 != 0.0 )
						throw new RuntimeException( "Downsampling has a fraction, cannot split dataset" );

					minStepSize[ d ] = lowestCommonMultiplier( minStepSize[ d ], Math.round( lowestResolution[ d ] ) );
				}
			}
		}
		else
		{
			IOFunctions.println( "Not a multi-resolution image loader, all data splits are possible." );
		}

		IOFunctions.println( "Final minimal step size: " + Arrays.toString( minStepSize ) );

		return minStepSize;
	}
	
	public static void main( String[] args ) throws SpimDataException
	{
		new ImageJ();

		GenericLoadParseQueryXML.defaultXMLfilename = "/Users/preibischs/SparkTest/IP/dataset.xml";

		new Split_Views().run( null );
		//SpimData2 data = new XmlIoSpimData2("").load( GenericLoadParseQueryXML.defaultXMLfilename );
		//findMinStepSize(data);
	}
}
