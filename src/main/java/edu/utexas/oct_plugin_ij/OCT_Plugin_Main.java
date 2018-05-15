package edu.utexas.oct_plugin_ij;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLDevice.Type;
import edu.utexas.opencl.UTOpenCL;
import edu.utexas.primitives.Tuples.Triplet;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.io.Opener;
import ij.process.*;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements ImageJ PlugIn class that reads and processes various OCT dataset types
 *
 * @author Austin McElroy
 * @version 0.2.0
 */
@Plugin(type = Command.class, menuPath = "Plugins>OCT Plugin")
public class OCT_Plugin_Main implements Command, MouseListener, MouseMotionListener, KeyListener, WindowListener{
    /**
     * ImagePlus stack that will be used to display the OCT data
     */
    static protected ImagePlus OCTPlus = null;

    /**
     * Array of CLProgramTransformVolume_IJ classes
     */
    static protected List<CLProgramTransformVolume_IJ> transformInterfaces = new ArrayList<CLProgramTransformVolume_IJ>();

    /**
     * AScanViewer window to display the depth information of A-Scans
     *
     * @see AscanViewer()
     */
    static protected AscanViewer ascanviewer;

    /**
     * AScanViewer window to display the depth information of A-Scans
     *
     * @see AscanViewer()
     */
    static protected AscanAverage ascanaverage;

    /**
     * EnFaceViewer to show arbitrary En Face views in a new window
     *
     * @see EnFaceViewer ()
     */
    static protected EnFaceViewer enfaceviewer;

    /**
     * CLContext that stores the OpenCL Context information
     */
    static protected UTOpenCL ocl = null;

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        // invoke the plugin
        ij.command().run(OCT_Plugin_Main.class, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Parser("UT_U16_OCT");
    }



    public static ImagePlus getOCTPlus(){
        return OCTPlus;
    }

    /**
     * Parsing function that takes String data from plugins.config
     *
     * @param args String argument
     */
    protected void Parser(String args){
        if(ocl == null){
            ocl = new UTOpenCL(Type.CPU);
        }

        GenericDialog gd;
        ImagePlus ip;
        ImageStack is;

        switch(args){

            case "CLEAN":
                transformInterfaces.clear();
                break;

            case "MERGE":
                merge();
                break;

            case "Colorizer":
                if(IJ.getImage().getBitDepth() != 16) {
                    IJ.showMessage("This script requires a 16-bit grayscale image");
                    return;
                }

                is = IJ.getImage().getStack();

                ImageStack output_is = new ImageStack(is.getWidth(), is.getHeight());

                for(int i = 1; i <= is.size(); i++) {
                    FloatProcessor fp = is.getProcessor(i).convertToFloatProcessor();
                    float[] pix = (float[])fp.getPixels();
                    int[] rgb = new int[pix.length];
                    for(int j = 0; j < pix.length; j++) {
                        int b = (int)(((int)pix[j]) & 0xff);
                        //int g = 0; //(int)(((int)pix[j]) & 0xff);
                        int r = (int)((((int)pix[j]) & 0xff00) >> 8);
                        rgb[j] = r << 16 | b;
                    }
                    ColorProcessor cp = new ColorProcessor(is.getWidth(), is.getHeight());
                    cp.setPixels(rgb);

                    output_is.addSlice(cp);
                }

                ip = new ImagePlus("RGB Color", output_is);
                ip.show();


                break;

            case "Split_Spectrum_UTOCT":
                try {
                    List<ImagePlus> ipList2 = openSplitSpectrumUTOCT();
                    if(ipList2 == null){
                        return;
                    }
                    OCTPlus = ipList2.get(0);
                } catch (IOException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                }


                break;

            case "ST_JUDE_LUMEN":
                ImagePlus orig = IJ.getImage();

                ImagePlus dup = orig.duplicate();
                dup.show();
                //Convert to float
                IJ.run("8-bit");
                //Zero out the top of the catheter
                IJ.makeRectangle(0, 0, 504, 48);
                IJ.setBackgroundColor(0, 0, 0);
                IJ.run("Clear", "slice");
                IJ.run("Bilateral Filter", "spatial=9 range=100");
                dup.close();

                orig.setColor(Color.RED);
                for(int k = 1; k < IJ.getImage().getStackSize(); k++) {
                    byte[] frame_b = (byte[])IJ.getImage().getStack().getProcessor(k).convertToByteProcessor().getPixels();
                    ByteProcessor bp = IJ.getImage().getStack().getProcessor(k).convertToByteProcessor();
                    int[] column = new int[IJ.getImage().getHeight()];

                    int[] lumen = new int[IJ.getImage().getWidth()];

                    for(int j = 0; j < IJ.getImage().getWidth(); j++) {
                        bp.getColumn(j, 48, column, column.length);

                        int index = 0;
                        int max = Integer.MIN_VALUE;
                        for(int i = 0; i < column.length; i++) {
                            if(column[i] > max) {
                                max = column[i];
                                index = i;
                            }
                        }

                        for(int i = index; i > 0; i--) {
                            if(column[i] < max*.75) {
                                lumen[j] = i + 48;
                                Roi r = new Roi(j, i + 48, 1, 1);
                                r.setStrokeColor(Color.RED);
                                orig.getStack().getProcessor(k).drawRoi(r);
                                break;
                            }
                        }
                    }


                    int zz = 0;
                }


                break;

            case "UT_U16_OCT":
                List<ImagePlus> ipList = null;

                gd = new GenericDialog("UT OCT Options");
                gd.addCheckbox("Scale Intensity (0 to 256)", true);
                gd.addCheckbox("Reduce Volume by 50%", true);
                gd.addCheckbox("Export FFT R & I? (Warning: Memory Intensive!)", false);
                gd.addNumericField("Interleaving Factor (1=no interleaving)", 1, 1);
                gd.showDialog();
                boolean scaleIntensity = gd.getNextBoolean();
                boolean scaleVolume = gd.getNextBoolean();
                boolean exportFFT = gd.getNextBoolean();
                int interleaveNum = (int)gd.getNextNumber();

                //just in case user thinks 0 should be no interleaving
                if (interleaveNum <= 0){
                    interleaveNum = 1;
                }

                try {
                    ipList = openUTOCT(scaleIntensity, scaleVolume, exportFFT, interleaveNum);
                } catch (IOException e1) {
                    IJ.log("Couldn't open and process this dataset because of error " + e1.getMessage());
                }

                if(ipList == null){
                    return;
                }

                for(int i = 0; i < ipList.size(); i++) {
                    if(i == 0) {
                        ImageWindow.setNextLocation(0,0);
                    }
                    int w = 256;
                    int h = 256;
                    ipList.get(i).show();

                    if(scaleVolume){
                        ipList.get(i).getWindow().setSize(w, h);
                    }

                    CLProgramTransformVolume_IJ slider = new CLProgramTransformVolume_IJ(ocl, ipList.get(i));
                    slider.addWindowListener(this); //When window is closed, perform an action

                    //IJ.run(ipList.get(i), "Enhance Contrast...", "saturated=0.3");

                    ipList.get(i).getWindow().setLocation((transformInterfaces.size())*w + 15, ipList.get(i).getWindow().getLocation().y);
                    int maxWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
                    if(ipList.get(i).getWindow().getLocation().x + w > maxWidth - w) {
                        ImageWindow.setNextLocation(0, ipList.get(i).getWindow().getLocation().y + h + 15);
                    }
                    slider.setSize(w,266);
                    slider.setLocation(ipList.get(i).getWindow().getLocation().x, ipList.get(i).getWindow().getLocation().y + h);

                    transformInterfaces.add(slider);
                    OCTPlus = ipList.get(i);
                }
                break;

            case "CYNOSURE":
                try {
                    openCynosureOCT();
                } catch (FileNotFoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                break;

            case "SJ_TD_OCT":
            case "SJ_SD_OCT":
                try {
                    ip = openStJudeOCT();
                    //IJ.getImage().show();
                    //IJ.run(ip, "Enhance Contrast...", "saturated=0.03");
                    ip.show();
                    IJ.run("blue orange icb");
                    IJ.run("Enhance Contrast", "saturated=0.35");

                    CLProgramTransformVolume_IJ slider = new CLProgramTransformVolume_IJ(ocl, ip);
                    slider.addWindowListener(this); //When window is closed, perform an action

                    transformInterfaces.add(slider);
                    OCTPlus = ip;
                } catch (FileNotFoundException e) {
                    IJ.log("File not found");
                }
                break;

            case "RtoP":
                if(IJ.getImage() == null){
                    IJ.error("No images are Open!");
                    return;
                }

                ImageStack a = IJ.getImage().getStack();
                ImageStack converted = RtoP(a);
                IJ.getImage().setStack(converted);
                IJ.getImage().show();
                break;

            case "A-Scan Viewer":
                if(IJ.getImage() == null){
                    IJ.error("No OCT images are Open!");
                    return;
                }

                viewAScan();

                break;

            case "Remove A-Scan Viewer":
                if(ascanviewer == null){
                    return;
                }

                ascanviewer.remove();

                break;

            case "A-Scan Average Viewer":
                if(IJ.getImage() == null){
                    IJ.error("No OCT images are Open!");
                    return;
                }

                ascanaverage = new AscanAverage(IJ.getImage());
                break;

            case "Remove A-Scan Average Viewer":
                if(ascanaverage == null){
                    return;
                }
                ascanaverage.remove();
                break;

            case "MOVING_WINDOW_EF":
                if(IJ.getImage() == null){
                    IJ.error("No Volume is Open!");
                    return;
                }

                ColorizedEnface cef = new ColorizedEnface(IJ.getImage());
                cef.compute();

                break;

            case "En Face Viewer":
                if(IJ.getImage() == null){
                    IJ.error("No OCT images are Open!");
                    return;
                }

                enfaceviewer = new EnFaceViewer(IJ.getImage());
                break;

            case "Remove En Face Viewer":
                if(enfaceviewer == null)
                    return;

                enfaceviewer.remove();
                break;

            case "Unscaled Attenuation Coefficient":
                float[] input = (float[])IJ.getProcessor().convertToFloatProcessor().getPixels();
                int w = IJ.getProcessor().getWidth();
                int h = IJ.getProcessor().getHeight();

                GenericDialog gd1 = new GenericDialog("Attenuation Scaling Parameters");
                gd1.addNumericField("mm per Pixel", .0075, 5);
                gd1.addNumericField("Focal depth in pixels", 150, 5);
                gd1.addNumericField("Rayleigh Range (mm)", .24, 5);
                gd1.addCheckbox("Ignore Scalling", true);
                gd1.addCheckbox("Apply to stack?", false);
                gd1.showDialog();
                double mmPerPix = gd1.getNextNumber();
                double focalDepthPixels = gd1.getNextNumber();
                double rrange = gd1.getNextNumber();
                boolean ignore = gd1.getNextBoolean();
                boolean applyToStack = gd1.getNextBoolean();

                float[] atten;
                AttenuationCoefficient ac = new AttenuationCoefficient();

                if(applyToStack){
                    is = new ImageStack(w, h);
                    for(int i = 1; i < IJ.getImage().getStackSize(); i++){
                        float[] frame = (float[])IJ.getImage().getStack().getProcessor(i).convertToFloatProcessor().getPixels();
                        if(ignore){
                            atten = ac.run(frame, w, h, true, (float)0.0, (float)0.0f, (float)mmPerPix);
                        }else{
                            atten = ac.run(frame, w, h, true, (float)focalDepthPixels, (float)rrange, (float)mmPerPix);
                        }
                        is.addSlice(new FloatProcessor(w, h, atten));
                        IJ.showProgress((double)i/(double)IJ.getImage().getStackSize());
                    }
                    ImagePlus ipAtten = new ImagePlus("Attenuation Stack", is);
                    ipAtten.show();
                }else{
                    if(ignore){
                        atten = ac.run(input, w, h, true, (float)0.0f, (float)0.0f, (float)mmPerPix);
                    }else{
                        atten = ac.run(input, w, h, true, (float)focalDepthPixels, (float)rrange, (float)mmPerPix);
                    }

                    FloatProcessor attenProc = new FloatProcessor(w, h, atten);
                    ImagePlus attenIp = new ImagePlus("Attenuation", attenProc);
                    attenIp.show();
                }

                break;
        }



        IJ.getImage().getCanvas().addMouseListener(this);
    }

    /**
     * Converts an ImagePlus to 8 bit grayscale
     *
     * @param ip
     */
    protected void convertTo8Bit(ImagePlus ip){
        StackConverter sc = new StackConverter(ip);
        sc.convertToGray8();
    }

    /**
     * Converts an ImagePlus to 16 bit grayscale
     *
     * @param ip
     */
    protected void convertTo16Bit(ImagePlus ip){
        StackConverter sc = new StackConverter(ip);
        sc.convertToGray16();
    }

    private void viewAScan(){
        ascanviewer = new AscanViewer(IJ.getImage());
    }

    /**
     * Converts an ImageStack from rectangular coordinates to polar coordinates
     *
     * @param input
     * @return A new ImageStack in polar coordinates
     */
    protected ImageStack RtoP(ImageStack input){
        int radius = input.getHeight();

        ImageStack output = new ImageStack(radius*2, radius*2);

        for(int i = 1; i <= input.getSize(); i++){
            IJ.showProgress(i, input.getSize());
            ImageProcessor temp_ip = input.getProcessor(i);
            byte[] pixels = (byte[]) temp_ip.getPixels();
            byte[] converted = Transforms.RectToPolar(pixels, temp_ip.getWidth(), temp_ip.getHeight());
            ByteProcessor converted_ip = new ByteProcessor(radius*2, radius*2, converted);
            output.addSlice(converted_ip);
        }

        return output;
    }

    @SuppressWarnings("unused")
    private List<String> selectedUTOCTFiles() throws FileNotFoundException{
        List<String> filename = new ArrayList<String>();

        FileDialog fd = new FileDialog(IJ.getInstance(), "Open .oct_scan", FileDialog.LOAD);
        fd.setMultipleMode(true);
        String osName = System.getProperty("os.name");
        boolean mac = osName.contains("Mac");
        boolean linux = osName.contains("Linux");
        boolean windows = osName.contains("Windows");
        if(mac || linux) {
            class OnlyExt implements FilenameFilter {
                private String ext;
                public OnlyExt(String ext) {
                    this.ext = "." + ext;
                }
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(ext);
                }
            }
            FilenameFilter only = new OnlyExt("oct_scan");
            fd.setFilenameFilter(only);
        }
        if (windows) {
            fd.setFile("*.oct_scan");
        }
        fd.setVisible(true);
        File[] fileset = fd.getFiles();
        for(int i = 0; i < fileset.length; i++) {
            filename.add(fd.getDirectory() + fileset[i].getName());
            if(filename == null){
                throw new FileNotFoundException();
            }
        }

        return filename;
    }

    private List<ImagePlus> openSplitSpectrumUTOCT() throws IOException{
        List<ImagePlus> ip = new ArrayList<ImagePlus>();

        GenericDialog gd = new GenericDialog("Split Spectrum Options");
        gd.addNumericField("Number of Spectrum (1 to 10)", 4, 0);
        gd.addNumericField("Percent Overlap (0 to .99)", .5, 2);
        gd.addCheckbox("Show Windows?", true);
        gd.showDialog();

        if(gd.wasCanceled()){
            return null;
        }

        double numberOfSpectrum = gd.getNextNumber();
        double overlap = gd.getNextNumber();
        boolean showWindows = gd.getNextBoolean();

        overlap = Math.max(0, overlap);
        overlap = Math.min(overlap, .99);
        numberOfSpectrum = Math.max(1, numberOfSpectrum);
        numberOfSpectrum = Math.min(10, numberOfSpectrum);

        List<String> filenames = selectedUTOCTFiles();
        if(filenames.isEmpty()){
            return null;
        }

        if(filenames.size() > 1){
            IJ.error("This option currently only supports a single pullback at a time");
            return null;
        }

        UTOCT u = new UTOCT();
        u.open(filenames.get(0));
        u.setMultispectralWindows((int)numberOfSpectrum, (float)overlap);
        if(showWindows){
            Float[] hanning = UTOCT.HanningWindow(u.getPointsPerAScan());
            ArrayList<Double> hList = new ArrayList<Double>();

            ArrayList<Double> x = new ArrayList<Double>();
            for(int i = 0; i < u.getPointsPerAScan(); i++){
                x.add(new Double(i));
                hList.add(new Double(hanning[i]));
            }
            Plot newPlot = new Plot("Spectrum", "Points in A-Scan", "Amplitude");
            newPlot.addPoints(x, hList, Plot.LINE);

            for(Float[] w : u.getWindows()){
                ArrayList<Double> tmp_w = new ArrayList<Double>();
                for(Float f : w){
                    tmp_w.add(new Double(f));
                }
                newPlot.addPoints(x, tmp_w, Plot.LINE);
            }

            newPlot.show();
        }

        ImageStack is = u.processVolume(false);

        ImagePlus hyperstack = new ImagePlus("Split Spectrum", is);
        hyperstack.setDimensions((int)numberOfSpectrum, u.getNumberOfBScans(), 1);
        CompositeImage ci = new CompositeImage(hyperstack, CompositeImage.GRAYSCALE);
        ci.show();
        ip.add(hyperstack);

        u.close();

        return ip;
    }

    /**
     * Opens and processes a UT OCT style data set. Displays the data in a new ImagePlus window
     *
     * @return A new ImagePlus containing the processed ImageStack
     * @throws IOException Thrown is .oct_scan is not found
     */
    @SuppressWarnings({ "unused" })
    protected List<ImagePlus> openUTOCT(boolean IntensityScaled, boolean VolumeScale, boolean ExportFFT, int interleaveNum) throws IOException{
        List<ImagePlus> ip = new ArrayList<ImagePlus>();
        List<ImageStack> is = new ArrayList<ImageStack>();

        List<String> filename = selectedUTOCTFiles();


        UTOCT utoct = new UTOCT();

        try {
            for(int i = 0; i < filename.size(); i++) {
                utoct.open(filename.get(i));
                is.add(utoct.processVolume(ExportFFT));

                if(ExportFFT){
                    ImagePlus f = new ImagePlus("FFT Results", utoct.getFFTResults());
                    f.setDimensions(2, utoct.getNumberOfBScans(), 1);
                    CompositeImage ci = new CompositeImage(f, CompositeImage.GRAYSCALE);
                    ci.show();
                }

                ImagePlus temp_ip = new ImagePlus("UT OCT " + filename.get(i), is.get(i));

                float scale = .5f;

                if(VolumeScale){
                    String xyzScale = "x=" + scale + " y=" + scale + " z=" + scale + " ";
                    String whd = "width=" + is.get(0).getWidth()*scale + " height=" + is.get(0).getHeight()*scale + " depth=" + is.get(0).getSize()*scale;

                    IJ.run(temp_ip, "Scale...", xyzScale + whd + " interpolation=Bicubic process");
                    temp_ip.close();
                }

                ip.add(temp_ip);

                if(IntensityScaled){
                    convertTo8Bit(temp_ip);
                }
                utoct.close();
            }
            return ip;
        } catch (IOException e) {
            IJ.log("Couldn't parse the .oct_scan file, maybe corrupted");
        }

        utoct = null;

        return null;
    }

    @SuppressWarnings("unused")
    private ImagePlus openCynosureOCT() throws FileNotFoundException{
        ImagePlus ip;

        FileDialog fd = new FileDialog(IJ.getInstance(), "Open .tif", FileDialog.LOAD);
        fd.setFile("*.tiff");
        fd.setVisible(true);
        String filename = fd.getDirectory() + fd.getFile();
        if(filename == null){
            throw new FileNotFoundException();
        }

        ip = new ImagePlus();

        Opener o = new Opener();
        FileInfo[] fi = Opener.getTiffFileInfo(filename);
        for(int i = 1; i <= fi.length; i++){
            ImageStack is = o.openTiff(filename, i).getStack();
            if(i == 0){
                ip = new ImagePlus("Cynosure", is);
            }
        }

        return ip;
    }

    /**
     * Opens a St. Jude Spectral Domain dataset
     *
     * @return A new ImagePlus containing the processed ImageStack
     * @throws FileNotFoundException Thrown if file is not found
     */
    @SuppressWarnings("unused")
    private ImagePlus openStJudeOCT() throws FileNotFoundException{
        ImagePlus ip;

        FileDialog fd = new FileDialog(IJ.getInstance(), "Open .oct", FileDialog.LOAD);
        fd.setFile("*.oct");
        fd.setVisible(true);
        String filename = fd.getDirectory() + fd.getFile();
        if(filename == null){
            throw new FileNotFoundException();
        }

        Opener o = new Opener();
        ImageStack temp_is = o.openTiff(fd.getDirectory(), fd.getFile()).getStack();
        ImageStack scaled_is = new ImageStack(temp_is.getWidth(), temp_is.getHeight());

        int numberOfFrames = temp_is.getSize();

        float[] f;

        for(int i = 1; i <= numberOfFrames; i++){
            FloatProcessor temp_ip = temp_is.getProcessor(i).convertToFloatProcessor();
            short[] scaled = new short[temp_is.getWidth()*temp_is.getHeight()];

            f = (float[])temp_ip.getPixels();
            for(int j = 0; j < f.length; j++) {
                scaled[j] = (short)(Math.log10(f[j])*20*678);
            }

            scaled_is.addSlice(new ShortProcessor(temp_ip.getWidth(), temp_ip.getHeight(), scaled, null));

            IJ.showProgress(i, numberOfFrames);
        }

        StackProcessor temp_sp = new StackProcessor(scaled_is);

        ip = new ImagePlus("St. Jude " + fd.getFile(), temp_sp.rotateRight());
        //ip = new ImagePlus("St. Jude " + fd.getFile(), temp_is);

        return ip;
    }

    /**
     * Merges any opened datasets into one super dataset
     */
    private void merge(){
        if(transformInterfaces.size() <= 1){
            IJ.error("Not enouch datasets are open to merge");
            return;
        }

        List<CLBuffer<ByteBuffer>> arrays = new ArrayList<CLBuffer<ByteBuffer>>();
        List<Triplet<Integer, Integer, Integer>> dims = new ArrayList<Triplet<Integer, Integer, Integer>>();
        List<Triplet<Double, Double, Double>> angles = new ArrayList<Triplet<Double, Double, Double>>();
        List<Triplet<Integer, Integer, Integer>> trans = new ArrayList<Triplet<Integer, Integer, Integer>>();

        for(CLProgramTransformVolume_IJ p : transformInterfaces){
            if(p == null)
                return;

            arrays.add(p.runWithoutWindowCLBuffer());
            dims.add(p.getDim());
            angles.add(p.getAngle());
            trans.add(p.getTranslation());
        }

        int xMax = Integer.MIN_VALUE;
        int yMax = Integer.MIN_VALUE;
        int zMax = Integer.MIN_VALUE;

        for(Triplet<Integer, Integer, Integer> t : dims){
            if(t.x() > xMax){
                xMax = t.x();
            }

            if(t.y() > yMax){
                yMax = t.y();
            }

            if(t.y() > zMax){
                zMax = t.z();
            }
        }

        int xSize = (int)Math.floor((double) (xMax*1.5));
        int ySize = (int)Math.floor((double) (yMax*1.5));
        int zSize = (int)Math.floor((double) (zMax*1.5));

        InputStream istream = this.getClass().getResourceAsStream("merge_3d.cl");

        CLProgramMerge cl_merge = new CLProgramMerge(ocl, istream, xSize, ySize, zSize);

        if(!cl_merge.merge(arrays, dims, trans)){
            IJ.log(cl_merge.getMsg());
            return;
        }

        Triplet<Integer, Integer, Integer> dims_cl_dims = cl_merge.getDims();

        ImageStack is = new ImageStack(dims_cl_dims.x(), dims_cl_dims.y());

        byte[] buffer = cl_merge.getMergedData();

        int slice_size = dims_cl_dims.x()*dims_cl_dims.y();
        for(int z = 0; z < dims_cl_dims.z() - 1; z++){
            byte[] temp = Arrays.copyOfRange(buffer, z*slice_size, (z + 1)*slice_size);
            ByteProcessor bp = new ByteProcessor(dims_cl_dims.x(), dims_cl_dims.y(), temp);
            is.addSlice(bp);
            temp = null;
        }

        ImagePlus mergedIP = new ImagePlus("Merged Results", is);
        mergedIP.show();
        mergedIP.getWindow().addWindowListener(cl_merge);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK && e.getKeyCode() == KeyEvent.VK_M){
            merge();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

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
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {
        if(e.getSource().getClass() == CLProgramTransformVolume_IJ.class){
            for(int i = 0; i < transformInterfaces.size(); i++){
                if(transformInterfaces.get(i) == e.getSource()){
                    transformInterfaces.get(i).cleanup();
                    transformInterfaces.remove(i);
                    break;
                }
            }
        }
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
}

