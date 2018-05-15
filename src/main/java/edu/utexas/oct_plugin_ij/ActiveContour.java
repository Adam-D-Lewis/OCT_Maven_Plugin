package edu.utexas.oct_plugin_ij;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import javax.swing.event.MouseInputListener;






import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.Path2D;
import java.awt.geom.Path2D.Float;
import java.io.PrintWriter;

public class ActiveContour implements PlugIn, MouseListener, MouseMotionListener{

//	public static void main(String[] args) {
//		@SuppressWarnings("unused")
//		int i = 4;
//	}
	
	private static ArrayList<Integer> latchedPointIndices = new ArrayList<Integer>();
	private static int fillCount = 0;
	private static ArrayList<Point> priorStepCurve = new ArrayList<Point>();
	ImagePlus _parent;

	int mouseClickedCount = 0;
	int catheterTop = 0;
	int catheterBottom = 0;
	int blankSpaceHeight = 0;
	Point noiseTemplateLocation = new Point(0,0);
	int[][] imageFillMask = new int[ij.WindowManager.getCurrentImage().getHeight()][ij.WindowManager.getCurrentImage().getWidth()];
	ArrayList<Point> userInputtedPoints = new ArrayList<Point>();
	
	
	//create curve method is only for user supplied initial contour
	public ArrayList<Point> createCurve(ArrayList<Point> inputPoints, int imageW, int numberOfPoints, boolean closed, int imageH, boolean wrap, ImageProcessor OCT_Image) {

		ArrayList<Point> workingArray = new ArrayList<Point>(); //array to be populated with the initial curve
		ArrayList<Point> outputArray = new ArrayList<Point>(); //array to be populated with the the output points
		ArrayList<Point> displacementsXY = new ArrayList<Point>(); //array to be populated with the displacements in x and y directions
		ArrayList<Double> displacementsMag = new ArrayList<Double>(); //array to be populated with the displacement magnitude to the next point
		Point firstPointOpen; //the first point is attached to left edge but has a y value of the first inputed point
		Point lastPointOpen = null; //the last point has the x value of end of image with y value of last inputed point
		Point lastPointClosed = null; //if closed, the first point is the last point
		Point lastLaid = new Point();
				
		double totalCurveDistance = 0;
		if (!closed){
			firstPointOpen = new Point(0, (int) inputPoints.get(0).getY());
			lastPointOpen = new Point(imageW, (int) inputPoints.get(inputPoints.size()-1).getY());
			workingArray.add(firstPointOpen); 
			workingArray.addAll(inputPoints);
			workingArray.add(lastPointOpen);
		}else{
			lastPointClosed = new Point((int)inputPoints.get(0).getX(), (int) inputPoints.get(0).getY());
			workingArray.addAll(inputPoints);
			workingArray.add(lastPointClosed);
		}
				
		//now calculate the displacements from each point to the next
		for (int k=0; k<workingArray.size()-1; k++){
			displacementsXY.add(new Point((int) (workingArray.get(k+1).getX()-workingArray.get(k).getX()), (int) (workingArray.get(k+1).getY()-workingArray.get(k).getY())));
			displacementsMag.add(Math.pow((double) Math.pow(((double) displacementsXY.get(k).getX()), 2) + Math.pow(((double) displacementsXY.get(k).getY()), 2), 0.5));
		}
		
		//calculate the total distance along the inputed curve
		for (int k=0; k<displacementsMag.size(); k++){
			totalCurveDistance = (double) (totalCurveDistance + displacementsMag.get(k));
		}
		//populating the output array	
		outputArray.add(workingArray.get(0));
		double distanceBtwnEachPoint = totalCurveDistance / numberOfPoints;
		double distanceToNextGivenPoint = 0;
		for (int k = 0; k < workingArray.size()-1; k++){
			distanceToNextGivenPoint = workingArray.get(k).distance(workingArray.get(k+1));
			while (distanceBtwnEachPoint <= distanceToNextGivenPoint){
				lastLaid = (outputArray.get(outputArray.size()-1));
				outputArray.add(interpolatePoint(lastLaid, workingArray.get(k+1), distanceBtwnEachPoint));
				distanceToNextGivenPoint = outputArray.get(outputArray.size()-1).distance(workingArray.get(k+1));
			}
			if (distanceBtwnEachPoint>distanceToNextGivenPoint && k < (workingArray.size()-2)){
				outputArray.add(workingArray.get(k+1));   //NOTE: we can remove this line if our sample is large enough
				outputArray.add(interpolatePoint(workingArray.get(k+1), workingArray.get(k+2), distanceBtwnEachPoint-distanceToNextGivenPoint));
			}
			
		}
		

		if (closed){
			outputArray.add(lastPointClosed);
		}else{
			outputArray.add(lastPointOpen);
		}
		return outputArray;
	}
	
	
	public Point interpolatePoint(Point point1, Point point2, double distanceFromPoint1){
		Point interpolatedPoint = new Point(0,0);
		double dist = Math.pow(Math.pow(point1.getX()-point2.getX(), 2)+Math.pow(point1.getY()-point2.getY(), 2),0.5);
		double ratio = distanceFromPoint1 / dist;
		
		interpolatedPoint.setLocation((int) Math.round(ratio*point2.getX()+(1-ratio)*point1.getX()),(int) Math.round(ratio*point2.getY()+(1-ratio)*point1.getY()));
		
		return interpolatedPoint;
	}
	
	public int[] createHistogram(double maxLimit, double minLimit, int numberOfBins, ArrayList<Double> intensities){
		int[] histogramBins = new int[numberOfBins+1];
		for(int i = 0; i<intensities.size(); i++){
			int indexForPlacement = (int) Math.round(((intensities.get(i)-minLimit)*(numberOfBins))/(maxLimit-minLimit));    ///NEED TO see how this is being calculated
			histogramBins[indexForPlacement] = histogramBins[indexForPlacement] + 1;
		}
		return histogramBins;
	
	}
	
	public double calculateHistogramSeparation(int[] hist1, int[] hist2){
		double histogramSeparationCoefficient = -1;
		if (hist1.length != hist2.length){
			System.out.println("Histogram lengths do not match.");
			return histogramSeparationCoefficient;
		}
		histogramSeparationCoefficient = 0;
		for (int i = 0; i < hist1.length; i++){
			histogramSeparationCoefficient = histogramSeparationCoefficient + Math.pow(hist1[i]*hist2[i], 0.5);
		}	
		return histogramSeparationCoefficient;
	}
	
	public ArrayList<Point> remvoveDuplicates(ArrayList<Point> inputPoints){
		ArrayList<Point> outputPoints = new ArrayList<Point>();
		for (int i = 0; i < inputPoints.size(); i++){
			if (outputPoints.contains(inputPoints.get(i)) == false){
				outputPoints.add(inputPoints.get(i));
			}
		}
		return outputPoints;
	}
	
	public void fillInterior(Point node, ArrayList<Point> curvePoints, boolean open, boolean up, ImageProcessor OCT_Image){
		curvePoints = createStepCurve(curvePoints, open);
		if (fillCount == 0){
			if(open){
				priorStepCurve = curvePoints; //save the initial open step/continuous curve for optimization later				
			}
			imageFillMask = new int[(int) OCT_Image.getHeight()][(int) OCT_Image.getWidth()];
		}
				
		if (open && fillCount>0){ //optimization to move un-latched point mask up by 1 instead of filling entire mask again
			for (int i = 0; i < curvePoints.size(); i++){
				if (!priorStepCurve.contains(curvePoints.get(i))){
					if (up){
						imageFillMask[(int) curvePoints.get(i).getY()][(int) curvePoints.get(i).getX()] = 0;
					}else{
						if(curvePoints.get(i).getY() < OCT_Image.getHeight() && curvePoints.get(i).getX() < OCT_Image.getWidth()){
							imageFillMask[(int) curvePoints.get(i).getY()][(int) curvePoints.get(i).getX()] = 1; 
						}
					}
				}
			}
			priorStepCurve = curvePoints; //save this iteration's curve for next check
			fillCount++;
			return;
		}
		//code for initial fill population
		fillCount++;
		if (open){
			node.setLocation(new Point(0,0));
		}

		if (node.getX() >= OCT_Image.getWidth() || node.getX() < 0 || node.getY() >= OCT_Image.getHeight() || node.getY() < 0){ //out of bounds catch
			System.out.println("Flood Fill Error: initial node out of bounds.");
			
		}
		
		
		ArrayList<Point> pointsToFill = new ArrayList<Point>();
		pointsToFill.add(node);
		

		
		while (pointsToFill.size()>0){
			
			int ptX = (int) (pointsToFill.get(pointsToFill.size()-1).getX());
			int ptY = (int) (pointsToFill.get(pointsToFill.size()-1).getY());
			
			if (curvePoints.contains(pointsToFill.get(pointsToFill.size()-1)) || pointsToFill.get(pointsToFill.size()-1).getX()<0 || pointsToFill.get(pointsToFill.size()-1).getY()<0 || pointsToFill.get(pointsToFill.size()-1).getX()>=OCT_Image.getWidth() || pointsToFill.get(pointsToFill.size()-1).getY()>=OCT_Image.getHeight()){ //this means that the point is hitting the border or is out of bounds
				pointsToFill.remove(pointsToFill.size()-1);
				continue;
			}
			
			if (imageFillMask[ptY][ptX] == 1){ //this means the point has been filled already
				pointsToFill.remove(pointsToFill.size()-1);
				continue;
			}

			imageFillMask[ptY][ptX] = 1;
			
			
			
			pointsToFill.remove(pointsToFill.size()-1);
			pointsToFill.add(new Point((int) ptX-1,(int) ptY));
			pointsToFill.add(new Point((int) ptX,(int) ptY+1));
			pointsToFill.add(new Point((int) ptX,(int) ptY-1));
			pointsToFill.add(new Point((int) ptX+1,(int) ptY));
		
		}
		for (int l = 0; l<curvePoints.size(); l++){
			if(curvePoints.get(l).getX() < OCT_Image.getWidth() && curvePoints.get(l).getY() < OCT_Image.getHeight()){
				imageFillMask[(int) curvePoints.get(l).getY()][(int) curvePoints.get(l).getX()]=1;
			}
		}
		return;

	}
	
	public void printMask(ImageProcessor mImage){
		try{
		    PrintWriter pr = new PrintWriter("output.txt");    

		    for (int i=0; i < mImage.getHeight(); i++) {
		    	for (int j = 0; j < mImage.getWidth(); j++){
		    		pr.print(imageFillMask[i][j]);
		    	}
		    		pr.println("");
		    }
		    pr.close();
		
		}catch (Exception e){
		    e.printStackTrace();
		    System.out.println("No such file exists.");
		}
	}
	
	public ArrayList<Point> createStepCurve(ArrayList<Point> curvePoints, boolean open){
		int current, next;
		ArrayList <Point> outputPoints = new ArrayList<Point>();
		ArrayList <Point> pointsToAppend = new ArrayList<Point>();
		
		for (int i = 0; i<curvePoints.size(); i++){
			if (i == curvePoints.size()-1){
				if (!open){
					current = i;
					next = 0;
				} else {
					continue;
				}
			} else {
				current = i;
				next = i+1;
			}
			if (curvePoints.get(current).equals(curvePoints.get(next)) || curvePoints.get(current).distanceSq(curvePoints.get(next))<=2){ //if points are adjacent, just add to output array
				outputPoints.add(curvePoints.get(current));
			} else { //if they're not adjacent, we need to operate
				pointsToAppend.clear();
				pointsToAppend = fillGap( (int) (curvePoints.get(next).getX()-curvePoints.get(current).getX()), (int) (curvePoints.get(next).getY()-curvePoints.get(current).getY()), curvePoints.get(current));
				outputPoints.addAll(pointsToAppend);
				
			}
		}
		
		return outputPoints;
	}
	
	public ArrayList<Point> fillGap(int deltaX, int deltaY, Point startingPoint){
		ArrayList <Point> outputPoints = new ArrayList<Point>();
		
		outputPoints.add(startingPoint);
		
		if (deltaX>=0){ //work with positive or zero deltaX
			for (int i = 1; i <= deltaX; i++){
				outputPoints.add(new Point((int) startingPoint.getX() + i,(int) startingPoint.getY()));
			}
		} else { //work with negative deltaX
			for (int i = -1; i >= deltaX; i--){
				outputPoints.add(new Point((int) startingPoint.getX() + i,(int) startingPoint.getY()));
			}
		}
		
		
		if (deltaY >= 0){ //work with positive or zero deltaY
			for (int i = 1; i <= deltaY; i++){
				outputPoints.add(new Point((int) outputPoints.get(outputPoints.size()-1).getX(),(int) startingPoint.getY() + i));
			}
		} else { //work with negative deltaY
			for (int i = -1; i >= deltaY; i--){
				outputPoints.add(new Point((int) outputPoints.get(outputPoints.size()-1).getX(),(int) startingPoint.getY() + i));
			}
		}
		
		
		return outputPoints;
	}
	
	
		
	public boolean inRange(int bound1, int bound2, int number){
		//checks if (number) is inside of the range enclosed by bound1 and bound2 (bound1 and bound2 need not be any order)
		if (bound1>bound2){
			return (number <= bound1 && number >= bound2);
		}
		if (bound1<bound2){
			return (number >= bound1 && number <= bound2);
		}
		return false;
		
	}
	
	public Point calculateCenterOfMass(ArrayList<Point> inputPoints){
		Point centerOfMass = new Point(0,0);
		double sumX = 0;
		double sumY = 0;
		for (int k = 0; k<inputPoints.size(); k++){
			sumX = sumX + inputPoints.get(k).getX();
			sumY = sumY + inputPoints.get(k).getY();
		}
		centerOfMass.setLocation(sumX/inputPoints.size(), sumY/inputPoints.size());

		return (centerOfMass);
	}

	public ArrayList<Point> movePointsTowardsCenterOfMass(ArrayList<Point> inputPoints, boolean open, boolean up, ImageProcessor OCT_Image){
		ArrayList <Point> updatedPoints = new ArrayList<Point>();
	
		Point centerOfMass = calculateCenterOfMass(inputPoints);
		if (!open){
			for (int k = 0; k<inputPoints.size(); k++){
	
				boolean isLatched = false;
				
				if (latchedPointIndices.contains(k)){
					isLatched = true;
				}
				
				
				if ((inputPoints.get(k).getX() == centerOfMass.getX() && inputPoints.get(k).getY() == centerOfMass.getY()) || isLatched){
					updatedPoints.add(inputPoints.get(k));
					continue;
				}
				
				updatedPoints.add(interpolatePoint(inputPoints.get(k),centerOfMass,1));
			}
		}else{
			for (int k = 0; k<inputPoints.size(); k++){
				boolean isLatched = false;
				
				if (latchedPointIndices.contains(k)){
					isLatched = true;
				}
				
				
				if (inputPoints.get(k).getY()<=0 || isLatched || inputPoints.get(k).getY() >= OCT_Image.getHeight()-2){
					updatedPoints.add(inputPoints.get(k));
					continue;
				}
				if (up){
					updatedPoints.add(new Point((int) inputPoints.get(k).getX(), (int) inputPoints.get(k).getY()-1));
				}else{
					updatedPoints.add(new Point((int) inputPoints.get(k).getX(), (int) inputPoints.get(k).getY()+1));
				}
			}
		}
			
		
		return (updatedPoints);
		
	}
	
	public boolean returnLatchedPercentage(int curvePointsSize){
		return ((int) (Math.round(( ((double)latchedPointIndices.size()) / ((double) curvePointsSize))*100 ))>99);
		
	}
	
	public ArrayList<Point> activeContourEngine(ArrayList<Point> curvePoints, int roiRadius, ImageProcessor mImage, int numberOfBins, int intensityMax, int intensityMin, double percentageLatchedTarget, double separationCoefficientCriteria, boolean open, boolean wrap, boolean up){
		_parent = ij.WindowManager.getCurrentImage();
		int latchedPointsThresholdNumber = (int) Math.round((percentageLatchedTarget/100) * curvePoints.size());
		float[] imagePixels = (float[]) mImage.getPixels();

		fillInterior(calculateCenterOfMass(curvePoints), curvePoints, open, up, mImage); //should replace with an inputed center point later...	
		//IJ.log("AC: Contour Filled.");
		printMask(mImage);
			for (int i = 0; i < curvePoints.size(); i++){ //goes through every point in curve
				ArrayList<Double> internalIntensities = new ArrayList<Double>();
				ArrayList<Double> externalIntensities = new ArrayList<Double>();
				if (latchedPointIndices.contains(i)){
					continue;
				}
				
				//now going through each point
				//setting up ROI bounds
				int rectXMin = (int) Math.round(curvePoints.get(i).getX()-roiRadius);
				if (rectXMin < 0 && !wrap){ 
					rectXMin = 0; 
				}
				
				int rectXMax = (int) Math.round(curvePoints.get(i).getX()+roiRadius);
				if (rectXMax > mImage.getWidth() && !wrap){ 
					rectXMax = (int) (mImage.getWidth()-1); 
				}
				
				int rectYMin = (int) Math.round(curvePoints.get(i).getY()-roiRadius);
				if (rectYMin < 0){ 
					rectYMin = 0; 
				}
				
				int rectYMax = (int) Math.round(curvePoints.get(i).getY()+roiRadius);
				if (rectYMax > mImage.getHeight()){ 
					rectYMax = (int) (mImage.getHeight()-1); 
				}
				
			
				
				for (int x = rectXMin; x < rectXMax; x++){ //goes through ROI to evaluate every point
					for (int y = rectYMin; y < rectYMax; y++){
						Point refPoint = new Point(x,y);
						if (curvePoints.get(i).distance(refPoint) <= roiRadius){
							//the point is inside the circular ROI
							
							//deal with negative wrap around
							int negative;
							if (x<0){
								negative = 1;
							}else{
								negative = 0;
							}

							if (imageFillMask[y][(int) (x-(x/mImage.getWidth())*mImage.getWidth()+mImage.getWidth()*negative)] == 1){
								//point is inside curve													
								internalIntensities.add((double) imagePixels[mImage.getWidth()*y + (x-(x/mImage.getWidth())*mImage.getWidth()+mImage.getWidth()*negative)]);
							}else{
							//point is outside curve
								externalIntensities.add((double) imagePixels[mImage.getWidth()*y + (x-(x/mImage.getWidth())*mImage.getWidth()+mImage.getWidth()*negative)]);
							}
						}
					} 
				}
				
				//now have internal and external intensities calculated for the curve point, need to make histograms
				int [] internalHist = createHistogram(intensityMin, intensityMax, numberOfBins, internalIntensities);
				int [] externalHist = createHistogram(intensityMin, intensityMax, numberOfBins, externalIntensities);
				double histogramSeparationCoefficient = calculateHistogramSeparation(externalHist, internalHist);
				//System.out.println(histogramSeparationCoefficient);
				if (curvePoints.get(i).getY()<=1 ||(histogramSeparationCoefficient <= separationCoefficientCriteria && internalIntensities.size() != 0 && externalIntensities.size() !=0 && !latchedPointIndices.contains(i) && internalIntensities.size()>0.05*(internalIntensities.size()+externalIntensities.size()) && externalIntensities.size()>0.05*(internalIntensities.size()+externalIntensities.size()))){
					latchedPointIndices.add(i);
					//System.out.println("-----------------------");
				}

		}
		if (latchedPointIndices.size() < latchedPointsThresholdNumber){
			curvePoints = movePointsTowardsCenterOfMass(curvePoints, open, up, mImage);
		}


		return curvePoints;
	}
	
	public ArrayList<Double> createHorizontalSumArray(ImageProcessor OCT_Image, int cropMargin){
		ArrayList<Double> horizontalSum = new ArrayList<Double>();
		double[] pixelArray = (double[]) OCT_Image.getPixels();
		double sum = 0;
		for (int i = cropMargin; i < OCT_Image.getHeight()-cropMargin; i++){
			for (int j = cropMargin; j < OCT_Image.getWidth()-cropMargin; j++){
				sum = sum + (double) pixelArray[OCT_Image.getWidth()*i + j];
			}
			horizontalSum.add(sum);
			sum = 0;
		}
		return horizontalSum;
	}
	
	public ArrayList<Double> createHorizontalSumDiffArray(ArrayList<Double> horizontalSumArray){
		ArrayList<Double> horizontalSumDiffArray = new ArrayList<Double>();
		for (int arrayIndex = 0; arrayIndex < horizontalSumArray.size()-1; arrayIndex++){
			horizontalSumDiffArray.add(horizontalSumArray.get(arrayIndex+1)-horizontalSumArray.get(arrayIndex));
		}
		return horizontalSumDiffArray;
	}
	
	public ArrayList<Double> createHorizontalSumDiffPercentArray(ArrayList<Double> horizontalSumDiffArray){
		ArrayList<Double> horizontalSumDiffPercentArray = new ArrayList<Double>();
		int maxIndex=0;
		for (int arrayIndex = 1; arrayIndex < horizontalSumDiffArray.size(); arrayIndex++){
			if (Math.abs(horizontalSumDiffArray.get(arrayIndex)) > Math.abs(horizontalSumDiffArray.get(maxIndex))){
				maxIndex = arrayIndex;
			}
		}
		
		for (int arrayIndex = 0; arrayIndex < horizontalSumDiffArray.size(); arrayIndex++){
			horizontalSumDiffPercentArray.add(Math.abs(horizontalSumDiffArray.get(arrayIndex)/horizontalSumDiffArray.get(maxIndex)));
		}
				
		return horizontalSumDiffPercentArray;
		
	}
	
	public int determineCeilingHeight(ImageProcessor OCT_Image, int blankSpaceHeight, int cropMargin){
		int ceilHeight = 0;	
		ArrayList<Double> horizontalSumDiffPercentArray = createHorizontalSumDiffPercentArray(createHorizontalSumDiffArray(createHorizontalSumArray(OCT_Image, cropMargin)));
		for (int i = blankSpaceHeight; i > 0; i--){
			if (horizontalSumDiffPercentArray.get(i) > 0.15){
				ceilHeight = i;
				break;
			}
		}
		
		return ceilHeight + 2;
	}
	
	public void resetActiveContour(){
		latchedPointIndices = new ArrayList<Integer>();
		fillCount = 0;
		mouseClickedCount = 0;
		catheterTop = 0;
		catheterBottom = 0;
		blankSpaceHeight = 0;
		_parent.setOverlay(null);
		
	}


	@Override
	public void run(String arg0) {
		switch(arg0){
			case "Reset":
				_parent = ij.WindowManager.getCurrentImage();
				resetActiveContour();
		}
		switch(arg0){
			case "Detect_Tissue":
				
				GenericDialog gd = new GenericDialog("Active Contour Inputs");
				gd.addMessage("Select the options and then click points from left to right");
				gd.addStringField("Mouse Input Clicks", "5");
				gd.addStringField("Points in Curve", "200");
				gd.addStringField("Iteration Limit", "400");
				gd.addStringField("Seperation Criteria", "17");
				gd.addStringField("ROI Radius", "5");
				gd.addCheckbox("Bottom Down?", true);
				gd.addCheckbox("Open Ended?", true);
				gd.showDialog();
				
				if(gd.wasCanceled()){
					return;
				}
				
				int numberOfClicks = 10;
				int iterationLimit = 400;
				double separationCoefficientCriteria = 17;
				int roiRadius = 5;
				boolean up = false;
				boolean open = true;
				int numberOfPoints = 200;
				
				try{
					numberOfClicks = Integer.parseUnsignedInt(gd.getNextString());
					numberOfPoints = Integer.parseUnsignedInt(gd.getNextString());
					iterationLimit = Integer.parseUnsignedInt(gd.getNextString());
					separationCoefficientCriteria = Double.parseDouble(gd.getNextString());
					roiRadius = Integer.parseUnsignedInt(gd.getNextString());
					up = !gd.getNextBoolean();
					open = gd.getNextBoolean();
				}catch(NumberFormatException e){
					IJ.error("Could not parse one or more of the inputs: " + e.getMessage());
					return;
				}

				
				

				
				
				
				_parent = ij.WindowManager.getCurrentImage();
				resetActiveContour();
				_parent.getCanvas().addMouseListener(this);
				_parent.getCanvas().addMouseMotionListener((MouseMotionListener) this);
				_parent.setOverlay(null);
				FloatProcessor OCTimageFloatProcessor = _parent.getProcessor().convertToFloatProcessor();
				//wait till all input received
				while (mouseClickedCount < numberOfClicks){
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				//NOTE conditioning is off for now
				//Conditioning: Removing space above last ghostline to turn into image-specific noise
				//capturing pixels above last ghost line ("blankSpace")
//				float[] imagePixels = (float[]) OCTimageFloatProcessor.getPixels();
//				float[] noiseArray = new float[11*11];
//				int noiseArrayIndex = 0;
				
//				for (int xCoord = (int) (noiseTemplateLocation.getX() - 5); xCoord <= noiseTemplateLocation.getX() + 5; xCoord++){
//					if (xCoord < 0){
//						continue;
//					}
//					for (int yCoord = (int) (noiseTemplateLocation.getY() - 5); yCoord <= noiseTemplateLocation.getY() + 5; yCoord++){
//						if (yCoord < 0){
//							continue;
//						}
//						
//						noiseArray[noiseArrayIndex] = imagePixels[xCoord + yCoord*_parent.getCanvas().getWidth()];
//						noiseArrayIndex++;
//					}
//					
//				}
				
//				float[] aboveGhostlineReplacement = new float[30 * _parent.getCanvas().getWidth()];
//				
//				for (int arrayIndex = 0; arrayIndex < aboveGhostlineReplacement.length; arrayIndex ++){
//					aboveGhostlineReplacement[arrayIndex] = noiseArray[(int) Math.round(Math.random()*(noiseArray.length-1))];
//				}
//				float[] tempArray = new float[imagePixels.length];
//				System.arraycopy(aboveGhostlineReplacement, 0, imagePixels, blankSpaceHeight*_parent.getCanvas().getWidth()-aboveGhostlineReplacement.length + 2*_parent.getCanvas().getWidth(), aboveGhostlineReplacement.length);
//				
//				System.arraycopy(aboveGhostlineReplacement, 0, tempArray, 0, aboveGhostlineReplacement.length);
//				
//				System.out.println(imagePixels.equals(tempArray));
//				System.out.println(imagePixels.equals(OCTimageFloatProcessor.getPixels()));
//				OCTimageFloatProcessor.setPixels(imagePixels);
//				float[] newImagePixels = (float[]) OCTimageFloatProcessor.getPixels();
//				System.out.println(imagePixels.equals(newImagePixels));
				//end of conditioning////////////////////////////////
			
				ArrayList<Point> curvePoints = new ArrayList<Point>();
				curvePoints = createCurve(userInputtedPoints, OCTimageFloatProcessor.getWidth(), numberOfPoints, false, OCTimageFloatProcessor.getHeight(), true, _parent.getCanvas().getImage().getProcessor());					

				//BEGIN AC init find catheter profile
				int numberOfBins = 100;
				int intensityMax = 255;
				int intensityMin = 0;
				double percentageLatchedTarget = 100;
				boolean wrap = true;
				
				//END AC init find catheter profile
				
				while (latchedPointIndices.size() < percentageLatchedTarget*numberOfPoints/100 && fillCount < iterationLimit){
					curvePoints = activeContourEngine(curvePoints, roiRadius, OCTimageFloatProcessor, numberOfBins, intensityMax, intensityMin, percentageLatchedTarget, separationCoefficientCriteria, open, wrap, up);			
					
					Path2D contourPath = new Path2D.Double();
					contourPath.moveTo(curvePoints.get(0).getX(), curvePoints.get(0).getY());
					for (int q = 1; q < curvePoints.size(); q++){
						contourPath.lineTo(curvePoints.get(q).getX(), curvePoints.get(q).getY()); 		
					}
					_parent.setOverlay(contourPath, Color.cyan, new BasicStroke());

				}
								
		}
								
	}
	
	public float[] shuffleArray(float[] inputArray, int numberOfIterations){

		for (int loopCount = 0; loopCount < numberOfIterations; loopCount++){
			int index1 = (int) Math.round((inputArray.length-1)*Math.random());
			int index2 = (int) Math.round((inputArray.length-1)*Math.random());
			float val1 = inputArray[index1];
			float val2 = inputArray[index2];
			
			inputArray[index1] = val2;
			inputArray[index2] = val1;
		}
		
		return inputArray;	
	}
	
	public float[] rowAverage(float[] inputArray, int imageWidth, int numberOfRows){
		float [] averageArray = new float[imageWidth];
		
		for (int i = 0; i < imageWidth; i++){
			float runningSum = 0;
			for (int j = 0; j < numberOfRows; j++){
				runningSum = runningSum + inputArray[i+imageWidth*j];
			}
			averageArray[i] = runningSum / numberOfRows; 
		}
		
		return averageArray;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		mouseClickedCount ++ ;
		IJ.log("AC: Mouse Click Detected.");
		if (_parent.getCanvas().isFocusOwner()){
			IJ.log("AC: Mouse in Focus.");
			if (mouseClickedCount > 10){
				return;
			}
			
			if (mouseClickedCount == 10){
				IJ.log("User Curve Recorded");
				IJ.log("------------");
				noiseTemplateLocation = _parent.getCanvas().getCursorLoc();
				_parent = ij.WindowManager.getCurrentImage();
				_parent.getCanvas().removeMouseListener(this);
				_parent.getCanvas().removeMouseMotionListener((MouseMotionListener) this);
				return;
			}
			
			IJ.log("Point Recorded");
			
			userInputtedPoints.add(_parent.getCanvas().getCursorLoc());

		}
	}


	@Override
	public void mousePressed(MouseEvent e) {
		
	}


	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	

}
