package edu.utexas.math;

import java.util.ArrayList;
import java.util.List;

public class Math1D {
	public static float[] differentiate(float[] in, int distance){
		float[] out = new float[in.length];
		
		for(int i = distance; i < in.length - 1 - distance; i++){
			float prev = in[i] - in[i - distance];
			float next = in[i + distance] - in[i];
			out[i - distance] = (prev + next)/2;
		}
		
		return out;
	}
	
	public static float[] zeroCrossing(float[] in){
		List<Float> zc = new ArrayList<Float>();
		
		for(int i = 0; i < in.length - 1; i++){
			if(in[i]*in[i + 1] < 0){
				Float f = (float)((2*i + 1)/2);
				zc.add((float) (i));
			}
		}		
		
		float[] zcArr = new float[zc.size()];
		for(int i = 0; i < zc.size(); i++){
			zcArr[i] = zc.get(i);
		}
		return zcArr;
	}
	
	public static float[] invert(float[] in){
		float max = Float.MIN_NORMAL;
		for(int i = 0; i < in.length; i++){
			if(in[i] > max){
				max = in[i];
			}
		}
		
		float[] out = new float[in.length];
		for(int i = 0; i < in.length; i++){
			out[i] = max - in[i];
		}
		
		return out;
	}
}
