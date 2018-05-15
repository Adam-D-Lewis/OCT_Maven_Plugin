package edu.utexas.math;

import java.util.ArrayList;
import java.util.List;

public class P2 {
	double _x;
	double _y;
	
	public P2(double x, double y){
		_x = x;
		_y = y;
	}
	
	public boolean equals(Object obj) {
		if(obj == null){
			return false;
		}
		
		if(obj.getClass() != P2.class){
			return false;
		}
		
		P2 o = (P2)obj;
		if(o.x() == _x && o.y() == _y){
			return true;
		}else{
			return false;
		}
	}
	
	public boolean equalsX(double X){
		if(_x == X){
			return true;
		}else{
			return false;
		}			
	}
	
	public boolean equalsY(double Y){
		if(_y == Y){
			return true;
		}else{
			return false;
		}			
	}
	
	public double x(){
		return _x;
	}
	
	public double y(){
		return _y;
	}
	
	public P2 bound(double xMin, double xMax, double yMin, double yMax){
		if(_x > xMax){
			_x = xMax;
		}
		
		if(_x < xMin){
			_x = xMin;
		}
		
		if(_y > yMax){
			_y = yMax;
		}
		
		if(_y < yMin){
			_y = yMin;
		}
		
		return new P2(_x, _y);
	}
	
	public double distanceFrom(P2 p){
		return Math.sqrt(Math.pow((p.x() - _x), 2) + Math.pow((p.y() - _y), 2));
	}
	
	public P2 subtract(P2 p){
		return new P2(_x - p.x(), _y - p.y());
	}
	
	public P2 add(P2 p){
		return new P2(_x + p.x(), _y + p.y());
	}
	
	public List<P2> getSurroundingPoints(){
		List<P2> n = new ArrayList<P2>();
		
		n.add(this.add(new P2(0, 0)));
		n.add(this.add(new P2(0, 1)));
		n.add(this.add(new P2(0, -1)));
		n.add(this.add(new P2(-1, 0)));
		n.add(this.add(new P2(-1, -1)));
		n.add(this.add(new P2(-1, 1)));
		n.add(this.add(new P2(1, 1)));
		n.add(this.add(new P2(1, 0)));
		
		return n;
	}
}
