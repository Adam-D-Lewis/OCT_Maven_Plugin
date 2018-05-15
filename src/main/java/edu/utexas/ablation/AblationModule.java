package edu.utexas.ablation;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class AblationModule {
    public int CurrentBScan;

    public int TotalBScans;

    public List<Layer> LayerList;
    
    protected Hashtable<Integer, Layer> LayerNumberLookup;

    public AblationModule()
    {
        LayerList = new ArrayList<Layer>();
    }

    public void load(String filename)
    {
        AblationXMLParser parser = new AblationXMLParser();
        AblationModule am = parser.parse(filename);
        this.LayerList = am.LayerList;
        
        LayerNumberLookup = new Hashtable<Integer, Layer>();
        for(Layer l : LayerList){
        	l.mapBScans();
        	LayerNumberLookup.put(l.LayerNumber, l);
        	
        }
    }
    
    public List<StartStop> getStartStopList(int Layer, int BScan){
    	List<StartStop> ss = new ArrayList<StartStop>();
    	
    	Layer l = LayerNumberLookup.get(Layer);
    	if(l == null){
    		
    	}else{
    		BScan b = l.findBScan(BScan);
    		if(b == null){
    			
    		}else{
    			ss = b.StartStopList;
    		}
    	}
    	
    	return ss;
    }
}
