/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import java.io.File;
import java.util.TreeSet;
import peridot.Archiver.Places;

/**
 *
 * @author pitagoras
 */
public class GeneOntologyScript extends AnalysisScript{
    public GeneOntologyScript(String name, String scriptPath, boolean externalScript)
    {
        super(name, scriptPath, externalScript, 
                PostAnalysisOntology.getDefaultParameters(), 
                getDefaultRequiredFiles(), 
                PostAnalysisOntology.getDefaultResults(),
                true);
        this.max2Conditions = false;
    }
    public GeneOntologyScript(File dir) throws Exception{
        super(dir);
    }
    
    private static TreeSet<String> getDefaultRequiredFiles(){
        TreeSet<String> requiredExternalFiles = new TreeSet<>();
        requiredExternalFiles.add(Places.geneListInputFile.getName());
        
        return requiredExternalFiles;
    }
}
