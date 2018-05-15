package edu.utexas.neuralnet;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import edu.utexas.imageprocessing.GLCM;
import edu.utexas.imageprocessing.GLCMResult;
import edu.utexas.imageprocessing.GLCM.GLCM_TYPE;
import edu.utexas.opencl.UTOpenCL;
import edu.utexas.oct_plugin_ij.AttenuationCoefficient;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.plugin.ContrastEnhancer;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ShortProcessor;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.BoxLayout;
import javax.swing.JTabbedPane;
import javax.swing.JButton;
import javax.swing.Box;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.AncestorEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SpringLayout;
import java.awt.FlowLayout;
import javax.swing.SwingConstants;
import javax.swing.JCheckBox;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class NeuralNetInterface extends JFrame implements WindowListener {

	private JPanel contentPane;
	
	JComboBox<String> comboBoxResults = new JComboBox<String>();
	private JTextField textFieldAngles;
	private JTextField textFieldPixelDistance;
	private JTextField textFieldXKernels;
	private JTextField textFieldYKernels;
	JCheckBox chckbxAttenuation;
	
	private ImagePlus ip;
	private UTOpenCL ocl;
	
	ArrayList<Integer> KernelXList = new ArrayList<Integer>();
	ArrayList<Integer> KernelYList = new ArrayList<Integer>();
	ArrayList<Integer> PixelList = new ArrayList<Integer>();
	ArrayList<Double> AngleList = new ArrayList<Double>();
	ArrayList<GLCMResult> Results = new ArrayList<GLCMResult>();

	/**
	 * Create the frame.
	 */
	public NeuralNetInterface(ImagePlus ip, UTOpenCL ocl) {
		this.ip = ip;
		this.ocl = ocl;
		
		addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent arg0) {

			}
			public void windowLostFocus(WindowEvent arg0) {
			}
		});
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent arg0) {

			}
		});
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				comboBoxResults.removeAllItems();
				for(int i = 0; i < Results.size(); i++){
					comboBoxResults.addItem(Results.get(i).getLabel());
				}
			}
		});
		contentPane.add(tabbedPane);
		
		JPanel panel_1 = new JPanel();
		tabbedPane.addTab("Compute", null, panel_1, null);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));
		
		Box verticalBox_2 = Box.createVerticalBox();
		verticalBox_2.setAlignmentY(Component.TOP_ALIGNMENT);
		panel_1.add(verticalBox_2);
		
		Box horizontalBox_3 = Box.createHorizontalBox();
		verticalBox_2.add(horizontalBox_3);
		
		Component horizontalStrut_6 = Box.createHorizontalStrut(40);
		horizontalBox_3.add(horizontalStrut_6);
		
		JLabel lblNewLabel = new JLabel("X Kernel Radii (comma seperated)");
		lblNewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		horizontalBox_3.add(lblNewLabel);
		
		Component horizontalStrut_7 = Box.createHorizontalStrut(20);
		horizontalBox_3.add(horizontalStrut_7);
		
		textFieldXKernels = new JTextField();
		textFieldXKernels.setText("5");
		horizontalBox_3.add(textFieldXKernels);
		textFieldXKernels.setColumns(10);
		
		Component horizontalStrut_8 = Box.createHorizontalStrut(40);
		horizontalBox_3.add(horizontalStrut_8);
		
		Box horizontalBox_4 = Box.createHorizontalBox();
		verticalBox_2.add(horizontalBox_4);
		
		Component horizontalStrut_9 = Box.createHorizontalStrut(40);
		horizontalBox_4.add(horizontalStrut_9);
		
		JLabel lblYKernelSizes = new JLabel("Y Kernel Radii (comma seperated)");
		lblYKernelSizes.setAlignmentX(0.5f);
		horizontalBox_4.add(lblYKernelSizes);
		
		Component horizontalStrut_10 = Box.createHorizontalStrut(20);
		horizontalBox_4.add(horizontalStrut_10);
		
		textFieldYKernels = new JTextField();
		textFieldYKernels.setText("5");
		textFieldYKernels.setColumns(10);
		horizontalBox_4.add(textFieldYKernels);
		
		Component horizontalStrut_11 = Box.createHorizontalStrut(40);
		horizontalBox_4.add(horizontalStrut_11);
		
		Box horizontalBox_1 = Box.createHorizontalBox();
		horizontalBox_1.setAlignmentY(Component.CENTER_ALIGNMENT);
		verticalBox_2.add(horizontalBox_1);
		
		Component horizontalStrut = Box.createHorizontalStrut(40);
		horizontalBox_1.add(horizontalStrut);
		
		JLabel lblAnglescommaSeperated = new JLabel("Angles (comma seperated, 0 <= a < 180)");
		horizontalBox_1.add(lblAnglescommaSeperated);
		
		Component horizontalStrut_2 = Box.createHorizontalStrut(20);
		horizontalBox_1.add(horizontalStrut_2);
		
		textFieldAngles = new JTextField();
		textFieldAngles.setHorizontalAlignment(SwingConstants.CENTER);
		horizontalBox_1.add(textFieldAngles);
		textFieldAngles.setText("0,45,90,135");
		textFieldAngles.setColumns(10);
		
		Component horizontalStrut_1 = Box.createHorizontalStrut(40);
		horizontalBox_1.add(horizontalStrut_1);
		
		Box horizontalBox_2 = Box.createHorizontalBox();
		verticalBox_2.add(horizontalBox_2);
		
		Component horizontalStrut_3 = Box.createHorizontalStrut(40);
		horizontalBox_2.add(horizontalStrut_3);
		
		JLabel lblPixelDistancecomma = new JLabel("Pixel Distance (comma seperated)");
		horizontalBox_2.add(lblPixelDistancecomma);
		
		Component horizontalStrut_4 = Box.createHorizontalStrut(20);
		horizontalBox_2.add(horizontalStrut_4);
		
		textFieldPixelDistance = new JTextField();
		textFieldPixelDistance.setHorizontalAlignment(SwingConstants.CENTER);
		textFieldPixelDistance.setText("1");
		horizontalBox_2.add(textFieldPixelDistance);
		textFieldPixelDistance.setColumns(10);
		
		Component horizontalStrut_5 = Box.createHorizontalStrut(40);
		horizontalBox_2.add(horizontalStrut_5);
		
		Box verticalBox_3 = Box.createVerticalBox();
		panel_1.add(verticalBox_3);
		
		chckbxAttenuation = new JCheckBox("Attenuation?");
		chckbxAttenuation.setAlignmentX(Component.CENTER_ALIGNMENT);
		verticalBox_3.add(chckbxAttenuation);
		
		Component verticalStrut = Box.createVerticalStrut(40);
		panel_1.add(verticalStrut);
		
		Component verticalGlue_1 = Box.createVerticalGlue();
		panel_1.add(verticalGlue_1);
		
		JButton btnComputeGLCM = new JButton("Compute GLCM");
		btnComputeGLCM.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				Results.clear();
				comboBoxResults.removeAllItems();
				parseInputs();				
				glcm();		
			}
		});
		btnComputeGLCM.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel_1.add(btnComputeGLCM);
		
		JPanel panel = new JPanel();
		tabbedPane.addTab("View", null, panel, null);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		Box verticalBox_1 = Box.createVerticalBox();
		panel.add(verticalBox_1);
		
		Box verticalBox = Box.createVerticalBox();
		verticalBox_1.add(verticalBox);
		
		comboBoxResults.setMaximumSize(new Dimension(512, 256));
		verticalBox.add(comboBoxResults);
		
		Component horizontalGlue = Box.createHorizontalGlue();
		verticalBox.add(horizontalGlue);
		
		Component verticalGlue = Box.createVerticalGlue();
		panel.add(verticalGlue);
		verticalGlue.setMinimumSize(new Dimension(0, 256));
		
		Box verticalBox_4 = Box.createVerticalBox();
		verticalBox_4.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(verticalBox_4);
		
		JButton viewButton = new JButton("View");
		viewButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		verticalBox_4.add(viewButton);
		
		Component verticalStrut_1 = Box.createVerticalStrut(20);
		verticalBox_4.add(verticalStrut_1);
		
		JButton btnExportButton = new JButton("Export");
		btnExportButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				//TODO: Add export Results here:
				exportResults();
			}
		});
		btnExportButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		verticalBox_4.add(btnExportButton);
		viewButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				int x = comboBoxResults.getSelectedIndex();
				GLCMResult g = Results.get(x);
				FloatProcessor fp = new FloatProcessor(g.getWidth(), g.getHeight(), g.getData());
				ImagePlus i = new ImagePlus(g.getLabel(), fp);
				i.show();
			}
		});
		
		this.addWindowListener(this);
	}
	
	private void exportResults(){
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setDialogTitle("Select Folder for export");
		jfc.showDialog(this, "Export");
		
		File dir = jfc.getSelectedFile();
		if(dir == null){
			return;
		}
		
		String p = dir.getAbsolutePath();
		for(GLCMResult r : Results){
			FloatProcessor f = new FloatProcessor(r.getWidth(), r.getHeight(), r.getData());
			ImagePlus ip = new ImagePlus("tmp", f);
			
			String filename = r.getLabel();
			filename = filename.replace(":", "-");
			
			FileSaver fs = new FileSaver(ip);
			fs.saveAsTiff(dir + "\\" + filename + ".tif");
			
			ip.close();
		}
	}
	
	private void parseInputs(){
		AngleList.clear();
		PixelList.clear();
		KernelXList.clear();
		KernelYList.clear();
		
		double[] dtmp = textToDouble(textFieldAngles.getText());
		for(double d : dtmp){
			AngleList.add(d);
		}
		
		int[] tmp = textToInteger(textFieldPixelDistance.getText());
		for(int i : tmp){
			PixelList.add(i);
		}
		
		tmp = textToInteger(textFieldXKernels.getText());
		for(int i : tmp){
			KernelXList.add(i);
		}
		
		tmp = textToInteger(textFieldYKernels.getText());
		for(int i : tmp){
			KernelYList.add(i);
		}
	}
	
	private int[] textToInteger(String line){
		String[] values = line.split(",");
		
		int[] ints = new int[values.length];
		
		try{
			for(int i = 0; i < values.length; i++){
				ints[i] = Integer.parseInt(values[i]);				
			}
		}catch(NumberFormatException e){
			JOptionPane.showInternalMessageDialog(this, e.getMessage());
		}
		
		return ints;
	}
	
	private double[] textToDouble(String line){
		String[] values = line.split(",");
		
		double[] d = new double[values.length];
		
		try{
			for(int i = 0; i < values.length; i++){
				d[i] = Double.parseDouble(values[i]);				
			}
		}catch(NumberFormatException e){
			JOptionPane.showInternalMessageDialog(this, e.getMessage());
		}
		
		return d;
	}
	
	private void glcm(){
		float[] input = (float[])ip.getProcessor().convertToFloatProcessor().getPixels();
		int w = ip.getProcessor().getWidth();
		int h = ip.getProcessor().getHeight();
		
		AttenuationCoefficient ac = new AttenuationCoefficient();
		float[] atten = ac.run(input, w, h, true, 0, 0, 0);
		
		FloatProcessor floatAtten = new FloatProcessor(w, h, atten);
		
		ShortProcessor shortAtten = floatAtten.convertToShortProcessor();
		ShortProcessor shortInput = ip.getProcessor().convertToShortProcessor();
		
		ContrastEnhancer ce = new ContrastEnhancer();
		ce.setNormalize(true);
		ce.stretchHistogram(shortInput, .03f);
		ce.equalize(shortInput);
		
		ce.stretchHistogram(shortAtten, .03f);
		ce.equalize(shortAtten);
		
		ByteProcessor byteAtten = shortAtten.convertToByteProcessor();
		ByteProcessor byteInput = shortInput.convertToByteProcessor();

		List<InputStream> isList = new ArrayList<InputStream>();
		InputStream fis = GLCM.class.getResourceAsStream("TextureAnalysis.cl");			
		isList.add(fis);
		
		GLCM octNN = new GLCM(ocl, isList);

		byte[] rawInput = (byte[])byteInput.getPixels();
		for(int i = 0; i < KernelXList.size(); i++){
			for(int j = 0; j < PixelList.size(); j++){
				int kernelX = KernelXList.get(i);
				int kernelY = KernelYList.get(i);	
				for(Integer k : PixelList){
					ArrayList<Integer> tal = new ArrayList<Integer>();
					tal.add(k);
					octNN.createXYOffset(tal, AngleList);
					octNN.glcm(rawInput, byteInput.getWidth(), byteInput.getHeight(), kernelX, kernelY, octNN.getXOffsetArray(), octNN.getYOffsetArray());
					Results.addAll(octNN.getGLCMResults("Intensity"));
				}
			}
		}	
		

		if(chckbxAttenuation.isSelected()){
		if(ip.getType() != ImagePlus.GRAY32){
			IJ.showMessage("ImagePlus stack needs to be a floating point grayscale number, in 20log10 format.\r\n"
					+ "The linear magnitude is required for the attenuation coefficient.");				
				return;
			}else{
				byte[] rawAtten = (byte[])byteAtten.getPixels();
				for(int i = 0; i < KernelXList.size(); i++){
					for(int j = 0; j < PixelList.size(); j++){
						int kernelX = KernelXList.get(i);
						int kernelY = KernelYList.get(i);
						for(Integer k : PixelList){
							ArrayList<Integer> tal = new ArrayList<Integer>();
							tal.add(k);
							octNN.createXYOffset(PixelList, AngleList);
							octNN.glcm(rawAtten, byteInput.getWidth(), byteInput.getHeight(), kernelX, kernelY, octNN.getXOffsetArray(), octNN.getYOffsetArray());
							Results.addAll(octNN.getGLCMResults("Attenuation"));
						}
					}
				}
			}
		}
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
}
