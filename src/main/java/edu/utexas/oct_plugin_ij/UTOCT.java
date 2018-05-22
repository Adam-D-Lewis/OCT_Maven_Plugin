package edu.utexas.oct_plugin_ij;

import ij.IJ;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.jtransforms.fft.FloatFFT_1D;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

/**
 * Class that parses UT OCT style OCT datasets.
 * <p>
 * 
 * 
 * @author Austin McElroy
 * @version 0.1.0
 */
public class UTOCT {	
	private RandomAccessFile raf;
	private String datasetName;
	private int pointsPerAScan;
	private int aScansPerBScan;
	private int numberOfBScans;
	private float voltageRange;
	private int bytesPerSample;
	private int bScanSizeBytes;
	private float bias;
	private float gain;
	private String Name = null;
	
	ForkJoinPool pool;
	
	private List<Float[]> window = new ArrayList<Float[]>();
	private byte[] unprocessedBuffer;
	
	private float[] Real;
	private float[] Imag;
	
	private ImageStack FFTResults;

	/**
	 * Closes the OCT volume
	 * 
	 * @throws IOException Throws an exception if the OCT volume can't be closed
	 */
	public void close() throws IOException{
		raf.close();
		window = null;
		unprocessedBuffer = null;
	}
	
	/**
	 * Opens the .oct_scan file and prepares the data for processing 
	 * 
	 * @param pathToParameters Path to the .oct_scan file
	 * @throws IOException Error if the file can't be read
	 */
	public void open(String pathToParameters) throws IOException{
		int pointsPerAScan = -1;
		int ascansPerBScan = -1;
		int bscans = -1;
		int bitsPerSample = -1;
		int startTrim = 0;
		int stopTrim = 0;
		
		File f = new File(pathToParameters);
		
		List<String> lines = Files.readAllLines(f.toPath());
		for(String line : lines){
			if(line.contains("[") || line.contains("]")){
				
			}else{				
				String[] splitLine = line.split("=");				
				
				if(splitLine.length == 2){					
					String key = splitLine[0].trim().toLowerCase();
					String value = splitLine[1].trim();
					
					switch(key){
						case "range":
							this.voltageRange = Float.valueOf(value);
							break;
							
						case "bits":
							bitsPerSample = Float.valueOf(value).intValue();						
							break;
							
						case "points per a-scan":
							pointsPerAScan = Integer.valueOf(value);
							break;
							
						case "a-scans per b-scan":
							ascansPerBScan = Integer.valueOf(value);
							break;
						
						case "b-scans":
							bscans = Integer.valueOf(value);
							break;
							
						case "name":
							Name = value;
							Name = Name.replaceAll("\"", "");
							break;
						
						case "stop trim":
							stopTrim = Integer.valueOf(value);
							break;
							
						case "start trim":
							startTrim = Integer.valueOf(value);
							break;
							
						default:
							System.out.println(line);
							break;
					}
				}				
			}			
		}	
		
		bias = voltageRange / 2;
		gain = (float) (voltageRange / (Math.pow(2, bitsPerSample) - 1));
		this.bytesPerSample = (int) Math.ceil(bitsPerSample/8);
		
		if(pointsPerAScan == -1 || ascansPerBScan == -1 || bscans == -1 || bitsPerSample == -1){
			throw new IOException();
		}
		
		ascansPerBScan = ascansPerBScan - startTrim - stopTrim;
		
		setScan(pointsPerAScan, ascansPerBScan, bscans);
		
		String osName = System.getProperty("os.name");
		boolean mac = osName.contains("Mac");
		boolean linux = osName.contains("Linux");
		boolean windows = osName.contains("Windows");
		if(mac || linux) {
			datasetName = pathToParameters.substring(0, pathToParameters.lastIndexOf("/"));
			
			if(Name == null){
				datasetName += "/data.bin";			
			}else{
				datasetName += "/" + Name;
			}
		}
		if(windows) {
			datasetName = pathToParameters.substring(0, pathToParameters.lastIndexOf("\\"));
			
			if(Name == null){
				datasetName += "\\data.bin";			
			}else{
				datasetName += "\\" + Name;
			}
		}
		
		_openDataset(datasetName);		
	}
	
	/**
	 * Opens an OCT volume
	 * 
	 * @param Filename Filename of the Volume
	 * @throws FileNotFoundException
	 * @throws SecurityException
	 */
	private void _openDataset(String Filename) throws FileNotFoundException, SecurityException{
		datasetName = Filename;
		raf = new RandomAccessFile(datasetName, "r");
	}
	
	/**
	 * 
	 * @return Number of B-Scans in the volume
	 */
	public int getNumberOfBScans(){
		return numberOfBScans;
	}
	
	/**
	 *	 *
	 * @return The number of points in an A-Scan
	 */
	public int getPointsPerAScan(){
		return pointsPerAScan;
	}
	
	/**
	 * Computes N number of windows with a percentage overlap, used for Multi-Spectral OCT analysis
	 * 
	 * @param Windows
	 * @param percentOverlap
	 */
	public void setMultispectralWindows(int Windows, float percentOverlap){
		int WindowSize = 0;
		
		window.clear();
		
		if(percentOverlap == 1){
			percentOverlap = .99f;
		}

		WindowSize = (int) Math.floor(pointsPerAScan / (Windows + 1));
		WindowSize /= (1 - percentOverlap); 
		int WindowSPacing = (int) Math.floor(pointsPerAScan / (Windows + 1));
		
		Float[] h = HanningWindow(WindowSize);
		for(int i = 0; i < Windows; i++){
			Float[] w = new Float[pointsPerAScan];	
			Float zero = new Float(0);
			Arrays.fill(w, zero);
			for(int j = 0; j < h.length; j++){
				w[i*WindowSPacing + j] = h[j];
			}
			window.add(w);
		}	
	}
	
	/**
	 * Sets the parameters of the OCT Volume. Should be called before processVolume()
	 * 
	 * @param pointsPerAScan Points per A-Scan, NOT in bytes
	 * @param ascansPerBScan A-Scans per B-Scan
	 * @param numerOfBScans Total number of B-Scans
	 */
	protected void setScan(int pointsPerAScan, int ascansPerBScan, int numerOfBScans){
		this.pointsPerAScan = pointsPerAScan;
		this.aScansPerBScan = ascansPerBScan;
		this.numberOfBScans = numerOfBScans;
		
		bScanSizeBytes = bytesPerSample*pointsPerAScan*aScansPerBScan;
		
		unprocessedBuffer = new byte[bScanSizeBytes];
		
		window.add(HanningWindow(pointsPerAScan));
		
		int numberOfProcessors = Runtime.getRuntime().availableProcessors(); //ADL
		pool = new ForkJoinPool(numberOfProcessors);
	}
	
	/**
	 * Creates a Hanning window of length size
	 * 
	 * @param size Length of the Hanning window
	 * @return float[] Array of data containing the window
	 */
	public static Float[] HanningWindow(int size){
		Float[] window = new Float[size];
		
		for(int i = 0; i < size; i++){
			window[i] = (float)(.5f*(1 - Math.cos(2*Math.PI*i / (size - 1))));
		}
		
		return window;
	}
	
	public ImageStack getFFTResults(){
		return FFTResults;
	}
	
	/**
	 * Creates an ImageStack with B-Scans wrapped in a FloatProcessor
	 * 
	 * @param //ScaleFactor to rescale the output size
	 * @return ImageStack that contains each B-Scan wrapped in a FloatProcessor
	 * @throws IOException
	 * @see ImageStack
	 */
	public ImageStack processVolume(boolean exportRealImag, int interleaveNum) throws IOException{
		int pointsPerInterleavedAScan = pointsPerAScan/interleaveNum;
		ImageStack is = new ImageStack(pointsPerInterleavedAScan/2, aScansPerBScan);

		FFTResults = null;
		
		if(exportRealImag){
			FFTResults = new ImageStack(pointsPerInterleavedAScan/2, aScansPerBScan);
		}
		
		ArrayList<ComputeBScan> cbsList = new ArrayList<ComputeBScan>();
		
		ExecutorService es = Executors.newFixedThreadPool(8); //ADL
		
		for(int i = 0; i < numberOfBScans; i++){
			for(int j = 0; j < window.size(); j++){
				Float[] currentWindow = window.get(j);			
				
				ComputeBScan cbs = new ComputeBScan(raf, pointsPerAScan, aScansPerBScan, bytesPerSample, i, gain, bias, currentWindow, new FloatFFT_1D(pointsPerInterleavedAScan), interleaveNum);
				cbsList.add(cbs);
				es.submit(cbs);
			}
		}
		
		es.shutdown();
		try {
			es.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(ComputeBScan cbs : cbsList){
			ImageProcessor ip = new FloatProcessor(pointsPerInterleavedAScan/2, aScansPerBScan, cbs.getBScan());
			
			if(exportRealImag){
				FFTResults.addSlice(new FloatProcessor(pointsPerInterleavedAScan/2, aScansPerBScan, cbs.getReal()));
				FFTResults.addSlice(new FloatProcessor(pointsPerInterleavedAScan/2, aScansPerBScan, cbs.getImag()));
			}
			
			is.addSlice(ip);		
		}
		
		cbsList.clear();
		
		StackProcessor temp_sp = new StackProcessor(is);	
		is = temp_sp.rotateRight();
		
		if(exportRealImag){
			temp_sp = new StackProcessor(FFTResults);
			FFTResults = temp_sp.rotateRight();
		}
		
		return is;
	}
	
	public List<Float[]> getWindows(){
		return window;
	}

//	private float[] processBscan(int BScanIndex, int WavelengthWindow, boolean exportFFTRealImag) throws IOException{		
//		float[] processedBuffer = new float[pointsPerAScan*aScansPerBScan/2];
//		Real = null; 
//		Imag = null; 
//		
//		if(exportFFTRealImag){
//			Real = new float[pointsPerAScan*aScansPerBScan/2];
//			Imag = new float[pointsPerAScan*aScansPerBScan/2];
//		}
//		
////		long BytesInVolume = (long)pointsPerAScan*(long)aScansPerBScan*(long)bytesPerSample;
//		
////		raf.seek(0);
////		raf.seek((long)bScanSizeBytes*BScanIndex);
////		raf.read(unprocessedBuffer);
//		
//		Float[] ffff = window.get(WavelengthWindow);
//
//		pool.invoke(new ComputeBScan(raf, (long)bScanSizeBytes*BScanIndex, (long)bScanSizeBytes*BScanIndex + bScanSizeBytes, pointsPerAScan, (byte)bytesPerSample, new FloatFFT_1D(pointsPerAScan), gain, bias, window.get(WavelengthWindow), processedBuffer, Real, Imag));
//		
//		return processedBuffer;
//	}
}
