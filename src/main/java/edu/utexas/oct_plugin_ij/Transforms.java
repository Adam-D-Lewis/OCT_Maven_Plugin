package edu.utexas.oct_plugin_ij;

/**
 * A library of functions to perform image transforms useful to OCT
 * 
 * @author Austin McElroy
 * @version 0.1.0
 */

public class Transforms {	
	/**
	 * Converts an image from rectangular to polar coordinates using nearest neighbor
	 * 
	 * @param input Frame to convert from rectangular coordinates to polar coordinates
	 * @param width Width of the frame
	 * @param height Height of the frame
	 * @return
	 */
	static public byte[] RectToPolar(byte[] input, int width, int height){
		int radius = height;
		int diameter = radius*2;
		
		byte[] output = new byte[radius*radius*4];
		
		//input array in imagej format is <T>[x][y]
		
		for(int x = -radius; x < radius; x++){
			for(int y = -radius; y < radius; y++){
				float radius_squared = x*x + y*y;
				if(radius_squared >= height*height){
					//do nothing
				}else{
					float r = (float) (Math.floor(Math.sqrt(radius_squared)));
					float theta = (float)((Math.atan2(x, y) + Math.PI)*width/(2*Math.PI));
					
					int offset = (int) (width*r + theta);
					if(offset < input.length){					
						output[(x + radius)*diameter + (y + radius)] = input[offset];
					}
				}
				
			}			
		}
		
		return output;
	}	
}
