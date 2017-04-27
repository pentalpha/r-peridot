/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pentalpha
 */
public class AnalysisParameters {
    public Map<String, Class> requiredParameters;
    public Map<String, Object> parameters;
    
    public AnalysisParameters(){
        requiredParameters = new HashMap<String, Class>();
        parameters = new HashMap<String, Object>();
    }
    
    public AnalysisParameters(Map<String, Class> requiredParameters){
        this.requiredParameters = new HashMap<String, Class>();
        this.parameters = new HashMap<String, Object>();
        
        this.requiredParameters.putAll(requiredParameters);
    }
    
    public AnalysisParameters(Map<String, Class> requiredParameters, 
                              Map<String, Object> parameters){
        this.requiredParameters = new HashMap<String, Class>();
        this.parameters = new HashMap<String, Object>();
        
        this.requiredParameters.putAll(requiredParameters);
        this.parameters.putAll(parameters);
    }
    
    public boolean passParameter(String name, Object value){
        if(requiredParameters.containsKey(name)){
            if(requiredParameters.get(name) == value.getClass()){
                parameters.put(name, value);
                return true;
            }else{
                Log.logger.info(requiredParameters.get(name) + " != " + value.getClass());
            }
        }else{
            Log.logger.info("required params do not contain " + name);
        }
        return false;
    }
    
    public void newRequiredParameter(String name, Class type){
        requiredParameters.put(name, type);
    }
    
    @Override
    public String toString(){
        String string = "";
        for(Map.Entry<String, Object> pair : parameters.entrySet()){
            string += pair.getKey() + ": ";
            string += pair.getValue() + ", ";
        }
        return string;
    }
}
