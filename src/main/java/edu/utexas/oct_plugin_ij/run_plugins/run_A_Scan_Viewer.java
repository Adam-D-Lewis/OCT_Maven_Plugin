package edu.utexas.oct_plugin_ij.run_plugins;

import edu.utexas.oct_plugin_ij.OCT_Plugin_Main;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>OCT Plugin>A-Scan Viewer")
public class run_A_Scan_Viewer implements Command{

    public void run() {
        OCT_Plugin_Main OCT_plugin = new OCT_Plugin_Main();
        OCT_plugin.run("A-Scan Viewer");
    }

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
        ij.ui().showUI();        // invoke the plugin
        ij.command().run(run_A_Scan_Viewer.class, true);
    }
}
