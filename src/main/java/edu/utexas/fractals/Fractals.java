package edu.utexas.fractals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import edu.utexas.math.P2;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.plugin.filter.RankFilters;
import ij.process.FloatProcessor;

public class Fractals implements PlugIn{

	public ImagePlus varFractal(ImagePlus im) {
		FloatProcessor ip = (FloatProcessor) im.getProcessor().convertToFloat();
		
		ImageStack is = new ImageStack(im.getWidth(), im.getHeight());
		
		for(int i = 1; i < 10; i += 2) {
			RankFilters rf = new RankFilters();
			FloatProcessor clone = (FloatProcessor) ip.duplicate();
			rf.rank(clone, i, RankFilters.VARIANCE);
			is.addSlice(clone);
		}
		
		return new ImagePlus("Var Fractal Filtered", is);
	}
	
	public ImagePlus zPlot(ImagePlus im) {
		ImagePlus retval;
		
		List<FloatProcessor> l = new ArrayList<FloatProcessor>();
		
		ImageStack is = im.getStack();
		for(int i = 1; i < is.getSize(); i++) {
			FloatProcessor fp = (FloatProcessor) is.getProcessor(i);
			l.add(fp);
		}
		
		float[] p4 = new float[im.getWidth()*im.getHeight()];
		float[] p3 = new float[im.getWidth()*im.getHeight()];
		float[] p2 = new float[im.getWidth()*im.getHeight()];
		float[] p1 = new float[im.getWidth()*im.getHeight()];
		float[] p0 = new float[im.getWidth()*im.getHeight()];
		
		for(int y = 0; y < im.getHeight(); y++) {
			for(int x = 0; x < im.getWidth(); x++) {
				float[] zPix = new float[is.getSize()];
				WeightedObservedPoints obs = new WeightedObservedPoints();
				for(int z = 0; z < l.size(); z++) {
					obs.add(z, l.get(z).getPixelValue(x, y));
				}				
				PolynomialCurveFitter pcf = PolynomialCurveFitter.create(4);
				double poly[] = pcf.fit(obs.toList());
				
				p4[y*im.getWidth() + x] = (float)poly[4];
				p3[y*im.getWidth() + x] = (float)poly[3];
				p2[y*im.getWidth() + x] = (float)poly[2];
				p1[y*im.getWidth() + x] = (float)poly[1];
				p0[y*im.getWidth() + x] = (float)poly[0];
			}
		}
		
		ImageStack out = new ImageStack(im.getWidth(), im.getHeight());
		out.addSlice(new FloatProcessor(im.getWidth(), im.getHeight(), p4));
		out.addSlice(new FloatProcessor(im.getWidth(), im.getHeight(), p3));
		out.addSlice(new FloatProcessor(im.getWidth(), im.getHeight(), p2));
		out.addSlice(new FloatProcessor(im.getWidth(), im.getHeight(), p1));
		out.addSlice(new FloatProcessor(im.getWidth(), im.getHeight(), p0));
		
		return new ImagePlus("zPlot 2nd, 1st, 0th order", out);
	}

	@Override
	public void run(String arg0) {
		ImagePlus ip;
		ImagePlus result;
		
		switch(arg0) {
		case "variance":
			ip = IJ.getImage();
			if(ip == null) {
				IJ.showMessage("No images are open");
			}else {
				result = varFractal(ip);
				result.show();
				zPlot(result).show();
				
			}
			break;
		}
		
	}
	
}
