package edu.utexas.primitives;

public class Tuples {
	public static class Quartet<X, Y, Z, W> {
		private final X x;
		private final Y y;
		private final Z z;
		private final W w;
		public Quartet(X xx, Y yy, Z zz, W ww){
			x = xx;
			y = yy;
			z = zz;
			w = ww;
		}
		
		public X x(){
			return x;
		}
		
		public Y y(){
			return y;
		}
		
		public Z z(){
			return z;
		}
		
		public W w(){
			return w;
		}		
	}
	
	public static class Triplet<X, Y, Z> {
		private final X x;
		private final Y y;
		private final Z z;
		public Triplet(X xx, Y yy, Z zz){
			x = xx;
			y = yy;
			z = zz;
		}
		
		public X x(){
			return x;
		}
		
		public Y y(){
			return y;
		}
		
		public Z z(){
			return z;
		}		
	}
	
	public static class Doublet<X, Y> {
		private final X x;
		private final Y y;
		public Doublet(X xx, Y yy){
			x = xx;
			y = yy;
		}
		
		public X x(){
			return x;
		}
		
		public Y y(){
			return y;
		}
	
	}
}
