/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import peridot.GeneIdType;

import java.io.File;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author pitagoras
 */
public class PostAnalysisOntology extends PostAnalysisModule {
    public PostAnalysisOntology(String name, String scriptPath)
    {
        super(name, scriptPath, getDefaultParameters(),
                getDefaultRequiredFiles(), getDefaultResults(), getRequiredScripts());
        this.max2Conditions = false;
        this.needsReplicates = false;
    }
    public PostAnalysisOntology(File dir) throws Exception{
        super(dir);
    }
    
    public static TreeMap<String, Class> getDefaultParameters(){
        TreeMap<String, Class> parameters = new TreeMap<>();
        parameters.put("geneIdType", GeneIdType.class);
        parameters.put("pValue", Float.class);
        parameters.put("fdr", Float.class);
        return parameters;
    }
    
    public static TreeSet<String> getDefaultRequiredFiles(){
        TreeSet<String> requiredExternalFiles = new TreeSet<>();
        requiredExternalFiles.add("VennDiagram.PostAnalysisModule/Intersect.tsv");
        
        return requiredExternalFiles;
    }
    
    public static TreeSet<String> getDefaultResults(){
        TreeSet<String> results = new TreeSet<>();
        results.add("enrich.pdf");
        results.add("enrichGOCC.jpg"); 
        results.add("enrichGOMF.jpg"); 
        results.add("enrichGOBP.jpg");
        return results;
    }
    
    public static TreeSet<String> getRequiredScripts(){
        TreeSet<String> scripts = new TreeSet<>();
        scripts.add("VennDiagram");
        return scripts;
    }
}
