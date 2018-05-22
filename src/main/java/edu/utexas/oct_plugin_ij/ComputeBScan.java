package edu.utexas.oct_plugin_ij;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.concurrent.RecursiveTask;

import org.jtransforms.fft.FloatFFT_1D;
/**
 * RecursiveTask that reduces the raw OCT volume into B-Scans which can be processed<br>
 * in parallel.<br> 
 * 
 * @see RecursiveTask<>()
 * 
 * @author Austin McElroy
 * @version 0.1.0
 */
@SuppressWarnings("serial")
public class ComputeBScan implements Runnable{		
	/**
	 * Raw B-Scan data read in as bytes. Needs to be converted to short
	 */
	private byte[] rawBScan;
	
	/**
	 * Output data as the Magnitude of Intensity
	 */
	private float[] output;
	
	private float[] real;
	
	private float[] imag;

	/**
	 * Bias of the analog to digital converter
	 */
	float bias;
	
	/**
	 * Gain of the analog to digital converter
	 */
	float gain;
	
	/**
	 * Window to multiply pre-FFT data to 
	 */
	Float[] window;
	
	/**
	 * Pre-FFT float buffer 
	 */
	float[] preFFTbuffer;
	
	/**
	 * Post-FFT buffer
	 */
	double[] postFFTbuffer;
	
	/**
	 * JTransform FFT Object
	 * 
	 * @see org.jtransforms.fft
	 */
	FloatFFT_1D fft;
	
	RandomAccessFile raf;

	private int pointsPerAScan;

	private int bytesPerSample;

	private int aScansPerBScan;

	private int currentBScan;

	private int interleaveNum;

	private int pointsPerInterleavedAScan;

	public ComputeBScan(RandomAccessFile Raf, int PointsPerAScan, int AScansPerBScan, int BytesPerSample, int CurrentBScan,
			float Gain, float Bias, Float[] Window, FloatFFT_1D FFT, int InterleaveNum){
		this.raf = Raf;
		gain = Gain;
		bias = Bias;
		window = Window;
		fft = FFT;
		pointsPerAScan = PointsPerAScan;
		bytesPerSample = BytesPerSample;
		aScansPerBScan = AScansPerBScan;
		currentBScan = CurrentBScan;
		interleaveNum = InterleaveNum;
		interleaveNum = InterleaveNum;
		pointsPerInterleavedAScan = pointsPerAScan/interleaveNum;
	}
	
	public Integer get(){
		return currentBScan;
	}

	public float[] getBScan(){
		return output;
	}
	
	public float[] getReal(){
		return real;
	}
	
	public float[] getImag(){
		return imag;
	}

	public void run(){	
		try{
			int PreSize = pointsPerInterleavedAScan; //whole B-scan of data
			int PostSize = pointsPerInterleavedAScan/2*aScansPerBScan;
			byte[] rawBScan = new byte[pointsPerAScan*aScansPerBScan*bytesPerSample];
			preFFTbuffer = new float[PreSize];
			postFFTbuffer = new double[PostSize];
			output = new float[PostSize];
			
			try {
				raf.seek((long)pointsPerAScan*aScansPerBScan*currentBScan*bytesPerSample);
				raf.read(rawBScan);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//k is the aScanNumber (in that B-scan)
			for(int k = 0; k < aScansPerBScan; k++){
				byte[] ascan = Arrays.copyOfRange(rawBScan, k*(pointsPerAScan*bytesPerSample), (k + 1)*pointsPerAScan*bytesPerSample);

				float sum = 0;
				int index = -1;
				for(long i = 0; i < pointsPerAScan; i++){
					float temp = (((int)ascan[(int)(bytesPerSample*i)] & 0xFF) << 8) + (((int)ascan[(int)(bytesPerSample*i + 1)] & 0xFF));
					temp = (temp*gain - bias)*window[(int)i];

					sum = sum + temp;
					if ((i + 1) % interleaveNum == 0) {
						index += 1;
						float ans = sum / interleaveNum;
						preFFTbuffer[index] = ans; //ADL
						sum = 0;
					}
				}
				
				fft.realForward(preFFTbuffer);

				int globalIndex = k * (pointsPerInterleavedAScan / 2);
				for (int j = 0; j < pointsPerInterleavedAScan / 2; j++) { //I'm convinced this is correct, j is the index within the A-scan
					postFFTbuffer[j] = (preFFTbuffer[2 * j] * preFFTbuffer[2 * j] + preFFTbuffer[2 * j + 1] * preFFTbuffer[2 * j + 1]); //get the magnitude from the complex number

					if (real != null && imag != null) {
						real[(int) (globalIndex + j)] = preFFTbuffer[2 * j];
						imag[(int) (globalIndex + j)] = preFFTbuffer[2 * j + 1];
					}

					postFFTbuffer[j] = 20 * Math.log10(Math.sqrt(postFFTbuffer[j]));
					output[(int) (globalIndex + j)] = (float) postFFTbuffer[j];
				}	
			}
		}catch(Exception e){
			e.printStackTrace();
			System.out.println(e);
		}
			
		
		preFFTbuffer = null;
		postFFTbuffer = null;
		rawBScan = null;
	}
	
//	/**
//	 * Reduces the OCT volume into B-Scans that can be computed in parallel.
//	 * <p>
//	 * @see ascanMagnitudeCompute()
//	 */
//	@Override
//	protected Float compute() {
//		long length = end - start;
//		if(length == reductionLimit){
//			ascanMagnitudeCompute();
//			return 0.0f;
//		}else if(length < reductionLimit){
//			return -1.0f;
//		}else{			
//			long numberOfAScans = length / reductionLimit;
//			long splitAScanIndex = (int) Math.floor(numberOfAScans / 2);
//			long split = splitAScanIndex*reductionLimit;
//
//			ComputeBScan l = new ComputeBScan(raf, start, start + split, reductionLimit, fft, gain, bias, window, output, real, imag);
//			ComputeBScan r = new ComputeBScan(raf, start + split, end, reductionLimit, fft, gain, bias, window, output, real, imag);
//			invokeAll(l, r);
//
//			return 1.0f;
//		}
//	}
}