package edu.utexas.opencl;

/**
 * Simple class to encapsulate OpenCL CLProgram results
 * 
 * @author Austin McElroy
 * @version 0.1.0
 */
public class ProgramBuildResults {
	/**
	 * Whether or not the program compiled successfully or now
	 */
	private boolean success;
	
	/**
	 * Results of the build
	 */
	private String buildResults;
	
	public ProgramBuildResults(boolean success, String results){
			this.success = success;
			buildResults = results;
	}
	
	/**
	 * 
	 * @return Build succeeded or failed
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * 
	 * @return Build results as String
	 */
	public String getBuildResults() {
		return buildResults;
	}
}
