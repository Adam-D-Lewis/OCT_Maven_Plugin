package edu.utexas.segmentation;

import java.util.ArrayList;
import java.util.List;

import edu.utexas.math.Histogram;
import edu.utexas.math.P2;
import edu.utexas.math.V2;

public class Region {
	float[] Pixels;
	
	float[] r1;	
	float[] r2;
	
	float[] mask1;
	float[] mask2;
	
	Histogram _h;
	
	P2 Offset;
	V2 Center;
	
	int Radius;
	
	List<P2> _ContourPoints;
	
	double distanceFromContour = 0;
	
	public Region(float[] pix, int radius, V2 center){
		this.Offset = center.getStart();
		this.Pixels = pix;
		this.Radius = radius;
		this.Center = center;
	}

	public float[] getPixels(){
		return Pixels;
	}
	
	private List<P2> interpolate(V2 start, V2 end){
		List<P2> newPoints = new ArrayList<P2>();
		V2 newVector = new V2(start.getStart(), end.getStart());
		double mag = newVector.magnitude();
		double diffMag = mag - 0;
		while(diffMag < Radius && diffMag > 1){
			P2 interp = newVector.getPointOnVector(.5);
			diffMag = mag - interp.distanceFrom(start.getStart());
			newPoints.add(interp);
			newVector = new V2(interp, end.getStart());
		}
		return newPoints;
	}
	
	public List<P2> contourInRegions(List<V2> contour){
		_ContourPoints = new ArrayList<P2>();

		int index = 0;
		for(index = 0; index < contour.size(); index++){
			if(contour.get(index).getStart().equals(Offset)){
				break;
			}
		}
		
		//Add the start point
		_ContourPoints.add(contour.get(index).getStart());
		
		P2 startPos = _ContourPoints.get(0);
		P2 startNeg = _ContourPoints.get(0);
		int i = 0;
		P2 posEnd = startPos;
		P2 negEnd = startNeg;
		
		int timeOut = 0;
		
		double minXCoord = Double.MAX_VALUE;
		double maxXCoord = Double.MIN_VALUE;
		double minYCoord = 0;
		double maxYCoord = 0;
		
		//Add all the points that are in the region
		while(_ContourPoints.get(0).distanceFrom(startPos) < Radius || _ContourPoints.get(0).distanceFrom(startNeg) < Radius){
			int pos = index + i;
			int neg = index - i;
			
			if(neg > Radius && pos < contour.size() - Radius){

				V2 posNext = contour.get(pos + 1);
				V2 negPrior = contour.get(neg - 1);

				V2 v = new V2(startPos, posNext.getStart());
				P2 np = v.getPointOnVector(.5);
				startPos = np;
				posEnd = np.add(new P2(Radius, Radius)).subtract(Offset);
				posEnd = posEnd.bound(0, 2*Radius + 1, 0, 2*Radius + 1);
				_ContourPoints.add(posEnd);
				if(posEnd.x() > maxXCoord){
					maxXCoord = posEnd.x();
					maxYCoord = posEnd.y();
				}

				v = new V2(startNeg, negPrior.getStart());
				np = v.getPointOnVector(.5);
				startNeg = np;
				negEnd = np.add(new P2(Radius, Radius)).subtract(Offset);
				negEnd = negEnd.bound(0, 2*Radius + 1, 0, 2*Radius + 1);
				_ContourPoints.add(negEnd);
				if(negEnd.x() < minXCoord){
					minXCoord = negEnd.x();
					minYCoord = negEnd.y();
				}
				
				distanceFromContour += _ContourPoints.get(0).distanceFrom(posNext.getStart());
				distanceFromContour += _ContourPoints.get(0).distanceFrom(negPrior.getStart());	
				i +=1;
			}else{
				timeOut += 1;
				if(timeOut > Radius){
					break;
				}
			}
		}		
		
		//Ensure that the contour leaves left to right
		for(int x = 0; x < minXCoord; x++){
			_ContourPoints.add(new P2(x, minYCoord));
		}
		
		for(int x = (int) maxXCoord; x < 2*Radius + 1; x++){
			_ContourPoints.add(new P2(x, maxYCoord));
		}
		
		
		distanceFromContour /= _ContourPoints.size();		
		
		return _ContourPoints;
	}
	
	public double getDistanceFromContour(){
		return distanceFromContour;
	}
	
	public boolean inBounds(P2 p){
		if(p.x() < Offset.x() - Radius || p.x() > Offset.x() + Radius ||
				p.y() < Offset.y() - Radius || p.y() > Offset.y() + Radius){
			return false;
		}else{
			return true;
		}
	}
	
	public float[] getR1(){
		return r1;
	}
	
	public float[] getR2(){
		return r2;
	}
	
	public float[] getMask1(){
		return mask1;
	}
	
	public float[] getMask2(){
		return mask2;
	}
	
	private void _bisect(P2 p){
		
	}
	
	public void bisect(){
		r1 = new float[Pixels.length];
		r2 = new float[Pixels.length];
		
		mask1 = new float[Pixels.length];
		mask2 = new float[Pixels.length];

		V2 v_cont1 = new V2(_ContourPoints.get(_ContourPoints.size() - 1), _ContourPoints.get(0));
		
		double angle = v_cont1.theta();
		
		boolean interpX = true;
		if(-Math.PI/2 < angle && angle < Math.PI/2){
			interpX = false;
		}		
		
		int w = 2*Radius + 1;
		int RSquared = Radius*Radius;

		for(P2 p : _ContourPoints){
			int yi = (int)p.y();
			int yend = (2*Radius + 1);

			if(interpX){
				int y = yi;
				int yminradSquared = (y - Radius)*(y - Radius);
				boolean yInBounds = (y >= 0 && y < w);
				
				for(int x = (int)p.x(); x < w ; x++){					
					//if(yInBounds && x >= 0 && x < w){						
						if((x - Radius)*(x - Radius) + yminradSquared <= RSquared){
							mask1[y*w + x] = 1;
						}
					//}
				}
				
				for(int x = 0; x < (int)p.x(); x++){					
					//if(yInBounds && x >= 0 && x < w){						
						if((x - Radius)*(x - Radius) + yminradSquared <= RSquared){
							mask2[y*w + x] = 1;
						}
					//}
				}
			}else{
				int x = (int)p.x();
				int xminradsquared = (x - Radius)*(x - Radius);
				boolean xInBounds = (x >= 0 && x < w);
				
				for(int y = yi; y < w; y++){				
					if(y >= 0 && y < w && xInBounds){						
						if(xminradsquared + (y - Radius)*(y - Radius) <= RSquared){
							mask1[y*w + x] = 1;
						}	
					}
				}
				
				for(int y = 0; y < yi; y++){				
					if(y >= 0 && y < w && xInBounds){						
						if(xminradsquared + (y - Radius)*(y - Radius) <= RSquared){
							mask2[y*w + x] = 1;
						}	
					}
				}
			}
		}	
		
		for(int i = 0; i < r1.length; i++){
			if(mask1[i] > 0){
				r1[i] = Pixels[i];
			}
			
			if(mask2[i] > 0){
				r2[i] = Pixels[i];
			}
		}		
	}
}
