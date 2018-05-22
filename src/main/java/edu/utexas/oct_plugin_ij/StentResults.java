package edu.utexas.oct_plugin_ij;

import edu.utexas.math.P2;

import java.util.HashMap;
import java.util.List;

public class StentResults {
	HashMap<Integer, P2> _Stent;
	HashMap<Integer, P2> _Tissue;
	
	public StentResults(List<P2> stentLoc, List<P2> tissue){
		_Stent = new HashMap<Integer, P2>();
		_Tissue = new HashMap<Integer, P2>();
		
		for(int i = 0; i < stentLoc.size(); i++){
			_Stent.put(i, stentLoc.get(i));
			_Tissue.put(i, tissue.get(i));
		}
	}
	
	public HashMap<Integer, P2> getStents(){
		return _Stent;
	}
	
	public HashMap<Integer, P2> getTissue(){
		return _Tissue;
	}
}
