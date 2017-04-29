/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import peridot.Archiver.Places;

import java.io.File;
import java.util.TreeMap;
import java.util.TreeSet;

//TODO: 
//Create constructor for packages 
//adapt class to external scripts too

/**
 *
 * @author pentalpha
 */
public class RNASeqPackage extends AnalysisScript {
    public RNASeqPackage(String name, String scriptPath, boolean externalScript, 
            boolean max2Conditions, boolean canDoFloatToo)
    {
        super(name, scriptPath, externalScript, getDefaultParameters(), getDefaultRequiredFiles(), 
              getDefaultResults(), canDoFloatToo);
        this.max2Conditions = max2Conditions;
        this.setResultAsMandatory("res.tsv");
    }
    public RNASeqPackage(File dir) throws Exception{
        super(dir);
    }
    
    public boolean mandatoryFailed(){
        return mandatoryFailed;
    }
    
    public static TreeMap<String, Class> getDefaultParameters(){
        TreeMap<String, Class> parameters = new TreeMap<String, Class>();
        parameters.put("pValue", Float.class);
        parameters.put("fdr", Float.class);
        parameters.put("log2FoldChange", Float.class);
        parameters.put("tops", Integer.class);
        
        return parameters;
    }
    
    public static TreeSet<String> getDefaultRequiredFiles(){
        TreeSet<String> requiredExternalFiles = new TreeSet<String>();
        requiredExternalFiles.add(Places.countReadsInputFileName);
        requiredExternalFiles.add(Places.conditionInputFileName);
        
        return requiredExternalFiles;
    }
    
    public static TreeSet<String> getDefaultResults(){
        TreeSet<String> results = new TreeSet<String>();
        results.add("plots.pdf");
        results.add("histogram.png");
        results.add("volcanoPlot.png");
        results.add("MAPlot.png");
        results.add("res.tsv");
        
        return results;
    }
    
    public static TreeSet<String> getMandatoryResults(){
        TreeSet<String> results = new TreeSet<String>();
        results.add("res.tsv");
        return results;
    }
}
