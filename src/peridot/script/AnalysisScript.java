/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author pitagoras
 */
public class AnalysisScript extends RScript{
    
    public AnalysisScript(String name, String scriptFile, boolean externalScript,
            Map<String, Class> requiredParameters, 
            Set<String> requiredExternalFiles,
            Set<String> results,
            boolean floatValuesToo)
    {
        super(name, scriptFile, externalScript, requiredParameters, 
                requiredExternalFiles, results, new TreeSet<String>(),
                floatValuesToo);
    }
    
    public AnalysisScript(File dir) throws Exception{
        super(dir);
    }
}
