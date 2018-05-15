package edu.utexas.segmentation;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import ij.ImagePlus;
import ij.WindowManager;
import ij.IJ;
import ij.ImageListener;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;


public class RemoveGuidewireAndCatheter implements MouseListener, PlugIn {

	int catheterBound = 0;
	int guidewireLeftBound = 0;
	int guidewireRightBound = 0;
	int numberClicks = 0;
	
	
	public void run(String arg0) {
		numberClicks = 0;
		
		GenericDialog gd = new GenericDialog("Remove Guide Wire and Catheter");
		gd.addMessage("Click minima of catheter, then click far left edge of guide wire, then click far right edge of guide wire");
		gd.showDialog();
		
		boolean removeGWACMLExists = false;
		MouseListener[] mllist = IJ.getImage().getCanvas().getMouseListeners();
		for(MouseListener ml : mllist){
			if(ml.getClass() == this.getClass()){
				removeGWACMLExists = true;
			}
		}
		
		
		
		if(!removeGWACMLExists){
			IJ.getImage().getCanvas().addMouseListener(this);	
		}
	}
	
	
	@Override
	public void mouseClicked(MouseEvent e) {
		numberClicks += 1;
		
		ImagePlus ip = IJ.getImage();
		Point p = ip.getCanvas().getCursorLoc();
		
		if (numberClicks == 1) {
			catheterBound = p.y;
			GenericDialog gd = new GenericDialog(" ");
			gd.addMessage("Height of catheter is " + catheterBound);
			gd.showDialog();			
			drawRoi1();
			clearRoi1();			
		} else if (numberClicks == 2) {
			guidewireLeftBound = p.x;
			GenericDialog gd = new GenericDialog(" ");
			gd.addMessage("Left boundary of guide wire is " + guidewireLeftBound);
			gd.showDialog();			
		} else if (numberClicks == 3) {
			guidewireRightBound = p.x;
			GenericDialog gd = new GenericDialog(" ");
			gd.addMessage("Right boundary of guide wire is " + guidewireRightBound);
			gd.showDialog(); 			
			
			drawRoi2();
			clearRoi2();
			
			IJ.getImage().getCanvas().removeMouseListener(this);	//numberClicks = 0;
			
			IJ.getImage().deleteRoi();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
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
	
	void drawRoi1() {
		
		double height = catheterBound;
		double width = IJ.getImage().getWidth();
		double x = 0;
		double y = 0;

		Roi roiCatheter1 = new Roi(x, y, width, height);
		IJ.getImage().setRoi(roiCatheter1);
	
}
	void clearRoi1() {
		ImagePlus imp = getImage();
		IJ.setBackgroundColor(0, 0, 0);
		IJ.run(imp, "Clear", "stack");
		
	}
	
	private ImagePlus getImage() {
		// TODO Auto-generated method stub
		return null;
	}


	void drawRoi2() {
		
		double height = IJ.getImage().getHeight();
		double width = Math.abs(guidewireLeftBound - guidewireRightBound);
		double x = guidewireLeftBound;
		double y = 0;

		Roi roiCatheter2 = new Roi(x, y, width, height);
		IJ.getImage().setRoi(roiCatheter2);
		
}
	void clearRoi2() {
		ImagePlus imp = getImage();
		IJ.setBackgroundColor(0, 0, 0);
		IJ.run(imp, "Clear", "stack");
		
	}
}