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
                Log.logger.warning(requiredParameters.get(name) + "'s type is not " + value.getClass());
            }
        }else{
            Log.logger.info(name + " is not necessary for the chosen modules.");
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
        defaultValues.put("fdr", new Float(0.01));
        defaultValues.put("foldChange", new Float(1.5));
        defaultValues.put("tops", new Integer(0));
        defaultValues.put("geneIdType", new GeneIdType("None"));
        defaultValues.put("referenceOrganism", new Organism("Human"));
        return defaultValues;
    }

    //TODO
    public static HashMap<String, Class> availableParamTypes = getAvailableParamTypes();

    public static HashMap<String, Class> getAvailableParamTypes(){
        HashMap<String, Class> classes = new HashMap<>();
        classes.put(Integer.class.getSimpleName(), Integer.class);
        classes.put(Float.class.getSimpleName(), Float.class);
        classes.put(GeneIdType.class.getSimpleName(), GeneIdType.class);
        classes.put(Organism.class.getSimpleName(), Organism.class);

        return classes;
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