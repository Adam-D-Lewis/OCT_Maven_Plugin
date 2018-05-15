package edu.utexas.ablation;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Layer {
    public List<BScan> BScanList;
    public int LayerNumber;
    
    protected Hashtable<Integer, BScan> BScanHashtable;

    public Layer()
    {
        BScanList = new ArrayList<BScan>();
    }
    
    public void mapBScans(){
    	if(BScanHashtable == null){
    		BScanHashtable = new Hashtable<Integer, BScan>();
    		for(BScan b : BScanList){
    			BScanHashtable.put(b.BScanNumber, b);
    		}
    	}
    }
    
    public BScan findBScan(int BScan){
    	mapBScans();
    	return BScanHashtable.get(BScan);    	
    }
}
