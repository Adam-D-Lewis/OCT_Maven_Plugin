package edu.utexas.opencl;

import java.nio.FloatBuffer;
import java.util.List;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;

/*
 * Test class for edu.utexas.jogamp.jocl
 */
public class main {

	public static void main(String[] args) {
		UTOpenCL ocl = new UTOpenCL(Type.GPU);
		
		int x = 512;
		int y = 512;
		
		ProgramBuildResults pbr = ocl.loadProgram(ocl.getClass().getResourceAsStream("speckle_lm.cl"));
		
		float[] a = new float[x*y];
		float[] b = new float[x*y];
		
		for(int i = 0; i < x*y; i ++){
			a[i] = i;
			b[i] = x*y - i;
		}
		
		
		CLBuffer<FloatBuffer> A = ocl.getContext().createFloatBuffer(x*y, Mem.READ_WRITE, Mem.ALLOCATE_BUFFER);
		CLBuffer<FloatBuffer> B = ocl.getContext().createFloatBuffer(x*y, Mem.READ_WRITE, Mem.ALLOCATE_BUFFER);
		CLBuffer<FloatBuffer> C = ocl.getContext().createFloatBuffer(x*y, Mem.READ_WRITE, Mem.ALLOCATE_BUFFER);
		
		A.getBuffer().put(a)
					.rewind();
		
		B.getBuffer().put(b)
					.rewind();
		
		ocl.getQueue().putWriteBuffer(A, true);
		ocl.getQueue().putWriteBuffer(B, true);
		
		CLKernel k = ocl.getProgramList().get(0).kernels.get("speckle_lm");
		
		if(k == null)
			System.out.print("Error getting kernel add_32f");
		
		k.setArg(0, A);
		k.setArg(1, B);
		k.setArg(2, C);
		
		ocl.getQueue().put2DRangeKernel(k, 0, 0, x, y, 0, 0);
		
		ocl.getQueue().putReadBuffer(C, false);
		
		ocl.getQueue().finish();
		
		float[] c = new float[x*y];
		
		C.getBuffer().get(c)
					.rewind();
		
		ocl.release();
		
	}

}
