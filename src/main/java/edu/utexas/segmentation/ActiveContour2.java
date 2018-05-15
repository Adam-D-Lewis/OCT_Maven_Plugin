package edu.utexas.segmentation;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

import edu.utexas.exceptions.PointNotFoundException;
import edu.utexas.math.P2;
import edu.utexas.math.V2;
import ij.ImagePlus;
import ij.process.FloatProcessor;

public class ActiveContour2{
	private List<V2> _CurrentCurve;
	
	float[] _Image;
	float[] _Mask;
	int Width;
	int Height;
	int MaskRadius;
	DIRECTION direction;
	
	double histogramDiffMedStop = 10;
	double histogramDiffEntropyStop = 100;
	
	private List<P2> _LUT;
	
	public enum DIRECTION{
		TOP_DOWN,
		BOTTOM_UP,
		LEFT_RIGHT,
		RIGHT_LEFT,
	}
	
	public ActiveContour2(){
		
	}
	
	public void init(P2 seed, DIRECTION d, int NumberOfPoints, int MaskRadius, float[] Image, int ImageWidth, int ImageHeight){
		_CurrentCurve = new ArrayList<V2>();
		
		_Image = Image;
		this.Width = ImageWidth;
		this.Height = ImageHeight;
		this.MaskRadius = MaskRadius;
		this.direction = d;
		
		_CurrentCurve.clear();
		
		for(int i = 0; i < NumberOfPoints; i++){
			double xstart = 0;
			double ystart = 0;
			
			double xend = 0;
			double yend = 0;
			
			switch(d){
			case BOTTOM_UP:
				ystart = seed.y();
				xstart = i*ImageWidth/NumberOfPoints;
				
				yend = 0;
				xend = i*ImageWidth/NumberOfPoints;
				
				_CurrentCurve.add(new V2(new P2(xstart, ystart), new P2(xend, yend)));
				break;
			case LEFT_RIGHT:
				xstart = seed.x();
				ystart = i*ImageHeight/NumberOfPoints;
				
				yend = i*ImageHeight/NumberOfPoints;
				xend = Width;
				
				_CurrentCurve.add(new V2(new P2(xstart, ystart), new P2(xend, yend)));	
				break;
			case RIGHT_LEFT:
				xstart = seed.x();
				ystart = i*ImageHeight/NumberOfPoints;
		
				yend = i*ImageHeight/NumberOfPoints;
				xend = 0;
				
				_CurrentCurve.add(new V2(new P2(xstart, ystart), new P2(xend, yend)));				
				break;
			case TOP_DOWN:
				ystart = seed.y();
				xstart = i*ImageWidth/NumberOfPoints;

				yend = Height;
				xend = i*ImageWidth/NumberOfPoints;
				
				_CurrentCurve.add(new V2(new P2(xstart, ystart), new P2(xend, yend)));
				break;
			default:
				break;			
			}
		}
		
		_LUT = new ArrayList<P2>();
		_Mask = new float[(MaskRadius*2 + 1)*(MaskRadius*2 + 1)];
		for(int y = -MaskRadius; y <= MaskRadius; y++){
			for(int x = -MaskRadius; x <= MaskRadius; x++){
				float m = 0;
				
				if(Math.sqrt(x*x + y*y) <= MaskRadius){
					m = 1;
				}
				
				_Mask[(y + MaskRadius)*(MaskRadius*2 + 1) + x + MaskRadius] = m;
				_LUT.add(new P2(x, y));
			}
		}	
	}
	
	public List<P2> getPointsInRange(int start, int stop) throws ArrayIndexOutOfBoundsException{
		if(start < 0 || stop > _CurrentCurve.size() - 1){
			throw new ArrayIndexOutOfBoundsException();
		}else{
			if(start == stop || start > stop){
				System.out.println("ActiveContour2::getPointsInRange - Make sure Start < Stop");
				return getContourPoints();
			}else{
				List<P2> p = getContourPoints();
				return p.subList(start, stop);
			}			
		}		
	}
	
	public float[] getMask(){
		return _Mask;
	}
	
	public int getMaskRadius(){
		return MaskRadius;
	}
	
	public P2 findPoint(int X) throws PointNotFoundException{
		for(V2 v : _CurrentCurve){
			if(v.getStart().equalsX(X)){
				return v.getStart();
			}
		}
		
		throw new PointNotFoundException();
	}
	
	public List<V2> getContourVector(){
		return _CurrentCurve;
	}
	
	public List<P2> getContourPoints(){
		List<P2> points = new ArrayList<P2>();
		for(V2 v : _CurrentCurve){
			points.add(v.getStart());
		}
		return points;
	}

	public List<P2> getTrimmedContourPoints(P2 offset, int Trim){
		List<P2> points = new ArrayList<P2>();
		for(int i = Trim; i < _CurrentCurve.size() - 1 - Trim; i++){
			P2 t = _CurrentCurve.get(i).getStart().subtract(offset);
			points.add(t);			
		}
		return points;
	}
	
	public List<P2> getContourPoints(P2 offset){
		List<P2> points = new ArrayList<P2>();
		for(V2 v : _CurrentCurve){
			P2 t = v.getStart().subtract(offset);
			points.add(t);			
		}
		return points;
	}
	
	public boolean inBounds(P2 p){
		if(p.x() <= 0 + MaskRadius || p.x() >= this.Width - MaskRadius 
				|| p.y() <= 0 + MaskRadius || p.y() >= this.Height - MaskRadius){
			return false;
		}else{
			return true;
		}
	}
	
	private float getMaskValueAt(int x, int y){
		return _Mask[y*(2*MaskRadius + 1) + x];
	}
	
	private float getPixelAt(int x, int y){
		return _Image[y*Width + x];
	}
	
	public Region createRegion(V2 v){
		//TODO: Parallelize, make X, Y LUT
		if(inBounds(v.getStart())){
			float[] masked = new float[_Mask.length];
			
			for(P2 p : _LUT){
				int x = (int)p.x();
				int y = (int)p.y();
				float val = getPixelAt((int)v.getStart().x() + x, (int)v.getStart().y() + y); 
				float mask = getMaskValueAt(x + MaskRadius, y + MaskRadius);
				masked[(y + MaskRadius)*(2*MaskRadius + 1) + x + MaskRadius] = mask*val;
			}
			
//			for(int y = -MaskRadius; y <= MaskRadius; y++){
//				for(int x = -MaskRadius; x <= MaskRadius; x++){
//					float val = getPixelAt((int)v.getStart().x() + x, (int)v.getStart().y() + y); 
//					float mask = getMaskValueAt(x + MaskRadius, y + MaskRadius);
//					masked[(y + MaskRadius)*(2*MaskRadius + 1) + x + MaskRadius] = mask*val;
//				}
//			}

			return new Region(masked, MaskRadius, v);
		}else{
			return null;
		}
	}
	
	public void testCurveContinuity(int range){		
	
	}
	
	public void setHistogramMedStop(double stop){
		histogramDiffMedStop = stop;
	}
	
	public void setHistogramEntStop(double stop){
		histogramDiffEntropyStop = stop;
	}
	
	public void run(V2 v){
		if(!v.isStopped()){
			Region r = createRegion(v);

			if(r == null){
				P2 newPoint = v.getPointOnVector(1);
				v.updateVector(newPoint);
			}else{
				r.contourInRegions(_CurrentCurve);

				r.bisect();
				
				RegionStatistics rs = new RegionStatistics(r);

				//if(rs.getHistogramDiffMean() < histogramDiffMedStop){// < 700 && rs.getDiffMean() < 700){
				if(rs.getHistogramDiffMean() < histogramDiffMedStop){//*MaskRadius/(r.getDistanceFromContour())){
						//|| rs.getDiffEntropyR1R2() < histogramDiffEntropyStop){
							v.updateVector(v.getPointOnVector(1));
				}else{
					v.incrementAnchorCounter();
				}
			}
		}
	}
	
	
	public void updateCurve(){
		//Updates the edge points regardless of histograms
		for(int i = 0; i < MaskRadius; i++){
			V2 v = _CurrentCurve.get(i);
			v.updateVector(v.getPointOnVector(1));
			V2 b = _CurrentCurve.get(_CurrentCurve.size() - i - 1);
			b.updateVector(b.getPointOnVector(1));
		}
		
		List<V2> validCurve = _CurrentCurve.subList(MaskRadius, _CurrentCurve.size() - MaskRadius);

		validCurve.parallelStream().forEach((v) -> { run(v); });	
	}
}	

