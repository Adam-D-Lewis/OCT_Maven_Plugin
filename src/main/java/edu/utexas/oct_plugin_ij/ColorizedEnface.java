package edu.utexas.oct_plugin_ij;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.ContrastEnhancer;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ShortProcessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ColorizedEnface {
	private ImagePlus Volume;
	
	private int WindowSize = 11;
	
	private int WindowHalf = 5;
	
	private int LowThreshold = 2;
	
	private int HighThreshold = 30;
	
	private int ColorMapStart;
	
	private int ColorMapEnd;
	
	private boolean colorByDepth;
	
	ArrayList<int[]> ColorLUT = new ArrayList<int[]>();
	
	ImagePlus EnFaceLayers;
	
	public ColorizedEnface(ImagePlus volume){
		Volume = volume;
		GenericDialog gd = new GenericDialog("En Face Moving Window Options");
		gd.addNumericField("Window Size (odd preferred)", 11, 0);
		gd.addNumericField("Low Threshold", 2, 0);
		gd.addNumericField("High Threshold", 50, 0);
		gd.addCheckbox("Color by depth?", false);
		gd.addNumericField("Color Map Start Depth", Volume.getHeight()*.1, 0);
		gd.addNumericField("Color Map End Depth", Volume.getHeight()*.8, 0);
		gd.showDialog();
		
		this.setWindowSize((int) gd.getNextNumber());
		LowThreshold = (int) gd.getNextNumber();
		HighThreshold = (int) gd.getNextNumber();	
		colorByDepth = gd.getNextBoolean();
		ColorMapStart = (int) gd.getNextNumber();
		ColorMapEnd = (int) gd.getNextNumber();
		
		if(ColorMapStart > ColorMapEnd){
			IJ.showMessage("Color map Start depth is larger than End depth, reversing");
			int tmp = ColorMapEnd;
			ColorMapEnd = ColorMapStart;
			ColorMapStart = tmp;
		}
		
		for(int i = 0; i < ColorMapEnd - ColorMapStart; i++){
			float fract = (float)i / (ColorMapEnd - ColorMapStart);
			int[] lut = new int[256];
			for(int j = 0; j < 256; j++){				
				Color c = new Color(fract*((float)j)/255.0f, (1 - fract)*((float)j)/255.0f, 0);
				int a = c.getAlpha() << 24;
				int rgb = c.getRGB();
				lut[j] = rgb + a;
			}
			ColorLUT.add(lut);
		}		
	}
	
	public void setWindowSize(int ws){
		this.WindowSize = ws;
		this.WindowHalf = (ws - 1)/2;
	}
	
	public void compute(){
		int x = Volume.getWidth();
		int y = Volume.getHeight();
		int z = Volume.getStackSize();		
		
		ArrayList<EnFaceThread> threadList = new ArrayList<EnFaceThread>();
		
		ExecutorService es = Executors.newFixedThreadPool(4);
		
		for(int i = 0; i < y; i++){
			EnFaceThread eft = new EnFaceThread(Volume, i, WindowSize, LowThreshold, HighThreshold);
			threadList.add(eft);
			es.submit(eft);
			eft.run();
		}
		
		es.shutdown();
		while(es.isTerminated()){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		ContrastEnhancer ce = new ContrastEnhancer();
		ce.setNormalize(true);
		ce.setUseStackHistogram(false);

		ImageStack is = null;
		int i = 0;
		for(EnFaceThread t : threadList){
			if(t.EnFace != null){
				if(is == null){
					is = new ImageStack(t.getWidth(), t.getDepth());
				}
				
				if(colorByDepth){
					ByteProcessor b = t.getFloatProcessor().convertToByteProcessor();
					//ce.equalize(b);
					ColorProcessor rgb = b.convertToColorProcessor();
					if(ColorMapStart <= i && i < ColorMapEnd){
						rgb.applyTable(ColorLUT.get(i - ColorMapStart));
					}
					
					is.addSlice(rgb);
				}else{					
					FloatProcessor b = t.getFloatProcessor();
					//ce.equalize(b);
					is.addSlice(b);
				}
			}
			i++;
		}

		EnFaceLayers = new ImagePlus("EnFace Images - Running Window", is);
		EnFaceLayers.show();
	}
	
	public class EnFaceThread implements Runnable{		
		float[] EnFace;
		
		int Layer;
		
		int WindowSize;
		
		int WindowHalf;
		
		int LowThreshold;
		
		int HighThreshold;
		
		ImagePlus Volume;
		
		public EnFaceThread(ImagePlus Volume, int Layer, int WindowSize, int LowThreshold, int HighThreshold){
			this.Volume = Volume;
			this.Layer = Layer;
			this.WindowSize = WindowSize;
			this.WindowHalf = (WindowSize - 1) / 2;
			this.LowThreshold = LowThreshold;
			this.HighThreshold = HighThreshold;
		}
		
		public FloatProcessor getFloatProcessor(){
			int x = Volume.getWidth();
			int y = Volume.getHeight();
			int z = Volume.getStackSize();
			return new FloatProcessor(x, z, EnFace);
		}
		
		public int getWidth(){
			return Volume.getWidth();
		}
		
		public int getDepth(){
			return Volume.getStackSize();
		}

		@Override
		public void run() {
			//super.run();
			
			int x = Volume.getWidth();
			int y = Volume.getHeight();
			int z = Volume.getStackSize();
			
			if(this.Layer < WindowHalf || this.Layer > y - 1 - WindowHalf){
				return;
			}
			
			EnFace = new float[x*z];			

			//Loop through the bscans
			for(int k = 1; k < z; k++){
				ShortProcessor bp = Volume.getStack().getProcessor(k).convertToShortProcessor();
				short[] ba = (short[]) bp.getPixels();
				//Loop through the A-Scans
				for(int j = 0; j < x; j++){					
					//Loops through the z depths
					float avg = 0;
					for(int win = -WindowHalf; win < WindowHalf; win++){
						int _depth = Layer + win;						
						int b = ba[_depth*x + j];
						//b -= Byte.MIN_VALUE;
						
						if(LowThreshold < b && b < HighThreshold){
							avg += b;
						}else{
							avg += 0;
						}						
					}						
	
					EnFace[k*x + j] = (avg / WindowSize);
				}	
				
				ba = null;
			}
		} 
	}
}
