package spim.process.deconvolution.iteration;

public interface ComputeBlockThreadFactory
{
	public ComputeBlockThread create( final int id );
	public int numParallelBlocks();
}
