package edu.utexas.segmentation;

import edu.utexas.math.Histogram;

public class RegionStatistics {

	private Region _region;
	
	private float mean_r1;
	private float std_r1;
	private float mean_r2;
	private float std_r2;
	
	private float[] r1;
	private float[] r2;
	
	Histogram hr1;
	Histogram hr2;
	
	private double diffEntropy;
	private double diffMean;
	private double diffStd;
	private double diffHistogramMedian;
	
	public RegionStatistics(Region r){
		try{
			this._region = r;
			r.bisect();
		
			r1 = r.getR1().clone();
			r2 = r.getR2().clone();
			
			mean_r1 = 0;
			mean_r2 = 0;
			
			float r1NonZero = 0;
			float r2NonZero = 0; 
			
			for(int i = 0; i < r1.length; i++){
				if(r1[i] != 0){
					mean_r1 += r1[i];
					r1NonZero += 1;
				}
			}
			
			for(int i = 0; i < r2.length; i++){				
				if(r2[i] != 0){
					mean_r2 += r2[i];
					r2NonZero += 1;
				}
			}
			
			mean_r1 /= r1NonZero;
			mean_r2 /= r2NonZero;
			
			if(r1NonZero == 0){
				hr1 = new Histogram(new float[] {1}, 0);
			}else{
				hr1 = new Histogram(r1, 0);
			}

			if(r2NonZero == 0){
				hr2 = new Histogram(new float[] {1}, 0);
			}else{
				hr2 = new Histogram(r2, 0);
			}
			
			for(int i = 0; i < r1.length; i++){
				if(r1[i] != 0){
					std_r1 = (float) Math.pow(r1[i] - mean_r1, 2);
				}
			}
			
			for(int i = 0; i < r2.length; i++){
				if(r2[i] != 0){
					std_r2 = (float) Math.pow(r2[i] - mean_r2, 2);
				}
			}
			
			std_r1 /= r1NonZero;
			std_r1 = (float)Math.sqrt(std_r1);
	
			std_r2 /= r2NonZero;
			std_r2 = (float)Math.sqrt(std_r2);
			
			diffEntropy = hr1.getEntropy() - hr2.getEntropy();
			diffMean = mean_r1 - mean_r2;
			diffStd = std_r1 - std_r2;
			diffHistogramMedian = hr1.getMedian() - hr2.getMedian();
		}catch(Exception e){
			e.getMessage();
		}
	}
	
	public double getDiffMedianR1R2(){
		return hr1.getMedian() - hr2.getMedian();
	}
	
	public double getDiffMedianR2R1(){
		return hr2.getMedian() - hr1.getMedian();
	}
	
	public double getDiffStdR1R2(){
		return std_r1 - std_r2;
	}
	
	public double[] getR1HistogramCount(){
		return hr1.getCount();
	}
	
	public double getDiffMeanR2R1(){
		return mean_r2 - mean_r1;
	}
	
	public double getDiffMeanR1R2(){
		return mean_r1 - mean_r2;
	}
	
	public double getHistogramDiffMean(){
		return diffHistogramMedian;
	}
	
	public double getDiffEntropyR1R2(){
		return hr1.getEntropy() - hr2.getEntropy();
	}
	
	public double getDiffEntropyR2R1(){
		return hr2.getEntropy() - hr1.getEntropy();
	}
	
	public Histogram getR1Histogram(){
		return hr1;
	}
	
	public Histogram getR2Histogram(){
		return hr2;
	}
	
	public double[] getR2HistogramCount(){
		return hr2.getCount();
	}
	
	public float getMeanR1(){
		return mean_r1;
	}
	
	public float getMeanR2(){
		return mean_r2;
	}
	
	public float getStdR1(){
		return std_r1;
	}
	
	public float getStdR2(){
		return std_r2;
	}
}
