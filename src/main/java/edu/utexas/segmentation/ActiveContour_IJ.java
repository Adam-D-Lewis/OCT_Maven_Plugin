package edu.utexas.segmentation;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import de.biomedical_imaging.ij.nlMeansPlugin.NLMeansDenoising_;
import edu.utexas.math.P2;
import edu.utexas.math.V2;
import edu.utexas.oct_plugin_ij.AttenuationCoefficient;
import edu.utexas.oct_plugin_ij.StentResults;
import edu.utexas.primitives.Tuples.Triplet;
import edu.utexas.exceptions.PointNotFoundException;
import edu.utexas.math.Histogram;
import edu.utexas.math.Math1D;
import edu.utexas.math.Math2D;
import edu.utexas.segmentation.ActiveContour2.DIRECTION;
import ij.IJ;
import ij.ImageJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;
import ij.plugin.filter.RankFilters;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.util.ArrayUtil;

public class ActiveContour_IJ implements PlugIn, MouseListener{
	
	private static ActiveContour2 ac2;
	
	private static List<P2> fitContour;
	
	private int MaskRadius = 7;
	
	private static ImagePlus original;
	
	private static int Extended = 0;
	
	private static HashMap<Integer, StentResults> _ResultsMap = new HashMap<Integer, StentResults>();
	
	public ActiveContour2 getAC2(){
		return ac2;
	}
	
	public static void main(String args[]){
		ImageJ ij = new ImageJ();		
		ij.setVisible(true);	
	}
	
	/***
	 * Draws and returns an ImageJ Overlay from a List of P2 points
	 * 
	 * @param ip ImagePlus to draw on
	 * @param points List of P2 points of the Overlay
	 * @return Returns the Overlay
	 */
	public static Overlay drawPoints(ImagePlus ip, List<P2> points){		
		Overlay o = new Overlay();
		
		for(P2 p : points){
			Roi r = new Roi(p.x(), p.y(), 1, 1);
			o.add(r);
		}
		
		ip.setOverlay(o);
		return o;
	}
	
	/***
	 * Convenience function that gets the P2 points of the Active Contour that is trimmed based on the 
	 * extension that is required to run the Active Contour.
	 * 
	 * @param Trim Size of the P2 points to trim, should be the same as the amount the image was
	 * extended.
	 * @param points List of P2 points that make up the contour.
	 * @return
	 */
	public List<P2> getTrimmedPoints(int Trim, List<P2> points){		
		List<P2> trimmed = new ArrayList<P2>();
		for(int i = Trim; i < points.size() - Trim; i++){
			trimmed.add(points.get(i).subtract(new P2(Trim, Trim)));
		}
	
		return trimmed;
	}
	
	public Overlay getTrimmedOverlay(int Trim, List<P2> points){		
		Overlay o = new Overlay();
		
		for(P2 p : points){
			Roi r = new Roi(p.x() - Trim, p.y() - Trim, 1, 1);
			o.add(r);
		}
	
		return o;
	}
	
	/***
	 * Creates an Overlay based on a List of P2 points
	 * 
	 * @param points 
	 * @return Overlay from List of P2 points
	 */
	public Overlay getOverlay(List<P2> points){		
		Overlay o = new Overlay();
		
		for(P2 p : points){
			Roi r = new Roi(p.x(), p.y(), 1, 1);
			o.add(r);
		}
	
		return o;
	}

	/***
	 * Runs the ActiveContour2 algorithm based on all the inputs.
	 * 
	 * @param ip
	 * @param options
	 * @return Overlay of final contour that is trimmed to fit the original image
	 */
	private Overlay contour(ImagePlus ip, String options){
		ac2 = new ActiveContour2();
		
		String[] args = options.split(" ");
		
		FloatProcessor fp = ip.getProcessor().convertToFloatProcessor();
		
		MaskRadius = 7;
		P2 start = new P2(0, fp.getHeight());
		DIRECTION dir = DIRECTION.BOTTOM_UP;
		boolean show = false;
		String direction = "";
		int fitPoly = 0;
		String exportDir = "";
		
		for(int i = 0; i < args.length; i++){
			String[] com = args[i].split("=");
			switch(com[0]){
			case "radius":
				MaskRadius = Integer.parseInt(com[1]);
				break;
			case "start":
				String[] xy = com[1].split(",");
				start = new P2(Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
				break;
			case "hist-med-diff":
				ac2.setHistogramMedStop(Double.parseDouble(com[1]));
				break;
			case "hist-ent-diff":
				ac2.setHistogramEntStop(Double.parseDouble(com[1]));
				break;		
			case "show":
				show = true;
				break;
			case "dir":
				direction = com[1];
				break;
			case "fit":
				fitPoly = Integer.parseInt(com[1]);
				break;
			case "export":
				if(com.length > 1){
					exportDir = com[1];
				}
				break;
			}
		}		
		
		int itter = 0;
		
		switch(direction){
		case "bottomup":
			dir = DIRECTION.BOTTOM_UP;
			itter = (int)(start.y() - 0);
			break;
		case "topdown":
			dir = DIRECTION.TOP_DOWN;
			itter = (int)(ip.getHeight() - start.y());
			break;
		case "leftright":
			dir = DIRECTION.LEFT_RIGHT;
			break;
		case "rightleft":
			dir = DIRECTION.RIGHT_LEFT;
			break;
		}
		
		int orgWidth = ip.getWidth();
		int orgHeight = ip.getHeight();		
		
		ac2.init(start, 
				dir, 
				orgWidth, 
				MaskRadius, 
				(float[])fp.getPixels(), 
				orgWidth, 
				orgHeight);
		
		if(show){
			drawPoints(ip, ac2.getContourPoints());
		}
		
		long starttime = System.nanoTime();
		for(int i = 0; i < itter; i++){
			ac2.updateCurve();
			if(show){
				ip.getOverlay().clear();
				drawPoints(ip, ac2.getContourPoints());
			}
			IJ.showProgress(i, itter);
		}
		
		List<P2> outputContour = getTrimmedPoints(Extended, ac2.getContourPoints());
		
		if(fitPoly > 0){
			
			if(fitContour != null)
				fitContour.clear();
			
			WeightedObservedPoints obs = new WeightedObservedPoints();
			for(P2 p : outputContour){
				obs.add(p.x(), p.y());
			}
			PolynomialCurveFitter pcf = PolynomialCurveFitter.create(fitPoly);
			double poly[] = pcf.fit(obs.toList());
			
			fitContour = new ArrayList<P2>();
			double x = 0;
			double y = 0;
			//poly is low order first, subtract Extended because the data is trimmed in X, but not y
			for(P2 p : outputContour){
				switch(fitPoly){
				case 1:
					break;
				case 2:
					break;
				case 3:
					x = p.x();
					y = (poly[0] + poly[1]*x + poly[2]*x*x + poly[3]*x*x*x) - MaskRadius*2;
					fitContour.add(new P2(x, y));
					break;		
				case 4:
					x = p.x();
					y = (poly[0] + poly[1]*x + poly[2]*x*x + poly[3]*x*x*x) + poly[4]*x*x*x*x - MaskRadius*2;
					fitContour.add(new P2(x, y));
					break;
				}
			}
			drawPoints(ip, fitContour);
			outputContour = fitContour;
		}else{
			fitContour = outputContour;
		}
		
		if(exportDir != ""){
			File f = new File(exportDir);
			try {
				FileWriter fw = new FileWriter(exportDir, true);
				String line = "";
				for(P2 p : fitContour){
					line += p.y() + ",";
				}
				fw.write(line + "\r\n");
				fw.flush();
				fw.close();
			} catch (IOException e) {
				IJ.log(e.getMessage());
				e.printStackTrace();
			}			
		}
		
		
		long endtime = System.nanoTime();
		System.out.println("Active Contour Runtime: " + (endtime - starttime)/1e9);
		
		return getOverlay(outputContour);
	}
	
	/***
	 * Extends the image in +/-x and +/-y based on the input string options "radius=X". The extension is
	 * a mirror of the edges.
	 * 
	 * @param options String options
	 * @param fp FloatProcessor to mirror
	 * @return new FloatProcessor that is extended through mirroring
	 */
	private FloatProcessor extendImage(String options, FloatProcessor fp){		
		int Radius = 7;
		
		if(options != null){
			String[] args = options.split(" ");
			for(int i = 0; i < args.length; i++){
				String[] com = args[i].split("=");
				switch(com[0]){
				case "radius":
					Radius = Integer.parseInt(com[1]);
					break;
				}			
			}
		}
		
		Extended = Radius;
		
		float[] old = (float[])fp.getPixels();
		int oldWidth = fp.getWidth();
		int oldHeight = fp.getHeight();
		
		int newWidth = fp.getWidth() + 2*Radius;
		int newHeight = fp.getHeight() + 2*Radius;
		
		int lineOffset = Radius;
		
		float[] padded = new float[newWidth*newHeight];
		
		for(int i = 0; i < Radius; i++){
			System.arraycopy(old, i*oldWidth, padded, (Radius - 1 - i)*newWidth + lineOffset, oldWidth);
			System.arraycopy(old, (oldHeight - 1 - i)*oldWidth, padded, (newHeight - 1 - (Radius - 1 - i))*newWidth + lineOffset, oldWidth);
		}
		
		for(int i = 0; i < oldHeight; i++){
			System.arraycopy(old, i*oldWidth, padded, (i + Radius)*newWidth + lineOffset, oldWidth);
			
			float[] front = Arrays.copyOfRange(old, i*oldWidth, i*oldWidth + Radius);
			float[] end = Arrays.copyOfRange(old, i*oldWidth + oldWidth - 1 - Radius, i*oldWidth + oldWidth);
			
			float[] revFront = new float[front.length];
			float[] revEnd = new float[end.length];
			
			for(int j = 0; j < front.length; j++){
				revFront[j] = front[front.length - 1 - j];
				revEnd[j] = end[end.length - 1 - j];
			}
			
			System.arraycopy(revFront, 0, padded, (i + Radius)*newWidth, revFront.length);
			System.arraycopy(revEnd, 0, padded, (i + Radius)*newWidth + oldWidth + Radius, revEnd.length);
		}
				
		FloatProcessor nfp = new FloatProcessor(newWidth, newHeight);
		nfp.setPixels(padded);
		
		return nfp;
	}
	
	/***
	 * Trolls the options string to see if the show option is set
	 * 
	 * @param options String of options
	 * @return true or false if "show" is present
	 */
	private boolean show(String options){
		boolean sh = false;
		if(options != null){
			String[] args = options.split(" ");
			for(int i = 0; i < args.length; i++){
				String[] com = args[i].split("=");
				switch(com[0]){
				case "show":
					return true;
				}			
			}
		}else{
			return false;
		}
		return sh;		
	}	
	
	private float[] peakDetect(float[] in, float distance, float amplitude){
		float[] inv = Math1D.invert(in);
		
		float[] filter = new float[inv.length];
		int window = 1;
		float mean = 0;
		for(int i = window; i < in.length - 1 - window; i++){
			float o = 0;
			for(int k = -window; k <= window; k++){
				o += inv[i+k];
			}
			o /= 2*window;
			filter[i] = o;
			mean += filter[i];
		}		
		mean /= in.length;
		
		float std = 0;
		float max = 0;
		for(int i = 0; i < filter.length; i++){
			std += Math.pow(mean - filter[i], 2);
			if(filter[i] > max){
				max = filter[i];
			}
		}
		std /= filter.length;
		std = (float) Math.sqrt(std);
		
		float[] diff = Math1D.differentiate(filter, 1);
		float[] acc = Math1D.differentiate(diff, 1);
		float[] zc = Math1D.zeroCrossing(acc);
		
		List<Float> peakList = new ArrayList<Float>();
		
		//loop through the zc to find peaks to average the amplitude of the peaks
		float meanPeaks = 0;
		float meanPeakCount = 0;
		for(int i = 0; i < zc.length - 1; i++){
			int k = (int) zc[i] + window;
			if(filter[k] > mean){
				meanPeaks += filter[k];
				meanPeakCount += 1;
			}			
		}
		meanPeaks /= meanPeakCount;
		meanPeaks *= .75;
		
		for(int i = 0; i < zc.length; i++){
			int k = (int) zc[i] + window;
			if(k < inv.length){
				if(filter[k] > (meanPeaks) && diff[k] < 0){// && diff[k - window] > 250){// && filter[k] > amplitude){
					peakList.add((float)k);
				}
			}
		}	
		
		float[] x = new float[diff.length];
		for(int i = 0; i < x.length; i++){
			x[i] = i;
		}
	
		boolean show = false;
		
		if(show){
			Plot intP = new Plot("Integrated Plot", "Width", "Integrated", x, filter);
			intP.show();
			
			Plot diffP = new Plot("Integrated Plot", "Width", "Integrated", x, diff);
			diffP.show();			
		}
		
		float[] peakListArr = new float[peakList.size()];
		for(int i = 0; i < peakList.size(); i++){
			peakListArr[i] = peakList.get(i);
		}
		
		return peakListArr;
	}
	
	private Overlay peaksToOverlay(float[] peaks){
		Overlay o = new Overlay();
		o.setFillColor(Color.ORANGE);
		
		for(float f : peaks){
			Roi r = new Roi(f, 25, 1, 1);
			o.add(r);
		}
		
		return o;
	}
	
	private StentResults findStentsFromShadows(float[] peaks, FloatProcessor fp, ActiveContour2 contour, boolean debug){
		List<P2> lp = contour.getTrimmedContourPoints(new P2(Extended, Extended), Extended);
		
		List<P2> stents = new ArrayList<P2>();
		List<P2> tissue = new ArrayList<P2>();
		
		for(float peakX : peaks){
			P2 contourpPoint = lp.get((int) peakX);
			P2 start = contourpPoint;
			
			try{
				int slop = 15;
				int w = 9;
				int h = 101;
				Roi r = new Roi(start.x() - (w - 1)/2, start.y() - (h - 1)/2 - slop, w, h);
				fp.setRoi(r);
				FloatProcessor nfp = (FloatProcessor) fp.crop();
				
				float[] stentMask = {0, 0, 0, 1, 1, 1, 0, 0, 0};
				float[] stentPlot = new float[h];
				float[] pixels = (float[])nfp.getPixels();
				float[] dep = new float[h];
				
				for(int y = 0; y < h; y++){
					dep[y] = y;
					for(int x = 0; x < w; x++){
						stentPlot[y] += stentMask[x]*pixels[y*w + x];
					}
					stentPlot[y] /= w;
				}
				
				float stentMax = Float.MIN_NORMAL;
				int stentMaxIndex = 0;
				
				for(int y = 0; y < h; y++){
					if(stentPlot[y] > stentMax){
						stentMax = stentPlot[y];
						stentMaxIndex = y;
					}
				}	
				
				
//				if(peakX == 369){
//					ImagePlus ip = new ImagePlus("Cropped", nfp.convertToShortProcessor());
//					ip.show();
//
//					Plot ppp = new Plot("","","",dep, stentPlot);
//					ppp.show();
//					
//					//float[] diff = Math1D.differentiate(filter, 2);
//					Plot pppp = new Plot("","","",dep, filter);
//					pppp.show();
//					
//					ip.close();
//				}		
				
				double yy = start.y() - (h - 1)/2 - slop + stentMaxIndex;
				
				P2 p = new P2(peakX, yy);
				//p = p.add(start);
				
				
				stents.add(p);
				tissue.add(new P2(peakX, contourpPoint.y()));
			}catch(ArrayIndexOutOfBoundsException e){
				System.out.print(e.getMessage());
			}			
		}	
		
		return new StentResults(stents, tissue);
	}
	
	/***
	 * Creates a string or options using an IJ general dialog interface.
	 * 
	 * @return String of parsed options
	 */
	private String Options(){
		String options = "";
		
		GenericDialog gd = new GenericDialog("Active Contour Options");
		gd.addNumericField("Region radius (odd only):", 11, 0);
		gd.addNumericField("Starting X:", 0, 0);
		gd.addNumericField("Starting Y:", IJ.getImage().getHeight(), 0);
		gd.addNumericField("Histogram Seperation", 12000, 0);
		gd.addCheckbox("Show updates?", false);
		String choices[] = {"Top Down", "Bottom Up", "Left Right", "Right Left"};
		gd.addChoice("Direction", choices, "Bottom Up");
		gd.addCheckbox("Fit Contour to 4th Order Poly", false);
		gd.addCheckbox("Export Contour", false);
		
		gd.showDialog();
		
		if(gd.wasCanceled()){
			return options;
		}
		
		int Radius = (int) gd.getNextNumber();
		int X = (int) gd.getNextNumber();
		int Y = (int) gd.getNextNumber();
		int HistSep = (int) gd.getNextNumber();
		boolean show = gd.getNextBoolean();
		String showS = "";
		int choiceIndex = gd.getNextChoiceIndex();
		boolean fitContour = gd.getNextBoolean();
		boolean export = gd.getNextBoolean();
		
		String exportPath = "export=";
		if(export){
			JFileChooser jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if(jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
				exportPath += jfc.getSelectedFile().getAbsolutePath();
			}
		}
		
		if(show){
			showS = "show ";
		}

		if(Radius % 2 == 0){
			Radius += 1;
		}
		
		String fit = "";
		if(fitContour){
			fit = "fit=4 ";
		}
		
		String dir = choices[choiceIndex].toLowerCase().replace(" ", "");
		
		options = "radius=" + Radius + " " +
				"start=" + X + "," + Y + " " +
				"hist-med-diff=" + HistSep + " " +
				"hist-ent-diff=.05 " +
				"dir=" + dir + " " + 
				showS + 
				fit + 
				exportPath;
		
		return options;
	}
	
	@Override
	public void run(String arg0) {
		String options = Macro.getOptions();

		try{
			IJ.getImage().getCanvas().removeMouseListener(this);
		}catch(Exception e){
			
		}
		
		FloatProcessor nfp;
		ImagePlus ni;
		ShortProcessor sp;
		ContrastEnhancer ce;
		RankFilters rf;
		float[] ff;
		FloatProcessor trimmedFp;
		Overlay o;
		ImagePlus ip;
		float[] out;
		int startFrame, endFrame;

		Extended = 27;
		
		switch(arg0){		
			case "Flatten to Contour":
				//TODO: Run through all images, apply Active Contour and flatten images
				sp = IJ.getProcessor().convertToShortProcessor();
				ff = (float[])sp.convertToFloatProcessor().getPixels();				
				
				List<P2> contour = ac2.getTrimmedContourPoints(new P2(Extended, Extended), Extended);

				out = Math2D.flatten(ff, 
						IJ.getImage().getWidth(), 
						IJ.getImage().getHeight(), 
						contour,
						300);	
				
				trimmedFp = new FloatProcessor(IJ.getImage().getWidth(), 300, out);
				ni = new ImagePlus("Flattened Image", trimmedFp);
				ni.show();
				
				break;
				
			case "FlattenVolume":
				if(options == null){
					//Show Popup
					options = Options();
				}				
				
				original = IJ.getImage();	
				
				startFrame = 1;
				endFrame = original.getStackSize();
				if(options.contains("applyall")){
					startFrame = 1;
					endFrame = original.getStackSize();				
				}
				
				ImageStack volStack = new ImageStack(original.getWidth(), original.getHeight());
				for(int i = startFrame; i <= endFrame; i++){
					long start = System.nanoTime();					
					
					ImageProcessor improc = original.getStack().getProcessor(i);
					o = _runActiveContour(improc, options);

					ff = (float[])improc.convertToFloatProcessor().getPixels();
					out = Math2D.flatten(ff, improc.getWidth(), improc.getHeight(), fitContour, improc.getHeight());
					nfp = new FloatProcessor(improc.getWidth(), improc.getHeight(), out);
					volStack.addSlice(nfp);
					
					long stop = System.nanoTime();
					double seconds = (stop - start) / 1e9;
					IJ.log("Finished frame " + i + " in " + seconds + " seconds");
				}
				ImagePlus vol = new ImagePlus("Flattened Volume", volStack);
				vol.show();
				
				break;
				
			case "Flatten to Fit":
				sp = IJ.getProcessor().convertToShortProcessor();
				ff = (float[])sp.convertToFloatProcessor().getPixels();				

				if(fitContour == null){
					IJ.showMessage("Fit hasn't been defined, run Active Contour 2 with Fit to Poly selected");
					return;
				}
				
				contour = fitContour;
				
				out = Math2D.flatten(ff, 
						IJ.getImage().getWidth(), 
						IJ.getImage().getHeight(), 
						contour,
						IJ.getImage().getHeight());	
				
				trimmedFp = new FloatProcessor(IJ.getImage().getWidth(), IJ.getImage().getHeight(), out);
				ni = new ImagePlus("Flattened Image", trimmedFp);
				ni.show();
				
				break;
				
			case "Export":
				JFileChooser jfc = new JFileChooser();
				jfc.setDialogTitle("Choose File for Export");
				jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				FileFilter fff = new FileNameExtensionFilter("Text File", "txt");
				jfc.addChoosableFileFilter(fff);
				int res = jfc.showOpenDialog(null);
				
				if(res == JFileChooser.APPROVE_OPTION){
					File file = jfc.getSelectedFile();
					file.setWritable(true);
					FileWriter fw;
					try {
						fw = new FileWriter(file);
						Collection<StentResults> src = _ResultsMap.values();
						Set<Integer> frames = _ResultsMap.keySet();
						for(int i = 0; i < src.size(); i++){
							StentResults sr = (StentResults) src.toArray()[i];
							String s = "Frame " + frames.toArray()[i] + ": \t";
							for(int j = 0; j < sr.getStents().size(); j++){
								P2 p = sr.getStents().get(j);
								s += p.x() + "," + p.y() + "," + sr.getTissue().get(j).y() + "\t";
							}
							fw.write(s + "\r\n");
						}	
						
						fw.close();
					} catch (IOException e) {
						System.out.println(e.getMessage());
					} 
				}			
				break;
		
			case "Stent":
				if(options == null){
					//Show Popup
					options = Options();
				}
				
				
				original = IJ.getImage();	
				
				startFrame = original.getCurrentSlice();
				endFrame = original.getCurrentSlice() + 1;
				if(options.contains("applyall")){
					startFrame = 1;
					endFrame = original.getStackSize();				
				}
				
				for(int i = startFrame; i < endFrame; i++){
					IJ.showProgress(i, endFrame - startFrame);
					ShortProcessor ipp = original.getStack().getProcessor(i).duplicate().convertToShortProcessor();

					ce = new ContrastEnhancer();
					ce.setNormalize(false);
					ce.equalize(ipp);
					
					float[] current = (float[])ipp.convertToFloatProcessor().getPixels();
					nfp = new FloatProcessor(ipp.getWidth(), ipp.getHeight(), current);
					nfp = extendImage("radius=27", nfp);		
					ip = new ImagePlus("Clone", nfp);
					
//					NLMeansDenoising_ nl = new NLMeansDenoising_();
//					nl.setup("", ip);
//					nl.run(nfp);
					
					if(show(options)){
						ip.show();
						ip.getCanvas().addMouseListener(this);
						IJ.run("Enhance Contrast", "saturated=0.35");
					}

					//IJ.run(ip, "Enhance Local Contrast (CLAHE)", "blocksize=127 histogram=256 maximum=3 mask=*None*");
					original.setOverlay(contour(ip, options));
					
					StentResults sr = shadowFinder(false);
					Integer key = i;
					_ResultsMap.put(key, sr);
					
					if(show(options)){
						ip.setOverlay(null);
						ip.changes = false;
						ip.close();
					}
				}
				
				break;
				
			case "Run ActiveContour2":
				ip = IJ.getImage();
				o = _runActiveContour(ip.getProcessor(), options);
				ip.setOverlay(o);
				break;
				
			case "Find Shadows":
				original = IJ.getImage();
				shadowFinder(true);				
				break;
		}
		
	}

	private Overlay _runActiveContour(ImageProcessor ip, String options) {		
		//ImagePlus ip = IJ.getImage();
		FloatProcessor nfp = ip.convertToFloatProcessor();
		nfp = extendImage("radius=27", nfp);
		
		if(options == null){
			//Show Popup
			options = Options();
		}
		
		ImagePlus ni = new ImagePlus("Clone", nfp);
//		NLMeansDenoising_ nl = new NLMeansDenoising_();
//		nl.setup("", ni);
//		nl.run(nfp);	
		
		if(show(options)){
			ni.show();
			
			boolean alreadyAdded = false;
			MouseListener[] m = ni.getCanvas().getMouseListeners();
			for(MouseListener ml : m){
				if(ml == this){
					alreadyAdded = true;
					break;
				}
			}
			
			if(!alreadyAdded){
				ni.getCanvas().addMouseListener(this);
			}
		}
		
		Overlay o = contour(ni, options);
		
//		if(show(options)){
//			ni.close();
//		}
		
		return o;
	}

	private StentResults shadowFinder(boolean debug) {
		ShortProcessor sp = original.getProcessor().convertToShortProcessor();		
		
		float[] ff = (float[])sp.convertToFloatProcessor().getPixels();	
		
		float[] outT = Math2D.flatten(ff, 
				original.getWidth(), 
				original.getHeight(), 
				ac2.getTrimmedContourPoints(new P2(Extended, Extended), Extended),
				100);	
		
		FloatProcessor trimmedFp = new FloatProcessor(original.getWidth(), 100, outT);
		RankFilters rf = new RankFilters();
		rf.rank(trimmedFp, 1, RankFilters.MEDIAN);
		//rf.rank(trimmedFp, 6, RankFilters.OUTLIERS, RankFilters.BRIGHT_OUTLIERS, 20);
		
		//trimmedFp.
		//NLMeansDenoising_ nl = new NLMeansDenoising_();
		//nl.applyNonLocalMeans(trimmedFp, 15);
		//IJ.run("Enhance Local Contrast (CLAHE)", "blocksize=127 histogram=256 maximum=3 mask=*None* fast_(less_accurate)");

		ff = (float[])trimmedFp.getPixels();	
		float[] shad = Math2D.integrate(ff, trimmedFp.getWidth(), trimmedFp.getHeight());
		
		
		float[] peaks = peakDetect(shad, 1, 750);
		//AttenuationCoefficient ac = new AttenuationCoefficient();
		//float[] attenuation = ac.run(ff, original.getWidth(), original.getHeight());
		ff = (float[])sp.convertToFloatProcessor().getPixels();	
		FloatProcessor nfp = new FloatProcessor(original.getWidth(), original.getHeight(), ff);
		
		if(debug){
			ImagePlus ipppp = new ImagePlus("Trimmed / Flattened", trimmedFp);
			ipppp.show();
			ipppp.setOverlay(peaksToOverlay(peaks));
		}
		
		StentResults stentRes = findStentsFromShadows(peaks, nfp, ac2, debug);
		
		Overlay o = new Overlay();
		
		int w = 5;
		for(int i = 0; i < stentRes.getStents().size(); i++){
			P2 s = stentRes.getStents().get(i);
			Roi r = new OvalRoi(s.x() - w, s.y() - w, 2*w, 2*w);
			o.add(r);
			P2 startLine = new P2(s.x() - 5, stentRes.getTissue().get(i).y());
			P2 endLine = new P2(s.x() + 5, stentRes.getTissue().get(i).y());
			r = new Line(startLine.x(), startLine.y(), endLine.x(), endLine.y());
			o.add(r);
		}
		original.setOverlay(o);
		
		return stentRes;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		ImageCanvas ic = (ImageCanvas)arg0.getSource();
		double mag = ic.getMagnification();
		Point p = ic.getCursorLoc();

		float[] mask = ac2.getMask();

		V2 v = new V2(new P2(0,0), new P2(10,10));
		for(int i = 0; i < ac2.getContourVector().size(); i++){
			v = ac2.getContourVector().get(i);
			if(v.getStart().x() == p.getX()){
				break;
			}
		}
		
		Region r = ac2.createRegion(v);
		
		if(r == null){
			return;
		}
		
		List<P2> cir = r.contourInRegions(ac2.getContourVector());
		r.bisect();
		
		
		double[] xAxis = new double[r.getR1().length];
		for(int i = 0; i < xAxis.length; i++){
			xAxis[i] = i;
		}

		//ac2.testCurveContinuity(new P2(p.getX(), p.getY()));
		
		RegionStatistics rs = new RegionStatistics(r);		
		Plot plotR1 = new Plot("R1 Histogram", "Bins", "Values", xAxis, rs.getR1HistogramCount());
		plotR1.show();
		Histogram hr1 = rs.getR1Histogram();
			
		Plot plotR2 = new Plot("R2 Histogram", "Bins", "Values", xAxis, rs.getR2HistogramCount());
		plotR2.show();
		Histogram hr2 = rs.getR2Histogram();
		
		//show the mask
		//FloatProcessor fp = new FloatProcessor(MaskRadius*2 + 1, MaskRadius*2 + 1, r.getPixels());
		//ImagePlus ip = new ImagePlus("Mask", fp);
		//ip.show();
		//drawPoints(fp, cir);
		
		//show the mask R1
		boolean ShowMasks = false;
		if(ShowMasks){
			FloatProcessor fp1 = new FloatProcessor(MaskRadius*2 + 1, MaskRadius*2 + 1, r.getMask1());
			ImagePlus ip1 = new ImagePlus("Mask 1", fp1);
			ip1.show();
			drawPoints(ip1, cir);
			
			//Show Mask R2
			FloatProcessor fp2 = new FloatProcessor(MaskRadius*2 + 1, MaskRadius*2 + 1, r.getMask2());
			ImagePlus ip2 = new ImagePlus("Mask 2", fp2);
			ip2.show();
			drawPoints(ip2, cir);
		}
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
