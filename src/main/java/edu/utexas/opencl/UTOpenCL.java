package edu.utexas.opencl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLCommandQueue.Mode;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.concurrent.CLQueueContext;

/**
 * Convenience class that wraps com.jogamp.opencl
 * 
 * @author Austin McElroy
 * @version 0.1.0
 * @see com.jogamp.opencl
 */
public class UTOpenCL {
	/**
	 * OpenCL Context
	 */
	private CLContext context = null;
	
	/**
	 * OpenCL device, from context
	 */
	private CLDevice device = null;
	
	/**
	 * OpenCL command queue for the selected device
	 */
	private CLCommandQueue queue = null;
	
	/**
	 * List of successfully compiled programs
	 */
	private List<UTOpenCLProgram> listOfCompiledPrograms;

	
	/**
	 * Creates a new OpenCL class with a user specified device, fastest device<br>
	 * of the device type will be chosen. 
	 * 
	 * @param deviceType Type of device to create the context for
	 */
	public UTOpenCL(Type deviceType){
		
		CLPlatform[] platList = CLPlatform.listCLPlatforms();

		CLDevice fastestDevice = null;
		
		for(int i = 0; i < platList.length; i++){
			CLDevice[] devList = platList[i].listCLDevices();
			for(int j = 0; j < devList.length; j++){
				if(devList[j].getType() == deviceType){
					if(fastestDevice == null){
						fastestDevice = devList[j];
					}					
					
					if(devList[j].getMaxComputeUnits()*devList[j].getMaxClockFrequency() > fastestDevice.getMaxClockFrequency()*fastestDevice.getMaxComputeUnits() 
							|| devList[j].getName().toLowerCase().contains("amd") || devList[j].getName().toLowerCase().contains("nvidia")){
						fastestDevice = devList[j];
					}
				}
			}
		}		
		
		context = CLContext.create(fastestDevice.getPlatform(), deviceType);
		for(CLDevice d : context.getDevices()){
			if(d.getID() == fastestDevice.getID()){
				device = d;
			}
		}
		queue = device.createCommandQueue(Mode.PROFILING_MODE);
	}	
	
	/**
	 *  Releases OpenCL devices and the context
	 */
	public void release(){
		context.release();
	}
	
	public ProgramBuildResults loadProgram(InputStream IS){
		if(listOfCompiledPrograms == null){
			listOfCompiledPrograms = new ArrayList<UTOpenCLProgram>();
		}
		
		UTOpenCLProgram p = new UTOpenCLProgram(this, IS);
		
		if(p.getBuildResults().isSuccess()){
			listOfCompiledPrograms.add(p);
		}
		
		return p.getBuildResults();
	}

	public CLContext getContext() {
		return context;
	}

	public CLDevice getDevice() {
		return device;
	}

	public CLCommandQueue getQueue() {
		return queue;
	}
	
	public List<UTOpenCLProgram> getProgramList(){
		return listOfCompiledPrograms;
	}
	
}
