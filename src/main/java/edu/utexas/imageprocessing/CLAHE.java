package edu.utexas.imageprocessing;

public class CLAHE {

	int Width;
	
	int Height;
	
	int Depth;	
	
	int X_Size = 64;
	
	int Y_Size = 64;
	
	int Z_Size = 64;
	
	int X_Spacing;
	
	int Y_Spacing;
	
	int Z_Spacing;
	
	float[][][] ImageIn;
	
	float[][][] ImageOut;
	
	public CLAHE(float[][][] Image, int width, int height, int depth){
		ImageIn = Image;
		Width = width;
		Height = height;
		Depth = depth;
	}
	
	public void bake(){
		X_Spacing = Width / X_Size;
		Y_Spacing = Height / Y_Size;
		Z_Spacing = Depth / Z_Spacing;
		
		ImageOut = new float[Width][Height][Depth];
	}
	
	public void process(){
		
	}
}
