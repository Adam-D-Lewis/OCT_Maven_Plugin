package edu.utexas.ablation;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AblationXMLParser {
	public AblationModule parse(String filename)
    {
        AblationModule am = new AblationModule();

        try
        {
        	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        	DocumentBuilder db = dbf.newDocumentBuilder();
        	Document d = db.parse(filename);
        	
        	Element root = d.getDocumentElement();
        	
            //XmlReader xmlr = XmlReader.Create(filename);
            String[] items;
            String s;

            Layer currentLayer;
            BScan currentBScan;
            
            NodeList nl = root.getElementsByTagName("Layer");
            NamedNodeMap nnm;
            
            for(int i = 0; i < nl.getLength(); i++){
            	Layer l = new Layer();
            	NamedNodeMap layerAttributes = nl.item(i).getAttributes();
            	Node tmp = layerAttributes.getNamedItem("number".toLowerCase());
            	l.LayerNumber = Integer.parseInt(tmp.getNodeValue());
            	
            	NodeList bscanNodes = nl.item(i).getChildNodes();
            	int bscanlenght = bscanNodes.getLength();
            	for(int k = 0; k < bscanNodes.getLength(); k++){
            		Node bscannode = bscanNodes.item(k);
            		if(bscannode.getNodeType() == Node.ELEMENT_NODE){
                		BScan b = new BScan();
                    	NamedNodeMap bscanAttributes = bscanNodes.item(k).getAttributes();
                    	b.BScanNumber = Integer.parseInt(bscanAttributes.item(0).getNodeValue());
                		
                		NodeList startStopNodes = bscannode.getChildNodes();
                		int sdfa = startStopNodes.getLength();
                		for(int j = 0; j < startStopNodes.getLength(); j++){
                			Node ssn = startStopNodes.item(j);
                			if(ssn.getNodeType() == Node.ELEMENT_NODE){
                    			String sss = startStopNodes.item(j).getTextContent();
                    			String[] ssss = sss.split(",");
                    			try{
        	            			StartStop ss = new StartStop(Integer.parseInt(ssss[0]), Integer.parseInt(ssss[1]));
        	            			b.StartStopList.add(ss);
                    			}catch(Exception e){
                    				
                    			}
                			}
                		}
                		l.BScanList.add(b);
            		}
            	}
            	
            	am.LayerList.add(l);
            }
        }catch(Exception e)
        {

        }

        return am;
    }
}
