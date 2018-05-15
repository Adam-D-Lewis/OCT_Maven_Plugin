package edu.utexas.oct_plugin_ij;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.StackProcessor;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.swing.event.MouseInputListener;

import edu.utexas.math.Math1D;

/**
 * Class that displays 2 sliders on an ImagePlus window and computes the En Face <br>
 * image between the two sliders which is shown in a new ImagePlus window
 * 
 * @author Austin McElroy
 * @version 0.1.0
 */
public class EnFaceViewer implements MouseInputListener, MouseMotionListener{
	ImagePlus _parent;
	Line _l1;
	Line _l2;
	Line _selectedLine;
	int _startDepth;
	int _endDepth;
	ImagePlus _enFaceImagePlus;
	float[] _output;
	
	/**
	 * Draws 2 sliders bars and registers new mouse events for the Parent ImagePlus window. <br>
	 * EnfaceViewer.remove() should be called to properly clean-up this object.
	 * 
	 * @param Parent
	 * @see remove()
	 */
	public EnFaceViewer(ImagePlus Parent){
		_parent = Parent;
		_parent.getCanvas().addMouseListener(this);
		_parent.getCanvas().addMouseMotionListener(this);
		
		int width = _parent.getWidth();
		int height = _parent.getHeight();
		
		Line.setWidth(3);
		_l1 = new Line(0, height/2 + 30, width, height/2 + 30);
		_l2 = new Line(0, height/2, width, height/2);
		
		Overlay o = new Overlay();
		o.add(_l1);
		o.add(_l2);

		_parent.setOverlay(o);
	}
	
	/**
	 * Removes the registered mouse listeners and clears the parent ImagePlus window sliders.
	 */
	public void remove(){
		_parent.getCanvas().removeMouseMotionListener(this);
		_parent.getCanvas().removeMouseListener(this);
		_parent.getOverlay().clear();
		_parent.updateAndDraw();
		if(_enFaceImagePlus != null){
			_enFaceImagePlus.close();
		}
	}
	
	/**
	 * Processes the parent ImagePlus data in parallel to derive an En Face window
	 * 
	 * @throws InterruptedException
	 */
	private void computeEnFace() throws InterruptedException{
		//See: http://stackoverflow.com/questions/17463224/parallelize-for-loops-in-java
		
		final int depth = _parent.getStackSize();
		final int width = _parent.getWidth();
		
		final float[] tmp = new float[depth*width];
		_output = new float[depth*width];
		
		//Loop through OCT frames
		ExecutorService es = Executors.newFixedThreadPool(4);
		for(int i = 1; i <= depth; i++){
			final int currentFrame = i;
			es.submit(new Runnable(){
				@Override
				public void run() {
					//Get current frame
					ImageProcessor ip = _parent.getStack().getProcessor(currentFrame);

					int[] col = new int[_endDepth - _startDepth];
					
					//Loop through a-scans
					for(int x = 0; x < width; x++){						
						ip.getColumn(x, _startDepth, col, _endDepth - _startDepth);	
						float sum = IntStream.of(col).sum();
						sum /= _endDepth - _startDepth;
						tmp[width*currentFrame + x] = sum;
					}					
				}				
			});
		}
		es.shutdown();
		es.awaitTermination(1000, TimeUnit.MILLISECONDS);
		
		_output = tmp.clone();
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Selects the nearest slider within +/- 10 pixels
	 */
	@Override
	public void mousePressed(MouseEvent arg0) {
		int y = _parent.getCanvas().getCursorLoc().y;
		
		if(y > _l1.y1 - 10 && y < _l1.y1 + 10){
			_selectedLine = _l1;
		}else if(y > _l2.y1 - 10 && y < _l2.y1 + 10){
			_selectedLine = _l2;
		}else{
			_selectedLine = null;
		}
	}

	/**
	 * Computes a new En Face value for the update slider settings.
	 */
	@Override
	public void mouseReleased(MouseEvent arg0) {
		_selectedLine = null;
		
		final int depth = _parent.getStackSize();
		final int width = _parent.getWidth();
		
		FloatProcessor fp = new FloatProcessor(width, depth, _output);
		
		if(_enFaceImagePlus == null){
			_enFaceImagePlus = new ImagePlus("Integrated En Face", fp.convertToShortProcessor(true));
			_enFaceImagePlus.show();
			_enFaceImagePlus.getCanvas().addMouseListener(new MouseListener() {
				
				@Override
				public void mouseReleased(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mousePressed(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mouseExited(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mouseEntered(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void mouseClicked(MouseEvent e) {
					// TODO Auto-generated method stub
					Point pp = _enFaceImagePlus.getCanvas().getCursorLoc();
					int y = (int) pp.getY();
					double[] l = _enFaceImagePlus.getProcessor().getLine(0, y, _enFaceImagePlus.getWidth(), y);
					float[] x = new float[l.length];
					float[] in = new float[l.length];
					for(int i = 0; i < x.length; i++){
						x[i] = i;
						in[i] = (float) l[i];
					}
					
					float[] inv = Math1D.invert(in);
					float[] filter = new float[inv.length];
					int window = 3;
					for(int i = window; i < in.length - 1 - 2*window; i++){
						float o = 0;
						for(int k = -window; k <= window; k++){
							o += inv[i+k];
						}
						o /= 2*window;
						filter[i] = o;
					}
					
					Plot p = new Plot("Cross Section", "", "Int", x, filter);
					p.show();
				}
			});
		}else{
			_enFaceImagePlus.setProcessor(fp.convertToShortProcessor(true));
		}
		
		if(!_enFaceImagePlus.isVisible()){
			_enFaceImagePlus.show();
		}
		
		//IJ.run(_enFaceImagePlus, "Enhance Contrast...", "saturated=0.3");
		
	}

	/**
	 * Moves the selected slider and updates the start and end depths to process
	 */
	@Override
	public void mouseDragged(MouseEvent arg0) {
		if(_selectedLine != null){
			//_selectedLine.y1 = _parent.getCanvas().getCursorLoc().y;
			//_selectedLine.y2 = _parent.getCanvas().getCursorLoc().y;
			_startDepth = Math.min(_l1.y1, _l2.y1);
			_endDepth = Math.max(_l1.y1, _l2.y2);
			_selectedLine.setLocation(0, _parent.getCanvas().getCursorLoc().y);
			_parent.updateAndDraw();
			try {
				computeEnFace();
			} catch (InterruptedException e) {
				IJ.log("Error compution En Face because of " + e.getMessage());
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {

	}
}
