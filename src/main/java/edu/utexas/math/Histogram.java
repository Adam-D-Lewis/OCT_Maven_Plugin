package edu.utexas.math;

import java.util.Arrays;

public class Histogram {

	int numerOfBins = 128;
	double binSize = 65535 / numerOfBins;
	double[] count;
	double[] bins;
	
	double entropy = 0;
	double totalCount = 0;
	double median = 0;
	
	public Histogram(float[] a, float exclude){
		float[] b = new float[a.length];
		
		System.arraycopy(a, 0, b, 0, a.length);
		
		Arrays.sort(b);
		
		bins = new double[numerOfBins];
		count = new double[numerOfBins];
		
		Double max = Double.NEGATIVE_INFINITY;
		
		double minWithExclude = Double.MAX_VALUE;		
		
		for(float t : b){
			Double d = (double) t;
			
			try{
				if(t != exclude){
					count[(int)(t/binSize) - 1] += 1;				
				}
			}catch(Exception e){
				//System.out.println(e.getMessage());
			}
		}
		
		
		totalCount = 0;
		for(Double d : count){
			totalCount += d;
		}
		
		
		
		for(Double d : count){
			entropy += d*Math.log10(d/totalCount)/totalCount;
		}
		
		median = 0;
		float cumulativeFreq = 0;
		int medianBin = 0;
		for(int x = 0; x < count.length; x++){
			cumulativeFreq += count[x];
			if(cumulativeFreq >= totalCount/2){
				medianBin = x;
				break;
			}
		}
		
		//Plot p = new Plot("","","", getBinsArray(), getCountArray());
		//p.show();
		
		median = medianBin*binSize;
		entropy *= -1;
	}
	
	public double[] getCount(){
		return count;
	}
	
	public double getEntropy(){
		return entropy;
	}
	
	public double getMedian(){
		return median;
	}
	
	public double[] getBinsList(){
		return bins;
	}
	
	public double[] getCountList(){
		return count;
	}
}
