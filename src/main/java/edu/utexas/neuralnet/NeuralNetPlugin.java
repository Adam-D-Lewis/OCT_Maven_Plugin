package edu.utexas.neuralnet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLDevice.Type;

import edu.utexas.imageprocessing.GLCM;
import edu.utexas.imageprocessing.GLCM.GLCM_TYPE;
import edu.utexas.imageprocessing.GLCMResult;
import edu.utexas.opencl.UTOpenCL;
import edu.utexas.oct_plugin_ij.AttenuationCoefficient;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class NeuralNetPlugin implements PlugIn{
	static UTOpenCL ocl;// = new UTOpenCL(Type.CPU);
	
	GLCM octNN;
	
	@Override
	public void run(String arg0) {
		ImagePlus ip;
		float[] atten;
		float[] input;
		NeuralNetInterface nni;
		
		switch(arg0.toLowerCase().trim()){
		case "feature-gpu":
			ocl = new UTOpenCL(Type.GPU);
			IJ.log(ocl.getDevice().getName());
			ip = IJ.getImage();			
			nni = new NeuralNetInterface(ip, ocl);
			nni.setVisible(true);		
			break;
			
		case "feature-cpu":
			ocl = new UTOpenCL(Type.CPU);
			IJ.log(ocl.getDevice().getName());
			ip = IJ.getImage();			
			nni = new NeuralNetInterface(ip, ocl);
			nni.setVisible(true);		
			break;
			
		case "export":
			
			break;
		}		
	}
	
	public static void main(String[] args){
		List<InputStream> isList = new ArrayList<InputStream>();
		
		InputStream fis = GLCM.class.getResourceAsStream("TextureAnalysis.cl");			
		isList.add(fis);
		
		GLCM octNN = new GLCM(ocl, isList);
		
		ArrayList<Integer> pixel = new ArrayList<Integer>();
		ArrayList<Double> theta = new ArrayList<Double>();
		
		pixel.add(1);
		theta.add((double) 0);
		theta.add(Math.PI/4);
		theta.add(Math.PI/2);
		theta.add(Math.PI*3/4);
		
		octNN.createXYOffset(pixel, theta);
	}
}
