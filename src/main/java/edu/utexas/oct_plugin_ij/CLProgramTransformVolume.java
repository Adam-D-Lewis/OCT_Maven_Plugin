package edu.utexas.oct_plugin_ij;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;
import edu.utexas.opencl.UTOpenCL;
import edu.utexas.opencl.UTOpenCLProgram;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * OpenCL functions to transform Volumes of data.
 * 
 * @author Austin McElroy
 * @version 0.2.0
 */
public class CLProgramTransformVolume extends UTOpenCLProgram {
	/**
	 * Input data to transform
	 */	
	private CLBuffer<ByteBuffer> Input_BB_Buffer;
	
	/**
	 * Output buffer, aprox 20% larger
	 */
	private CLBuffer<ByteBuffer> Output_BB_Buffer;
	
	/**
	 * Width, Height, Depth of the output buffer
	 */
	private int wOut;
	private int hOut;
	private int dOut;
	
	/*
	 * Width, Height, Depth of the input buffer
	 */
	private int wIn;
	private int hIn;
	private int dIn;
	
	/**
	 * Creates a new instance. This class covers the following transforms:<br>
	 * 1.) Rotate about the X-Axis<br>
	 * 2.) Rotate about Y-Axis<br>
	 * 3.) Rotate about the Z-Axis<br>
	 * @param OpenCLContext Created context
	 * @param PathToProgram Path to OpenCL .cl file that contains the functions
	 */
	public CLProgramTransformVolume(UTOpenCL ocl, InputStream is) {
		super(ocl, is);
	}
	
	/**
	 * 
	 * @param bb Byte buffer to load onto the OpenCL device
	 */
	public void loadVolume(ByteBuffer bb, int Width, int Height, int Depth){
		wIn = Width;
		hIn = Height;
		dIn = Depth;
		
		bb.rewind();
		
		Input_BB_Buffer = ocl.getContext().createByteBuffer(bb.limit(), Mem.READ_WRITE, Mem.ALLOCATE_BUFFER);
		//Get mapped memory buffer
		Input_BB_Buffer.getBuffer()
						.put(bb)
						.rewind();
		bb.clear();
		
		ocl.getQueue().putWriteBuffer(Input_BB_Buffer, false);
		
		//create an output buffer that is 20% larger than the input
		wOut = (int) Math.floor(Width*1.2f);
		hOut = (int) Math.floor(Height*1.2f);
		dOut = (int) Math.floor(Depth*1.2f);
		
		int temp_output_size = wOut*hOut*dOut;
		
		if(Output_BB_Buffer != null){
			Output_BB_Buffer.release();
		}		
		
		Output_BB_Buffer = ocl.getContext().createByteBuffer(temp_output_size, Mem.READ_WRITE, Mem.ALLOCATE_BUFFER);
		
		ocl.getQueue().finish();
	}
	
	/**
	 * Gets the transformed volume.
	 * 
	 * @return 
	 */
	public ByteBuffer getVolume(){
		Output_BB_Buffer.getBuffer().rewind();
		return Output_BB_Buffer.getBuffer();
	}
	
	/**
	 * Get the output dimensions
	 * @return int[] -> [x,y,z]
	 */
	public int[] getDimensions(){
		int[] dim = {wOut, hOut, dOut};
		return dim;
	}
	
	/**
	 * Transforms the volume.
	 * 
	 * @param rotX Value for x rotation angle
	 * @param rotY Value for y rotation angle
	 * @param rotZ Value for z rotation angle
	 * @param transX Value for x translation
	 * @param transY Value for y translation
	 * @param transZ Value for z translation
	 * @return getVolume()
	 */
	public ByteBuffer transform(float rotX, 
								float rotY, 
								float rotZ, 
								int transX, 
								int transY, 
								int transZ){
		
		CLKernel k = kernels.get("rotate_u8");
		
		k.setArg(0, Input_BB_Buffer);
		k.setArg(1, rotX);
		k.setArg(2, rotY);
		k.setArg(3, rotZ);
		k.setArg(4, wIn);
		k.setArg(5, hIn);
		k.setArg(6, dIn);
		k.setArg(7, Output_BB_Buffer);
		
		ocl.getQueue().put3DRangeKernel(k, 0, 0, 0, wOut, hOut, dOut, 0, 0, 0);
		
		//Unmap the memory to finalize copy
		ocl.getQueue().putReadBuffer(Output_BB_Buffer, false);

		ocl.getQueue().finish();
		
		Input_BB_Buffer.release();
		
		Input_BB_Buffer = null;
		
		return getVolume();
	}

	public CLBuffer<ByteBuffer> getOutputCLBuffer(){
		return Output_BB_Buffer;
	}
	
	public void cleanup(){
		if(Output_BB_Buffer != null)
			if(!Output_BB_Buffer.isReleased())
				Output_BB_Buffer.release();
		
		if(Input_BB_Buffer != null)
			if(!Input_BB_Buffer.isReleased())
				Input_BB_Buffer.release();
		
		Output_BB_Buffer = null;
		Input_BB_Buffer = null;
	}
	
}
