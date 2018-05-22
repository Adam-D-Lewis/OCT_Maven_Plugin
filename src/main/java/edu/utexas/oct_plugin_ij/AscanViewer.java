package edu.utexas.oct_plugin_ij;

import ij.ImagePlus;
import ij.gui.Plot;
import ij.gui.PlotWindow;

import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;


public class AscanViewer implements MouseMotionListener, MouseInputListener{
	ImagePlus _parent;
	float[] _xaxisData;
	float[] _yaxisData;
	int[] _yaxisDataInt;
	Line2D _selector;
	PlotWindow _plotWindow = null;
	
	/**
	 * Creates a new A-Scan Viewer window 
	 * <p>
	 * Creates a new A-Scan Viewer that grabs the depth information 
	 * from the processed data and displays it to a new ImageJ Plot
	 * 
	 * @param parent ImagePlus used for input
	 */
	public AscanViewer(ImagePlus parent) {
		_parent = parent;
		_selector = new Line2D.Float(_parent.getWidth()/2, 0, _parent.getWidth()/2, _parent.getHeight());
		_parent.setOverlay(_selector, Color.YELLOW, new BasicStroke(2));
		_parent.getCanvas().addMouseListener(this);
		_parent.getCanvas().addMouseMotionListener(this);
		
		_xaxisData = new float[_parent.getHeight()];
		_yaxisData = new float[_parent.getHeight()];
		_yaxisDataInt = new int[_parent.getHeight()];
		
		for(int i = 0; i < _parent.getHeight(); i++){
			_xaxisData[i] = i;
		}
		
		_parent.updateAndDraw();
	}
	
	/**
	 * Removes the mouse listeners and closes the plot window
	 */
	public void remove(){
		_parent.getCanvas().removeMouseListener(this);
		_parent.getCanvas().removeMouseMotionListener(this);
		if(_plotWindow != null) {
			_plotWindow.close();
		}
		_parent.getOverlay().clear();
		_parent.updateAndDraw();
	}

	/**
	  * {@inheritDoc}
	  */
	@Override
	public void mouseDragged(MouseEvent e) {
		if(_parent.getCanvas().isFocusOwner()){
			_parent.getProcessor().getColumn(_parent.getCanvas().getCursorLoc().x, 0, _yaxisDataInt, _yaxisDataInt.length);
			for(int i = 0; i < _yaxisDataInt.length; i++){
				_yaxisData[i] = _yaxisDataInt[i];
			}
			
			Plot p = new Plot("A-Scan", "Pixel in Depth", "Magnitude (scaled)", _xaxisData, _yaxisData);
			
			if(_plotWindow == null || _plotWindow.isClosed()){
				_plotWindow = p.show();
			}
			
			_plotWindow.drawPlot(p);
			_selector = new Line2D.Float(_parent.getCanvas().getCursorLoc().x, 0,_parent.getCanvas().getCursorLoc().x, _parent.getHeight());
			_parent.setOverlay(_selector, Color.YELLOW, new BasicStroke(2));
		}		
	}

	/**
	  * {@inheritDoc}
	  */
	@Override
	public void mousePressed(MouseEvent e) {

	}
	
	/**
	  * {@inheritDoc}
	  */
	@Override
	public void mouseReleased(MouseEvent e) {

	}

	/**
	  * {@inheritDoc}
	  */
	@Override
	public void mouseEntered(MouseEvent e) {

	}

	/**
	  * {@inheritDoc}
	  */
	@Override
	public void mouseExited(MouseEvent e) {

	}

	/**
	  * {@inheritDoc}
	  */
	@Override
	public void mouseMoved(MouseEvent e) {

	}

	/**
	  * {@inheritDoc}
	  */
	@Override
	public void mouseClicked(MouseEvent arg0) {
		
	}	
}
