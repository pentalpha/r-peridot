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

/**
 *
 * @author pentalpha
 */
public class DiffExpressionModule extends AnalysisModule {
    public DiffExpressionModule(String name, String scriptPath,
                                boolean max2Conditions, boolean needsReplicates)
    {
        super(name, scriptPath, getDefaultParameters(), getDefaultRequiredFiles(),
              getDefaultResults());
        this.max2Conditions = max2Conditions;
        this.needsReplicates = needsReplicates;
        this.setResultAsMandatory("res.tsv");
    }
    public DiffExpressionModule(File dir) throws Exception{
        super(dir);
    }
    
    public boolean mandatoryFailed(){
        return mandatoryFailed;
    }
    
    public static TreeMap<String, Class> getDefaultParameters(){
        TreeMap<String, Class> parameters = new TreeMap<String, Class>();
        parameters.put("pValue", Float.class);
        parameters.put("fdr", Float.class);
        parameters.put("foldChange", Float.class);
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
