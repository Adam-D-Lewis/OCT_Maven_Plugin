package edu.utexas.ablation;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import java.util.List;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.JLabel;
import java.awt.Dimension;
import javax.swing.event.ChangeListener;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;

import javax.swing.event.ChangeEvent;
import java.awt.event.WindowStateListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;

public class AblationGUI extends JFrame implements ImageListener {

	private JPanel contentPane;
	private JSpinner spinnerLayer;

	/**
	 * Create the frame.
	 */
	public AblationGUI(ImagePlus ip, AblationModule am) {
		if(ip != null){
			ImagePlus.addImageListener(this);
		}
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				if(ip == null){
					return;
				}
			}
		});
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		
		contentPane.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		setContentPane(contentPane);
		contentPane.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel lblLayer = new JLabel("Layer");
		contentPane.add(lblLayer);
		
		spinnerLayer = new JSpinner();
		spinnerLayer.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				if(ip == null){
					return;
				}

				if(ip.getOverlay() != null){
					ip.getOverlay().clear();
				}
				
				JSpinner s = (JSpinner)arg0.getSource();
				int layerNumber = (int)s.getValue();
				int BScanNumber = ip.getCurrentSlice();
				
				List<StartStop> ssList = am.getStartStopList(layerNumber, BScanNumber);
				if(ssList.isEmpty()){
					return;
				}
				
				Overlay o = new Overlay();
				for(StartStop ss : ssList){
					for(int i = ss.Start; i < ss.Stop; i++){
						Roi r = new Roi(i, layerNumber, 1, 1);
						o.add(r);
					}
				}
				
				ip.setOverlay(o);
			}
		});
		spinnerLayer.setPreferredSize(new Dimension(75, 22));
		spinnerLayer.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		contentPane.add(spinnerLayer);
	}

	@Override
	public void imageClosed(ImagePlus arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void imageOpened(ImagePlus arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void imageUpdated(ImagePlus arg0) {
		int o = (int)spinnerLayer.getValue();
		spinnerLayer.setValue(o + 1);
		spinnerLayer.setValue(o);
	}

}
