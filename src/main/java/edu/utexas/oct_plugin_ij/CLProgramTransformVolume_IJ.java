package edu.utexas.oct_plugin_ij;

import com.jogamp.opencl.CLBuffer;
import edu.utexas.opencl.UTOpenCL;
import edu.utexas.primitives.Tuples.*;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class CLProgramTransformVolume_IJ extends JFrame implements KeyListener, WindowListener, ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = -399696880830811245L;

	private ImagePlus _parent;
	
	private ImagePlus outputImagePlus = null;
	
	private JPanel contentPane;
	private JTextField rotx_textfield;
	private JTextField roty_textfield;
	private JTextField rotz_textfield;
	private JTextField translatex_textfield;
	private JTextField translatey_textfield;
	private JTextField translatez_textfield;
	private CLProgramTransformVolume backEnd;
	private float rotx_value;
	private float roty_value;
	private float rotz_value;
	private int transx_value;
	private int transy_value;
	private int transz_value;
	private JButton btnGo;
	
	private enum AXIS_CONTROL {X, Y, Z};
	private enum AXIS_TRANS {X, Y, Z};
	
	/**
	 * Create the frame.
	 */
	public CLProgramTransformVolume_IJ(UTOpenCL ocl, ImagePlus parent) {
		super("Slider");

		_parent = parent;
		
		this.addWindowListener(this);
		
		InputStream is = getClass().getResourceAsStream("rotation_matrix_3d.cl");

		backEnd = new CLProgramTransformVolume(ocl, is);
		
		Quartet<ByteBuffer, Integer, Integer, Integer> output = imagePlus_to_ByteBuffer(_parent);
		backEnd.loadVolume(output.x(), output.y(), output.z(), output.w());
		

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 282, 227);
		
		rotx_textfield = new JTextField();
		rotx_textfield.setText("0.0");
		rotx_textfield.setColumns(10);
		rotx_textfield.addActionListener(new RotationTextFieldActionListener());
		
		JLabel lblRotationX = new JLabel("Rotation X");
		
		JLabel label = new JLabel("Rotation Y");
		
		JLabel label_1 = new JLabel("Rotation Z");
		
		translatex_textfield = new JTextField();
		translatex_textfield.setText("0");
		translatex_textfield.setColumns(1);
		translatex_textfield.addActionListener(new RotationTextFieldActionListener());
		
		translatey_textfield = new JTextField();
		translatey_textfield.setText("0");
		translatey_textfield.setColumns(1);
		translatey_textfield.addActionListener(new RotationTextFieldActionListener());
		
		translatez_textfield = new JTextField();
		translatez_textfield.setText("0");
		translatez_textfield.setColumns(1);
		translatez_textfield.addActionListener(new RotationTextFieldActionListener());
		
		JLabel lblTranslateX = new JLabel("Translate X");
		
		JLabel lblTranslateY = new JLabel("Translate Y");
		
		JLabel lblTranslateZ = new JLabel("Translate Z");
		getContentPane().setLayout(new GridLayout(0, 2, 0, 0));
		getContentPane().add(rotx_textfield);
		getContentPane().add(lblRotationX);
		
		roty_textfield = new JTextField();
		roty_textfield.setText("0.0");
		roty_textfield.setColumns(10);
		roty_textfield.addActionListener(new RotationTextFieldActionListener());
		getContentPane().add(roty_textfield);
		getContentPane().add(label);
		
		rotz_textfield = new JTextField();
		rotz_textfield.setText("0.0");
		rotz_textfield.setColumns(10);
		rotz_textfield.addActionListener(new RotationTextFieldActionListener());
		getContentPane().add(rotz_textfield);
		getContentPane().add(label_1);
		getContentPane().add(translatex_textfield);
		getContentPane().add(lblTranslateX);
		getContentPane().add(translatey_textfield);
		getContentPane().add(lblTranslateY);
		getContentPane().add(translatez_textfield);
		getContentPane().add(lblTranslateZ);
		
		JLabel label_2 = new JLabel("");
		getContentPane().add(label_2);
		
		btnGo = new JButton("Go");
		btnGo.addActionListener(this);
		getContentPane().add(btnGo);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		this.addKeyListener(this);
		
		this.setVisible(true);
	}
	
	public Triplet<Integer, Integer, Integer> getDim(){
		int[] dim = outputImagePlus.getDimensions();
		return new Triplet<Integer, Integer, Integer>(dim[0], dim[1], dim[3]);
	}
	
	public Triplet<Double, Double, Double> getAngle(){
		return new Triplet<Double, Double, Double>((double)rotx_value, (double)roty_value, (double)rotz_value);
	}
	
	public Triplet<Integer, Integer, Integer> getTranslation(){
		return new Triplet<Integer, Integer, Integer>(transx_value, transy_value, transz_value);
	}
	
	public ImageStack runWithoutWindow(){
		btnGo.doClick();
		
		if(outputImagePlus != null){
			outputImagePlus.hide();
		}
		
		return outputImagePlus.getStack();
	}
	
	public CLBuffer<ByteBuffer> runWithoutWindowCLBuffer(){
		btnGo.doClick();
		
		if(outputImagePlus != null){
			outputImagePlus.hide();
		}
		
		return backEnd.getOutputCLBuffer();
	}
	
	private Quartet<ByteBuffer, Integer, Integer, Integer> imagePlus_to_ByteBuffer(ImagePlus ip){
		Quartet<ByteBuffer, Integer, Integer, Integer> results = null;
		
		int[] dim = ip.getDimensions();
		Integer width = dim[0];
		Integer height = dim[1];
		Integer depth = dim[3];
		
		ByteBuffer bb = ByteBuffer.allocateDirect(width*height*depth);
		
		results = new Quartet<ByteBuffer, Integer, Integer, Integer>(bb, 
																	width, 
																	height, 
																	depth);
		
		for(int k = 1; k <= depth; k++){
			ImageProcessor imProc = ip.getStack().getProcessor(k);
			ByteProcessor bp = imProc.convertToByteProcessor();
			byte[] array = (byte[]) bp.getPixels();
			bb.put(array);
		}
		
		return results;
	}
	
	class RotationTextFieldActionListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() instanceof JTextField){
				JTextField jtf = (JTextField)e.getSource();
				String s = jtf.getText();
				double value = 0;
				try{
					value = Double.valueOf(s);
				}catch(NumberFormatException error){
					System.out.print("Invalid number, setting to 0.0 \n");	
				}
				
				AXIS_CONTROL ac = null;
				AXIS_TRANS at = null;
				
				if(e.getSource().equals(rotx_textfield)){
					ac = AXIS_CONTROL.X;
				}else if(e.getSource().equals(roty_textfield)){
					ac = AXIS_CONTROL.Y;
				}else if(e.getSource().equals(rotz_textfield)){
					ac = AXIS_CONTROL.Z;
				}else if(e.getSource().equals(translatex_textfield)){
					at = AXIS_TRANS.X;
				}else if(e.getSource().equals(translatey_textfield)){	
					at = AXIS_TRANS.Y;
				}else if(e.getSource().equals(translatez_textfield)){
					at = AXIS_TRANS.Z;
				}
				
				if(ac != null){
					switch(ac){
					case X:
						rotx_value = (float) value;
						break;
					case Y:
						roty_value = (float) value;
						break;
					case Z:
						rotz_value = (float) value;
						break;
					default:
						break;			
					}
				}
				
				if(at != null){
					switch(at){
					case X:
						transx_value = (int) value;
						break;
					case Y:
						transy_value = (int) value;
						break;
					case Z:
						transz_value = (int) value;
						break;
					default:
						break;	
					}
				}
			}		
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
	
	}


	@Override
	public void keyPressed(KeyEvent e) {

	}


	@Override
	public void keyReleased(KeyEvent e) {
	
	}

	@Override
	public void windowOpened(WindowEvent e) {

	}

	@Override
	public void windowClosing(WindowEvent e) {

	}

	@Override
	public void windowClosed(WindowEvent e) {

	}

	@Override
	public void windowIconified(WindowEvent e) {
	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	
	}

	@Override
	public void windowActivated(WindowEvent e) {
	
	}

	@Override
	public void windowDeactivated(WindowEvent e) {

	}
	
	public void cleanup(){		
		if(_parent != null)
			_parent.close();
		
		_parent = null;
		
		if(outputImagePlus != null)
			outputImagePlus.close();
		
		outputImagePlus = null;
		
		if(backEnd != null)
			backEnd.cleanup();
		
		backEnd = null;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		rotx_textfield.postActionEvent();
		roty_textfield.postActionEvent();
		rotz_textfield.postActionEvent();
		
		translatex_textfield.postActionEvent();
		translatey_textfield.postActionEvent();
		translatez_textfield.postActionEvent();
		
		Quartet<ByteBuffer, Integer, Integer, Integer> outputParams = imagePlus_to_ByteBuffer(_parent);
		backEnd.loadVolume(outputParams.x(), outputParams.y(), outputParams.z(), outputParams.w());
		
		ByteBuffer output = backEnd.transform(rotx_value, roty_value, rotz_value, transx_value, transy_value, transz_value);
		
		int[] dim = backEnd.getDimensions();

		ImageStack is = new ImageStack(dim[0], dim[1]);
		
		for(int i = 0; i < dim[2]; i++){
			byte[] processedBScan = new byte[dim[0]*dim[1]];
			output.rewind();
			output.position(processedBScan.length*i);
			output.get(processedBScan);
			ByteProcessor bp = new ByteProcessor(dim[0], dim[1], processedBScan);
			processedBScan = null;
			is.addSlice(bp);
		}
		
		output.clear();
		
		if(outputImagePlus == null){
			outputImagePlus = new ImagePlus("Rotated Volume", is);
		}else{
			outputImagePlus.setStack(is);
		}
		
		outputImagePlus.show();	
	}

}
