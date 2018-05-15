package edu.utexas.oct_plugin_ij;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.event.MouseInputListener;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.PlugIn;

/***
 * Crops an Image or Image Stack based a single mouse click. The image is cropped such that all pixels
 * above the mouse click are deleted.
 * 
 * @author Austin McElroy
 * @version 0.1
 *
 */
public class QuickCropPlugin implements MouseInputListener, PlugIn{

	@Override
	public void mouseClicked(MouseEvent e) {
		
		ImagePlus ip = IJ.getImage();
		Point p = ip.getCanvas().getCursorLoc();
		
		Roi roi = new Roi(0, p.getY(), ip.getWidth(), ip.getHeight() - p.getY());
		IJ.getImage().setRoi(roi);
		ip.setRoi(roi);
		
		IJ.run("Crop");
		
		for(MouseListener ml : ip.getCanvas().getMouseListeners()){
			if(ml.equals(this)){
				ip.getCanvas().removeMouseListener(this);
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
	
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	
	}

	@Override
	public void mouseExited(MouseEvent e) {
	
	}

	@Override
	public void mouseDragged(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	public void run(String arg0) {
		ImagePlus ip = IJ.getImage();
		ip.getCanvas().addMouseListener(this);
	}

}
