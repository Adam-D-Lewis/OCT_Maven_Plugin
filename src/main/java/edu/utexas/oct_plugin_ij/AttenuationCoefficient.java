package edu.utexas.oct_plugin_ij;

public class AttenuationCoefficient {

	public AttenuationCoefficient(){
		
	}
	
	public float[] run(float[] image, int width, int height, boolean in_dB, float ZFocalDeptPixels, float RRangeInmm, float MMPerPixel){
		float[] atten = new float[image.length];
		
		float zr = (float)(4*Math.pow(RRangeInmm, 2));
		float z0 = (float)(ZFocalDeptPixels);
		
		for(int x = 0; x < width; x++){
			float sum = 0;
			for(int y = height - 1; y > 1; y--){
				float pix = 0;
				float pix_prior = 0;
				if(in_dB){
					pix = (float)Math.pow(10, (image[y*width + x]/20));
					pix_prior = (float)Math.pow(10, (image[(y - 1)*width + x]/20));
				}else{
					pix = image[y*width + x];
					pix_prior = image[(y - 1)*width + x];
				}
				sum += pix;		
				
				if(ZFocalDeptPixels == 0 && RRangeInmm <= 0 && MMPerPixel <= 0){
					atten[(y - 1)*width + x] = pix_prior / (2*sum);
				}else if(ZFocalDeptPixels == 0 && RRangeInmm <= 0){
					atten[(y - 1)*width + x] = pix_prior / (2*sum*MMPerPixel);
				}else{
					float z = y;
					atten[(y - 1)*width + x] = pix_prior / (2*sum*MMPerPixel);
					atten[(y - 1)*width + x] += (z0 - z)*MMPerPixel / ((z0 - z)*(z0 - z)*MMPerPixel*MMPerPixel + zr);
				}
				
				if(atten[(y - 1)*width + x] < 0){
					atten[(y - 1)*width + x] = 0;
				}
			}
		}
		return atten;
	}
}
