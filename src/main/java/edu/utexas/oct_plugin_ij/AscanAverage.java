package edu.utexas.oct_plugin_ij;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;

import javax.swing.event.MouseInputListener;

/**
 * Class that displays 2 sliders on an ImagePlus window and displays the average<br>
 * of a range of depth information from the processed data between the sliders.
 * The A-Scan Average is displayed in a new window.
 * 
 * @author Anthony Hsu
 * @version 0.1.0
 */
public class AscanAverage implements MouseMotionListener, MouseInputListener{
	ImagePlus _parent;
	float[] _xaxisData;
	float[] _yaxisData = {0.0f};
	int[] _yaxisDataInt;
	Line2D _selector;
	PlotWindow _plotWindow = null;
	
	Line _l1;
	Line _l2;
	Line _selectedLine;
	int _startWidth;
	int _endWidth;
	ImagePlus _enFaceImagePlus;
	float[] _output;
	
	/**
	 * Creates a new A-Scan Average Viewer window 
	 * <p>
	 * Creates a new A-Scan Average Viewer that grabs the depth information 
	 * from the processed data and displays it to a new ImageJ Plot
	 * 
	 * @param parent ImagePlus used for input
	 */
	public AscanAverage(ImagePlus parent) {
		_parent = parent;
		_parent.getCanvas().addMouseListener(this);
		_parent.getCanvas().addMouseMotionListener(this);
		
		_xaxisData = new float[_parent.getHeight()];
		_yaxisData = new float[_parent.getHeight()];
		_yaxisDataInt = new int[_parent.getHeight()];
		
		for(int i = 0; i < _parent.getHeight(); i++){
			_xaxisData[i] = i;
		}
		
		//
		
		int width = _parent.getWidth();
		int height = _parent.getHeight();
		
		Line.setWidth(3);
		_l1 = new Line(width/2 + 30, height, width/2 + 30, 0);
		_l2 = new Line(width/2, height, width/2, 0);
		
		Overlay o = new Overlay();
		o.add(_l1);
		o.add(_l2);

		_parent.setOverlay(o);
		
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
	  * When the mouse is dragged after clicking on a line, the line is dragged with the cursor.
	  */
	@Override
	public void mouseDragged(MouseEvent e) {
		if(_selectedLine != null){
			
			_startWidth = Math.min(_l1.x1, _l2.x1);
			_endWidth = Math.max(_l1.x1, _l2.x1);
			_selectedLine.setLocation(_parent.getCanvas().getCursorLoc().x, 0);
			_parent.updateAndDraw();
			
		}		

	}

	/**
	 * Selects the nearest slider within +/- 10 pixels
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
		int x = _parent.getCanvas().getCursorLoc().x;
		
		if(x > _l1.x1 - 10 && x < _l1.x1 + 10){
			_selectedLine = _l1;
		}else if(x > _l2.x1 - 10 && x < _l2.x1 + 10){
			_selectedLine = _l2;
		}else{
			_selectedLine = null;
		}
		
	}
	
	/**
	 * When mouse is released, calculate average values for the A-scan.
	 * Plot the average values.
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		//case for if the selected line is on the right
		if(_selectedLine.x1 == Math.max(_l1.x1, _l2.x1)) {
			//for-loop to go across the x-axis.
			for(int j = Math.max(_l1.x1, _l2.x1); j >= Math.min(_l1.x1, _l2.x1); j--) {
				//for each x-coordinate, new set of data.
				//for-loop to add up the yaxisData.
				_parent.getProcessor().getColumn(j, 0, _yaxisDataInt, _yaxisDataInt.length);
				for(int i = 0; i < _yaxisDataInt.length; i++){
					_yaxisData[i] += _yaxisDataInt[i];
				}
			}
		}
		
		//case for if the selected line is on the left
		else {
			for(int j = Math.min(_l1.x1, _l2.x1); j <= Math.max(_l1.x1, _l2.x1); j++) {
				//for each x-coordinate, new set of data.
				//for-loop to add up the yaxisData.
				_parent.getProcessor().getColumn(j, 0, _yaxisDataInt, _yaxisDataInt.length);
				for(int i = 0; i < _yaxisDataInt.length; i++){
					_yaxisData[i] += _yaxisDataInt[i];
				}
			}
		}
		
		//average the _yaxisData.
		for(int i = 0; i < _yaxisDataInt.length; i++) {
			_yaxisData[i] = (float) _yaxisData[i] / (Math.max(_l1.x1, _l2.x1) - Math.min(_l1.x1, _l2.x1) + 1);
		}
		
		//Plot the _yaxisData
		Plot p = new Plot("A-Scan", "Pixel in Depth", "Magnitude (scaled)", _xaxisData, _yaxisData);
		if(_plotWindow == null || _plotWindow.isClosed()){
			_plotWindow = p.show();
		}
		_plotWindow.drawPlot(p);
		
	}

	/**
	  * {@inheritDoc}
	  */
	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	/**
	  * {@inheritDoc}
	  */
	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	/**
	  * {@inheritDoc}
	  */
	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	/**
	  * {@inheritDoc}
	  */
	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}	
}
