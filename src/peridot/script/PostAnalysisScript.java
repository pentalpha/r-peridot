/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author pitagoras
 */
public class PostAnalysisScript extends RScript{
    public PostAnalysisScript(String name, String scriptFile, boolean externalScript,
            Map<String, Class> requiredParameters, 
            Set<String> requiredExternalFiles,
            Set<String> results,
            Set<String> requiredScripts)
    {
        super(name, scriptFile, externalScript, requiredParameters, 
                requiredExternalFiles, results, requiredScripts, true);
    }
    
    public PostAnalysisScript(File dir) throws Exception{
        super(dir);
    }
}
