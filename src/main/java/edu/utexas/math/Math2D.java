package edu.utexas.math;

import java.util.List;

import edu.utexas.primitives.Tuples.Triplet;

public class Math2D {
	public static float[] integrate(float[] im, int width, int height){
		
		float[] shad = new float[width];
		float[] tmp = new float[width];
		
		for(int i = 0; i < height; i++){
			System.arraycopy(im, i*width, tmp, 0, width);
			for(int j = 0; j < width; j++){
				shad[j] += tmp[j]/(float)height;
			}
		}
		
		return shad;
	}
	
	public static float[] convolve(float[] image, float[] kernel){
		float[] convolved = new float[image.length];
		for(int i = 0; i < kernel.length; i++){
			convolved[i] = image[i]*kernel[i];
		}
		return convolved;
	}
	
	public static float[] getSubImage(float[] image, int width, int height, P2 p, int subWidth, int subHeight){		

		int w = subWidth / 2;
		int h = subHeight / 2;
		
		if(w < p.x() && p.x() < width - 1 - w && h < p.y() && p.y() < height - 1 - h){
			float[] sub = new float[subWidth*subHeight];
			for(int y = -h; y <= h; y++){
				for(int x = -w; x <= w; x++){
					sub[(y + h)*subWidth + x + w] = image[(int) ((p.y() + y)*width + p.x() + x)];
				}
			}
			return sub;
		}else{
			return null;
		}
	}
	
	public static float[] flatten(float[] pixels, 
			int width, 
			int height, 
			List<P2> points,
			int trimAmt){
		
		float[] out = new float[width*trimAmt];
		
		if(pixels == null){
			int x = 0;
		}
		
		points.parallelStream().forEach((p) -> {
			try{
				int ytrim = (int) Math.floor(p.y());
				int x = (int)Math.floor(p.x());							
				for(int y = ytrim; y < ytrim + trimAmt; y++){	
					if(p.x() >= 0 && p.y() >= 0 && p.x() < width && y < height){
						out[(y - ytrim)*width + x] = pixels[(y)*width + x];		
					}
				}	
			}catch(Exception e){
				e.printStackTrace();
			}
		});		
		
		return out;
	}
}
