/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot;

import java.util.HashMap;
import java.util.Map;

/**
 * The parameters necessary for an analysis.
 * @author pentalpha
 */
public class AnalysisParameters {
    /**
     * Maps parameter name to type of parameter to be passed.
     */
    public Map<String, Class> requiredParameters;

    /**
     * The parameters that have already been passed.
     */
    public Map<String, Object> parameters;

    /**
     * Creates an empty AnalysisParameters
     */
    public AnalysisParameters(){
        requiredParameters = new HashMap<String, Class>();
        parameters = new HashMap<String, Object>();
    }

    /**
     * Creates AnalysisParameters defining the required parameters.
     * @param requiredParameters    The parameters required (Name,Type).
     */
    public AnalysisParameters(Map<String, Class> requiredParameters){
        this.requiredParameters = new HashMap<String, Class>();
        this.parameters = new HashMap<String, Object>();
        
        this.requiredParameters.putAll(requiredParameters);
    }

    /**
     * Creates a ready AnalysisParameters, with both definition of
     * required parameters and parameters.
     * @param requiredParameters    The parameters required (Name,Type).
     * @param parameters            The parameters (Name, Value).
     */
    public AnalysisParameters(Map<String, Class> requiredParameters, 
                              Map<String, Object> parameters){
        this.requiredParameters = new HashMap<String, Class>();
        this.parameters = new HashMap<String, Object>();
        
        this.requiredParameters.putAll(requiredParameters);
        this.parameters.putAll(parameters);
    }

    /**
     * Pass a parameter.
     * @param name  Name of parameter to be passed.
     * @param value Value to be passed to the parameter.
     * @return      True if could pass the parameter successfully.
     */
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

    /**
     * Inserts new required parameter.
     * @param name  Parameter name.
     * @param type  Parameter type.
     */
    public void newRequiredParameter(String name, Class type){
        requiredParameters.put(name, type);
    }

    public static HashMap<String, Object> getDefaultValues(){
        HashMap<String, Object> defaultValues = new HashMap<>();
        defaultValues.put("pValue", new Float(0.01));
        defaultValues.put("fdr", new Float(0.05));
        defaultValues.put("log2FoldChange", new Float(0.01));
        defaultValues.put("tops", new Integer(0));
        defaultValues.put("geneIdType", new GeneIdType("None"));

        return defaultValues;
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
