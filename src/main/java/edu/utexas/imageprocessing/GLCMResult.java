package edu.utexas.imageprocessing;

public class GLCMResult {
	float[] Data;
	String Label;
	int Width;
	int Height;
	
	public GLCMResult(String Label, float[] Data, int Width, int Height){
		this.Label = Label;
		this.Data = Data;
		this.Height = Height;
		this.Width = Width;
	}
	
	public float[] getData(){
		return Data;
	}
	
	public String getLabel(){
		return Label;
	}
	
	public int getWidth(){
		return Width;
	}
	
	public int getHeight(){
		return Height;
	}
}
