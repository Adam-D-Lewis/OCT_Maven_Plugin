package edu.utexas.oct_plugin_ij;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;

import edu.utexas.opencl.UTOpenCLProgram;
import edu.utexas.primitives.Tuples.Triplet;
import edu.utexas.opencl.UTOpenCL;

public class CLProgramMerge extends UTOpenCLProgram implements WindowListener{
	
	/**
	 * Width, height, depth of the 
	 */
	int Width;
	int Height;
	int Depth;
	
	CLBuffer<ByteBuffer> Output_Buffer;
	
	public CLProgramMerge(UTOpenCL ocl, 
						InputStream is,
						int w, int h, int d) {
		super(ocl, is);
		
		Width = w;
		Height = h;
		Depth = d;
		
		long size = w*h*d;
		
		if(size > Integer.MAX_VALUE){
			log("Cannot allocate the output buffer, it is " + size + "; Max size is " + Integer.MAX_VALUE);
		}else{
			if(Output_Buffer != null){
				Output_Buffer.release();
			}
			Output_Buffer = ocl.getContext().createByteBuffer(w*h*d, Mem.READ_WRITE, Mem.ALLOCATE_BUFFER);
		}
	}

	/**
	 * Merges pre-allocated and filled buffers into a single buffer. 
	 * 
	 * @param toMerge - Array of allocated and filled buffers to merge
	 * @param dims - Dimensions of the allocated and filled arrays
	 * @param trans - Translation, in Pixels of the allocated and filled arrays 
	 * @return True if merge completed, false otherwise
	 */
	public boolean merge(List<CLBuffer<ByteBuffer>> toMerge, 
						List<Triplet<Integer, Integer, Integer>> dims, 
						List<Triplet<Integer, Integer, Integer>> trans){
		
		if(Output_Buffer == null){
			return false;
		}
		
		if(toMerge.size() <= 1){
			log("Multiple datasets not loaded, aborting merge...");
			return false;
		}
		
		int[] dimsArray = new int[dims.size()*3];
		int[] transArray = new int[trans.size()*3];
		
		for(int i = 0; i < dims.size(); i++){
			dimsArray[3*i] = dims.get(i).x();
			dimsArray[3*i + 1] = dims.get(i).y();
			dimsArray[3*i + 2] = dims.get(i).z();
			
			transArray[3*i] = trans.get(i).x();
			transArray[3*i + 1] = trans.get(i).y();
			transArray[3*i + 2] = trans.get(i).z();
		}
		
		//Allocate the dim and transform arrays on the device
		CLBuffer<IntBuffer> d_dimsArray = ocl.getContext().createIntBuffer(dimsArray.length, Mem.READ_WRITE, Mem.ALLOCATE_BUFFER);
		CLBuffer<IntBuffer> d_transArray = ocl.getContext().createIntBuffer(transArray.length, Mem.READ_WRITE, Mem.ALLOCATE_BUFFER);
		
		d_dimsArray.getBuffer().put(dimsArray).rewind();
		d_transArray.getBuffer().put(transArray).rewind();
		
		ocl.getQueue().putWriteBuffer(d_dimsArray, false);
		ocl.getQueue().putWriteBuffer(d_transArray, false);
		
		CLKernel k = null;
		
		switch(toMerge.size()){
		case 1:
			k = kernels.get("merge_1_to_1_u8");
			break;
		case 2:
			k = kernels.get("merge_2_to_1_u8");
			break;
		case 3:
			k = kernels.get("merge_3_to_1_u8");
			break;
		case 4:
			k = kernels.get("merge_4_to_1_u8");
			break;
		case 5:
			k = kernels.get("merge_5_to_1_u8");
			break;
		case 6:
			k = kernels.get("merge_6_to_1_u8");
			break;
		default:
				break;
		}
		
		if(k == null){
			log("Couldn't find kernel for the number of input arrays to merge");
			return false;
		}
		
		int i = 0;
		for(i = 0; i < toMerge.size(); i++){
			//ocl.getQueue().putWriteBuffer(toMerge.get(0), false);			
			k.setArg(i, toMerge.get(i));
		}
		
		k.setArg(i, d_dimsArray);
		i += 1;
		k.setArg(i, d_transArray);
		i += 1;
		k.setArg(i, Output_Buffer);

		ocl.getQueue().put3DRangeKernel(k, 0, 0, 0, Width, Height, Depth, 0, 0, 0);
		
		ocl.getQueue().finish();
		
		d_dimsArray.release();
		d_transArray.release();
		
		return true;
	}
	
	public void cleanup(){
		Output_Buffer.release();
	}
	
	public Triplet<Integer, Integer, Integer> getDims(){
		return new Triplet<Integer, Integer, Integer>(Width, Height, Depth);
	}
	
	public byte[] getMergedData(){
		ocl.getQueue().putReadBuffer(Output_Buffer, true);
		ocl.getQueue().finish();
		byte[] buffer = new byte[Width*Height*Depth];
		Output_Buffer.getBuffer().get(buffer);
		return buffer;
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		if(Output_Buffer != null)
			Output_Buffer.release();
		
		Output_Buffer = null;
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	
}
