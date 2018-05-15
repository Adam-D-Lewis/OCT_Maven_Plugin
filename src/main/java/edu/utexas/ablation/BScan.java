package edu.utexas.ablation;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class BScan {
    public List<StartStop> StartStopList;
    public int BScanNumber;
    
    protected Hashtable<Integer, BScan> StartStopHashTable;

    public BScan()
    {
        StartStopList = new ArrayList<StartStop>();
    }

    public void addStartStop(StartStop pair)
    {
        StartStopList.add(pair);
    }
}
