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
public class AnalysisModule extends RModule {
    
    public AnalysisModule(String name, String scriptFile,
                          Map<String, Class> requiredParameters,
                          Set<String> requiredExternalFiles,
                          Set<String> results)
    {
        super(name, scriptFile, requiredParameters,
                requiredExternalFiles, results, new TreeSet<String>());
    }
    
    public AnalysisModule(File dir) throws Exception{
        super(dir);
    }
}
