package edu.utexas.math;

public class V2 {
	private P2 _start;
	
	private P2 _end;
	
	private double vector;
	
	private double _theta;
	
	private boolean _stop;

	private int _vectorAnchorCounter = 0;
	
	public V2(P2 start, P2 end){
		_start = start;
		_end = end;
		vector = (_end.y() - _start.y())/(_end.x() - _start.x());
		
		_theta = theta();
		
		_stop = false;
	}
	
	public void stop(){
		_stop = true;
	}
	
	public void start(){
		_vectorAnchorCounter = 0;
		_stop = false;
	}
	
	public boolean isStopped(){
		return _stop;
	}
	
	public double magnitude(){
		return Math.sqrt(Math.pow((_start.x() - _end.x()), 2) + Math.pow((_start.y() - _end.y()), 2));
	}
	
	public double theta(){
		return Math.atan2(_end.y() - _start.y(), _end.x() - _start.x());
	}
	
	public P2 getStart(){
		return _start;
	}
	
	public P2 getEndPoint(){
		return _end;
	}
	
	public P2 getPointOnVector(double distance){
		double n = distance;
		P2 newPoint = new P2(_start.x(), _start.y());
		P2 scaledVec = new P2(distance*Math.cos(_theta), distance*Math.sin(_theta));
		return newPoint.add(scaledVec);
	}
	
	public void incrementAnchorCounter(){
		_vectorAnchorCounter += 1;
		if(_vectorAnchorCounter > 15){
			stop();
		}
	}
	
	public V2 updateVector(P2 p){
		_vectorAnchorCounter = 0;
		_start = p;
		return this;
	}
}
