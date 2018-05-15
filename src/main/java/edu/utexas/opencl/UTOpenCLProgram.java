package edu.utexas.opencl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;

/**
 * Convenience class that wraps CLProgram. Each .cl file that a user needs should be sub-classed so that 
 * device memory and work flow can be implemented as needed.
 * 
 * @author Austin McELroy
 * @version 0.1.0
 */
public class UTOpenCLProgram {
	/**
	 * OpenCL program
	 */
	protected CLProgram program;
	
	/**
	 * Compiled kernels from the program
	 */
	protected Map<String, CLKernel> kernels;
	
	/**
	 * OpenCL class
	 */
	protected UTOpenCL ocl;
	
	/**
	 * Message to log
	 */
	protected String msg;
	
	/**
	 * Flag whether the program successfully compiled or not
	 */
	private boolean programCompiled = false;
	
	/**
	 * Loads a program with the given CLContext
	 * 
	 * @param OpenCLContext OpenCL Context
	 * @param PathToProgram Path to the OpenCL program file that contains 1 or more kernels
	 */
	public UTOpenCLProgram(UTOpenCL ocl, InputStream IS){
		this.ocl = ocl;
		try {	
			long filesize = IS.available();
			byte[] fileBuffer = new byte[(int) filesize];
			IS.read(fileBuffer);		
			String src = new String(fileBuffer, "UTF-8");
			program = ocl.getContext().createProgram(src);
			program.build("-cl-denorms-are-zero -cl-finite-math-only -cl-mad-enable -cl-no-signed-zeros");
			programCompiled = program.isExecutable();
			System.out.print(program.getBuildLog());
			kernels = program.createCLKernels();
			IS.close();
		} catch (IOException e) {

		}	
	}
	
	public UTOpenCLProgram(UTOpenCL ocl, List<InputStream> ISList){
		this.ocl = ocl;
		try {	
			for(InputStream IS : ISList){
				long filesize = IS.available();
				byte[] fileBuffer = new byte[(int) filesize];
				IS.read(fileBuffer);		
				String src = new String(fileBuffer, "UTF-8");
				program = ocl.getContext().createProgram(src);
				program.build("-cl-denorms-are-zero -cl-finite-math-only -cl-mad-enable -cl-no-signed-zeros");
				programCompiled = program.isExecutable();
				System.out.print(program.getBuildLog());
				Map<String, CLKernel> m = program.createCLKernels();
				if(kernels == null){
					kernels = m;
				}else{
					kernels.putAll(m);
				}
				IS.close();
			}
		} catch (IOException e) {

		}	
	}
	
	/**
	 * 
	 * @return ProgramBuildResults
	 * @see ProgramBuildResults()
	 */
	public ProgramBuildResults getBuildResults(){
		return new ProgramBuildResults(programCompiled, program.getBuildLog());
	}
	
	public void log(String msg){
		this.msg = msg;
		System.out.print(msg);
	}
	
	public String getMsg(){
		return msg;
	}
}
