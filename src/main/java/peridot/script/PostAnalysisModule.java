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
public class PostAnalysisModule extends RModule {
    public static String extension = "PostAnalysisModule";

    public PostAnalysisModule(String name, String scriptFile,
                              Map<String, Class> requiredParameters,
                              Set<String> requiredExternalFiles,
                              Set<String> results,
                              Set<String> requiredScripts)
    {
        super(name, scriptFile, requiredParameters,
                requiredExternalFiles, results, requiredScripts);
    }
    
    public PostAnalysisModule(File dir) throws Exception{
        super(dir);
    }
}
