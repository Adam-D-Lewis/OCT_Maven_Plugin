package edu.utexas.imageprocessing;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLEvent.ProfilingCommand;
import com.jogamp.opencl.CLEventList;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.llb.CL;

import edu.utexas.opencl.UTOpenCL;
import edu.utexas.opencl.UTOpenCLProgram;

public class GLCM extends UTOpenCLProgram{

	List<Integer> XOffset;
	List<Integer> YOffset;
	
	CLBuffer<ByteBuffer> inputBuffer;
	
	CLBuffer<FloatBuffer> Contrast;
	CLBuffer<FloatBuffer> Energy;
	CLBuffer<FloatBuffer> Correlation;
	CLBuffer<FloatBuffer> Homogeneity;
	CLBuffer<FloatBuffer> Entropy;
	CLBuffer<FloatBuffer> MaxProb;
	CLBuffer<FloatBuffer> ClusterShade;
	CLBuffer<FloatBuffer> ClusterProm;
	
	public enum GLCM_TYPE {CONTRAST, ENERGY, CORRELATION, HOMOGENEITY, ENTROPY, MAXPROB, CLUSTERSHADE, CLUSTERPROM};
	
	boolean b_FirstRun;
	
	int i_Width;
	int i_Height;
	int i_GLCMWidth;
	int i_GLCMHeight;
	int i_KernelX;
	int i_KernelY;
	double d_Time;
	
	public GLCM(UTOpenCL ocl, List<InputStream> isList) {
		super(ocl, isList);	
		b_FirstRun = true;
	}
	
	public void disposeBuffers(){
		if(Contrast != null){
			Contrast.release();
		}

		if(Energy != null){
			Energy.release();
		}
		
		if(Correlation != null){
			Correlation.release();
		}

		if(Homogeneity != null){
			Homogeneity.release();
		}

		if(Entropy != null){
			Entropy.release();
		}

		if(MaxProb != null){
			MaxProb.release();
		}
	
		if(ClusterShade != null){
			ClusterShade.release();
		}

		if(ClusterProm != null){
			ClusterProm.release();
		}

		if(inputBuffer != null){
			inputBuffer.release();
		}
		
	}
	
	public float[] getGLCM(GLCM_TYPE t){
		float[] f = new float[i_Width*i_Height];
		CLBuffer<FloatBuffer> fb;
		
		switch(t){
		case CLUSTERPROM:
			fb = ClusterProm;
			break;
		case CLUSTERSHADE:
			fb = ClusterShade;
			break;
		case CONTRAST:
			fb = Contrast;
			break;
		case CORRELATION:
			fb = Correlation;
			break;
		case ENERGY:
			fb = Energy;
			break;
		case ENTROPY:
			fb = Entropy;
			break;
		case HOMOGENEITY:
			fb = Homogeneity;
			break;
		case MAXPROB:
			fb = MaxProb;
			break;
		default:
			fb = Contrast;
			break;
		
		}
		
		ocl.getQueue().putReadBuffer(fb, true);
		fb.getBuffer().rewind();
		fb.getBuffer().get(f);
		fb.getBuffer().rewind();
		
		return f;
	}
	
	public int getGLCMWidth(){
		return i_GLCMWidth;
	}
	
	public int getGLCMHeight(){
		return i_GLCMHeight;
	}
	
	public List<Integer> getXOffsetList(){
		return XOffset;
	}
	
	/***
	 * Returns the X Offset indecies as an array
	 * 
	 * @return
	 */
	public int[] getXOffsetArray(){
		int[] tmp = new int[XOffset.size()];
		int ii = 0;
		for(Integer i : XOffset){
			tmp[ii] = i.intValue();
			ii+=1;
		}
		return tmp;
	}
	
	/***
	 * Returns the Y Offset indecies as an array
	 * 
	 * @return
	 */
	public int[] getYOffsetArray(){
		int[] tmp = new int[YOffset.size()];
		int ii = 0;
		for(Integer i : YOffset){
			tmp[ii] = i.intValue();
			ii+=1;
		}
		return tmp;
	}
	
	protected void zeroFloatBuffer(CLBuffer<FloatBuffer> b, int Width, int Height){
		float[] f = new float[Width*Height];
		b.getBuffer().rewind();
		b.getBuffer().put(f).rewind();
		
		ocl.getQueue().putWriteBuffer(b, false);
	}
	
	protected void allocateMemory(int Width, int Height){
		i_Width = Width;
		i_Height = Height;
		
		disposeBuffers();
		
		Contrast = ocl.getContext().createFloatBuffer(Width*Height, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		Energy = ocl.getContext().createFloatBuffer(Width*Height, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		Correlation = ocl.getContext().createFloatBuffer(Width*Height, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		Homogeneity = ocl.getContext().createFloatBuffer(Width*Height, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		Entropy = ocl.getContext().createFloatBuffer(Width*Height, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		MaxProb = ocl.getContext().createFloatBuffer(Width*Height, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		ClusterShade = ocl.getContext().createFloatBuffer(Width*Height, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		ClusterProm = ocl.getContext().createFloatBuffer(Width*Height, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		inputBuffer = ocl.getContext().createByteBuffer(Width*Height, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		
		zeroFloatBuffer(Contrast, Width, Height);
		zeroFloatBuffer(Energy, Width, Height);
		zeroFloatBuffer(Correlation, Width, Height);
		zeroFloatBuffer(Homogeneity, Width, Height);
		zeroFloatBuffer(Entropy, Width, Height);
		zeroFloatBuffer(MaxProb, Width, Height);
		zeroFloatBuffer(ClusterShade, Width, Height);
		zeroFloatBuffer(ClusterProm, Width, Height);
		
	}
	
	public void glcm(byte[] Image, int Width, int Height, int kernelX, int kernelY, int[] XOffset, int[] YOffset){
		CLKernel k = kernels.get("TextureAnalysis_u8");
		
		i_KernelX = kernelX;
		i_KernelY = kernelY;
		
		if(b_FirstRun){
			allocateMemory(Width, Height);
			b_FirstRun = false;
		}
		
		if(i_Width != Width || i_Height != Height){
			allocateMemory(Width, Height);
		}
		
		i_GLCMHeight = i_Height - 2*kernelX + 1;
		i_GLCMWidth = i_Width - 2*kernelY + 1;
		
		CLBuffer<IntBuffer> xoffsetclbuffer = ocl.getContext().createIntBuffer(Integer.BYTES*XOffset.length, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		xoffsetclbuffer.getBuffer().rewind();
		xoffsetclbuffer.getBuffer().put(XOffset);
		xoffsetclbuffer.getBuffer().rewind();
		
		CLBuffer<IntBuffer> yoffsetclbuffer = ocl.getContext().createIntBuffer(Integer.BYTES*YOffset.length, Mem.ALLOCATE_BUFFER, Mem.READ_WRITE);
		yoffsetclbuffer.getBuffer().rewind();
		yoffsetclbuffer.getBuffer().put(YOffset);
		yoffsetclbuffer.getBuffer().rewind();
		
		CL cl = CLPlatform.getLowLevelCLInterface();
		
		k.setArg(0, inputBuffer);
		k.setArg(1, kernelX);
		k.setArg(2, kernelY);
		k.setArg(3, XOffset.length);
		k.setArg(4, xoffsetclbuffer);
		k.setArg(5, yoffsetclbuffer);
		k.setArg(6, Contrast);
		k.setArg(7, Energy);
		k.setArg(8, Correlation);
		k.setArg(9, Homogeneity);
		k.setArg(10, Entropy);
		k.setArg(11, MaxProb);
		k.setArg(12, ClusterShade);
		k.setArg(13, ClusterProm);
		
		CLEventList clel = new CLEventList(1); 
		
		inputBuffer.getBuffer().rewind();
		inputBuffer.getBuffer().put(Image);
		inputBuffer.getBuffer().rewind();
		ocl.getQueue().putWriteBuffer(inputBuffer, true);
		ocl.getQueue().putWriteBuffer(xoffsetclbuffer, true);
		ocl.getQueue().putWriteBuffer(yoffsetclbuffer, true);
		ocl.getQueue().put3DRangeKernel(k, 0, 0, 0, 1, Height, Width, 0, 0, 0, clel);
		
		
		ocl.getQueue().finish();
		
		d_Time = (clel.getEvent(0).getProfilingInfo(ProfilingCommand.END) - clel.getEvent(0).getProfilingInfo(ProfilingCommand.START)) / 1000000000.0;
		System.out.print("GLCM Time: " + d_Time + "\r\n");		

		xoffsetclbuffer.release();
		yoffsetclbuffer.release();
	}
	
	public ArrayList<GLCMResult> getGLCMResults(String Info){
		ArrayList<GLCMResult> tmp = new ArrayList<GLCMResult>();
		
		for(int l = 0; l < GLCM_TYPE.values().length; l++){
			String label = Info + " Type: " + GLCM_TYPE.values()[l].toString() + " X,Y: " + i_KernelX + "," + i_KernelY + " Offsets(X,Y): ";
			for(int k = 0; k < XOffset.size(); k++){
				label += XOffset.get(k) + "," + YOffset.get(k) + " ";
			}
			
			GLCMResult g = new GLCMResult(label, getGLCM(GLCM_TYPE.values()[l]), i_Width, i_Height); 
			tmp.add(g);
		}
		
		return tmp;
	}

	/***
	 * Creates an array of X,Y offsets, for example, 1,1, -1,1, etc which are used to when computing the GLCM.	 * 
	 * 
	 * @param pixelDistance Distance in, in pixels, to differentiate when computing the GLCM, only positive values will be used.
	 * @param pixelTheta Angle used to compute X,Y offset, valid values are 0 <= angle < Pi  
	 */
	public void createXYOffset(List<Integer> pixelDistance, List<Double> pixelTheta){
		XOffset = new ArrayList<Integer>();
		YOffset = new ArrayList<Integer>();
		for(Integer d : pixelDistance){
			for(Double a: pixelTheta){
				int mult = 1;
				double tmp_a = a;
				double tmp_d = (double)d;
				if(a > 90){
					mult = -1;
					tmp_a = (float) (180 - a);
				}
				
				Integer X = (int)(Math.round(tmp_d*Math.cos(tmp_a*Math.PI/180)))*mult;
				Integer Y = (int)(Math.round(tmp_d*Math.sin(tmp_a*Math.PI/180)));
				XOffset.add(X);
				YOffset.add(Y);
			}
		}
	}
}
