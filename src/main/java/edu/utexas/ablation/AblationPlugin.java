package edu.utexas.ablation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeListener;

import edu.utexas.oct_plugin_ij.OCT_Plugin_Main;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.plugin.PlugIn;

public class AblationPlugin implements PlugIn, ImageListener{

	protected static AblationModule am = null;
	
	@SuppressWarnings("static-access")
	@Override
	public void run(String arg0) {
//		if(OCT_Plugin_Main.getOCTPlus() == null){
//			IJ.showMessage("OCT Volume needs to be open");
//			return;
//		}else{
//			OCT_Plugin_Main.getOCTPlus().addImageListener(this);
//		}		

		switch(arg0.toLowerCase()){
		case "import":
			JFileChooser jfc = new JFileChooser();
			jfc.showOpenDialog(null);
			
			File f = jfc.getSelectedFile();
			if(f == null){
				return;
			}
			
			am = new AblationModule();
			am.load(f.getAbsolutePath());
			AblationGUI ag = new AblationGUI(OCT_Plugin_Main.getOCTPlus(), am);		
			ag.setVisible(true);			
			break;
		}
		
		
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
		int currentFrame = arg0.getSlice();

		for(int i = 0; i < am.LayerList.size(); i++){
			
		}
	}
}
