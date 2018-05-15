package edu.utexas.oct_plugin_ij;

import java.io.InputStream;
import java.nio.ByteBuffer;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Map;
import com.jogamp.opencl.CLMemory.Mem;

import edu.utexas.opencl.UTOpenCLProgram;
import edu.utexas.opencl.UTOpenCL;

/**
 * OpenCL functions to transform Volumes of data.
 * 
 * @author Austin McElroy
 * @version 0.1.0
 */
public class TransformVolumeCLProgram extends UTOpenCLProgram {
	/**
	 * Input data to transform
	 */
	private CLBuffer<?> d_pinnedInput = null;
	
	/**
	 * Output transformed data
	 */
	private CLBuffer<?> d_pinnedOutput = null;
	
	/**
	 * Simplified options for transforms along axis
	 * 
	 * @author Austin McElroy
	 * @version 0.1.0
	 */
	enum AXIS {X, Y, Z};
	
	/**
	 * Creates a new instance. This class covers the following transforms:<br>
	 * 1.) Rotate about the X-Axis<br>
	 * 2.) Rotate about Y-Axis<br>
	 * 3.) Rotate about the Z-Axis<br>
	 * @param OpenCLContext Created context
	 * @param PathToProgram Path to OpenCL .cl file that contains the functions
	 */
	public TransformVolumeCLProgram(UTOpenCL ocl, InputStream is) {
		super(ocl, is);
	}
	
	/**
	 * 
	 * @param bb Byte buffer to load onto the OpenCL device
	 */
	public byte[] rotate(byte[] b, float theta, float phi, float gamma){
		d_pinnedInput = ocl.getContext().createByteBuffer(b.length, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		ByteBuffer h_pinnedInput = ocl.getQueue().putMapBuffer(d_pinnedInput, Map.READ_WRITE, true);
		
		d_pinnedOutput = ocl.getContext().createByteBuffer(b.length, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		ByteBuffer h_pinnedOutput = ocl.getQueue().putMapBuffer(d_pinnedOutput, Map.READ_WRITE, true);
		
		h_pinnedInput.put(b);
		
		ocl.getQueue().finish();
		
		CLKernel rotate_kernel = this.program.createCLKernel("rotate_u8");
		rotate_kernel.setArg(0, d_pinnedInput);
		rotate_kernel.setArg(1, theta);
		rotate_kernel.setArg(2, phi);
		rotate_kernel.setArg(3, gamma);
		rotate_kernel.setArg(4, d_pinnedOutput);
		
		ocl.getQueue().put3DRangeKernel(rotate_kernel, 0, 0, 0, 
													   10, 10, 10,
													   0, 0, 0);
		
		ocl.getQueue().putReadBuffer(d_pinnedOutput, true);
		
		ocl.getQueue().finish();
		
		
		
		byte[] output = new byte[h_pinnedOutput.capacity()];
		h_pinnedOutput.get(output);
		
		return output;
	}

}
