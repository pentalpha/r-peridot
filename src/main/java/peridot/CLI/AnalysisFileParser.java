package peridot.CLI;

import org.json.JSONArray;
import org.json.JSONObject;
import peridot.*;
import peridot.Archiver.Manager;
import peridot.Archiver.Spreadsheet;
import peridot.Archiver.Spreadsheet.Info;
import peridot.script.AnalysisModule;
import peridot.script.RModule;
import peridot.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Parses an AnalysisFile file to the AnalysisFile object.
 * Created by pentalpha on 02/05/17.
 */
public class AnalysisFileParser {
    AnalysisFile analysisFile = null;

    boolean valid, allInfo;
    boolean hasData;
    boolean hasConditions;
    boolean hasModules;
    boolean hasParams;

    int threshold, numberOfAnalysisModules;
    String roundingMode;

    File countReadsFile;
    File conditionsFile;
    File saveFolder;
    Spreadsheet.Info info;
    Set<String> modules;
    Map<String, Class> paramTypes;
    Map<String, String> params;
    Map<String, Map<String, String>> specificParams;
    String separatorChar;

    public AnalysisFileParser(File file){
        analysisFile = null;

        valid = true;
        allInfo = false;

        hasData = false;
        hasConditions = false;
        hasModules = false;
        hasParams = false;
        countReadsFile = null;
        conditionsFile = null;
        info = new Spreadsheet.Info();
        modules = new TreeSet<>();
        params = new HashMap<>();
        specificParams = new HashMap<>();
        paramTypes = new HashMap<>();
        numberOfAnalysisModules = 0;

        parse(file);
    }

    public static AnalysisFile make(File file){
        AnalysisFileParser parser = new AnalysisFileParser(file);
        return parser.analysisFile;
    }

    private void parse(File file) {
        analysisFile = new AnalysisFile();
        try {
            String jsonContent = Manager.fileToString(file).toString();
            JSONObject json = new JSONObject(jsonContent);

            parseSimpleFields(json);

            JSONArray modulesArray = json.getJSONArray(modulesStr);
            parseModules(modulesArray);

            JSONObject paramsJson = json.getJSONObject(paramsStr);
            parseParams(paramsJson);

            setExpression();
            setModules();
            analysisFile.outputFolder = saveFolder;
            analysisFile.params = getAnalysisParamsFromMap(params, null);
            analysisFile.specificParams = getSpecificParams();

            for(Map.Entry<String, Class> pair : analysisFile.params.requiredParameters.entrySet()){
                boolean found = false;
                for(Map.Entry<String, Object> pair2 : analysisFile.params.parameters.entrySet()){
                    if(pair.getKey().equals(pair2.getKey())){
                        found = true;
                        break;
                    }
                }
                if(found == false){
                    throw new ParseException("Error: The parameter " +
                            pair.getKey() + " has not been specified.");
                }
            }
            hasParams = true;

            peridot.ConsensusThreshold minimumPackages = (peridot.ConsensusThreshold) analysisFile.params.parameters.get(
                    "minimumPackagesForConsensus"
            );
            if(minimumPackages.toInt() > numberOfAnalysisModules){
                throw new ParseException("Your consensus threshold is greater than the number of analysis modules.");
            }
        }
        catch (ParseException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            valid = false;
        }
        catch(IOException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            valid = false;
        }
        catch (NumberFormatException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            valid = false;
        }

        analysisFile.hasData = hasData;
        analysisFile.hasConditions = hasConditions;
        analysisFile.hasModules = hasModules;
        analysisFile.hasParams = hasParams;

        analysisFile.valid = valid;
    }

    private void parseSimpleFields(JSONObject json) throws ParseException{
        countReadsFile = new File(peridot.Archiver.Manager.makeTildeIntoHomeDir(
                json.getString(AnalysisFileParser.dataStr)
        ));
        if (countReadsFile.exists() == false) {
            throw new ParseException(countReadsFile.getAbsolutePath() + ": file does not exists.");
        }
        conditionsFile = new File(peridot.Archiver.Manager.makeTildeIntoHomeDir(
                json.getString(conditionsStr)
        ));
        if (conditionsFile.exists() == false) {
            throw new ParseException(conditionsFile.getAbsolutePath() + " file does not exists.");
        }
        saveFolder = new File(peridot.Archiver.Manager.makeTildeIntoHomeDir(
                json.getString(saveAtStr)
        ));
        if(saveFolder.isFile()){
            throw new ParseException("The output directory " +
                    "'" + saveFolder.getAbsolutePath() + "' is a file, not a directory.");
        }

        info.setLabelsOnFirstCol(json.getBoolean(labelsOnFirstColStr));
        info.setHeaderOnFirstLine(json.getBoolean(headerOnFirstLineStr));
        threshold = json.getInt(thresholdStr);
        roundingMode = json.getString(roundingModeStr);

        String separatorStr = json.getString(sepStr);
        separatorStr = separatorStr.replace("\"", "");
        if(separatorStr.equals("") || separatorStr.equals(" ")){
            separatorChar = " ";
        }else if(separatorStr.equals(",")){
            separatorChar = ",";
        }else if(separatorStr.equals("\\t") || separatorStr.equals("\t")){
            separatorChar = "\t";
        }else if(separatorStr.equals(";")){
            separatorChar = ";";
        }else if(separatorStr.length() > 0){
            separatorChar = separatorStr;
        }else{
            separatorChar = null;
        }
        if(separatorChar != null){
            info.separator = separatorChar;
        }
    }

    private void parseModules(JSONArray modulesArray) throws ParseException{
        List<Object> modulesList = modulesArray.toList();
        for (Object obj : modulesList){
            String moduleName = obj.toString();
            if(moduleName.startsWith("#")){
                Log.logger.info(moduleName + " module name starting with #, ignoring it.");
            }else {
                if(!RModule.availableModules.containsKey(moduleName)) {
                    throw new ParseException("Error: " + moduleName + " is not a" +
                            "valid module.");
                }else{
                    modules.add(moduleName);
                    if(RModule.getAvailableAnalysisModules().contains(moduleName)){
                        numberOfAnalysisModules += 1;
                    }
                }
            }
        }
    }

    private void parseParams(JSONObject paramsJson) throws ParseException{
        for (String paramName : paramsJson.keySet()){
            String name = null;
            String moduleName = null;
            if(paramName.contains(paramModSeparator)){
                String[] modAndName = paramName.split(paramModSeparator);
                moduleName = modAndName[0];
                name = modAndName[1];
            }else{
                name = paramName;
            }

            String[] valueAndType = paramsJson.getJSONArray(name).toList().toArray(new String[0]);
            if(valueAndType.length != 2){
                throw new ParseException("Error: No parameter type: " + name);
            }
            Class paramType = null;

            if(AnalysisParameters.availableParamTypes.containsKey(valueAndType[1])){
                paramType = AnalysisParameters.availableParamTypes.get(valueAndType[1]);
            }else {
                throw new ParseException("Error: The type " + valueAndType[1] + " is not valid.");
            }
            String value = valueAndType[0];

            if(moduleName != null){
                if(specificParams.containsKey(name) == false){
                    specificParams.put(name, new HashMap<>());
                }
                specificParams.get(name).put(moduleName, value);
                paramTypes.put(name, paramType);
            }else{
                params.put(name, value);
                paramTypes.put(name, paramType);
            }
        }
    }

    public void setExpression() throws IOException, AnalysisFileParser.ParseException{
        if(conditionsFile != null && countReadsFile != null && info.allInfoSet()){
            if(!info.getHeaderOnFirstLine() && info.getLabelsOnFirstCol()){
                info.setFirstCellPresent(true);
            }else{
                info.setFirstCellPresent(Info.getFirstCellPresent(countReadsFile, separatorChar));
            }

            analysisFile.expression = new AnalysisData(countReadsFile, conditionsFile, info, roundingMode, threshold);
            hasData = true;
            hasConditions = true;
            System.out.println("[Gene Expression Loaded]");
        }else if(!info.allInfoSet()){
            Log.logger.severe("Missing informations about the count reads file.");
        }else if(countReadsFile == null){
            Log.logger.severe("Count reads file not defined.");
        }else if(conditionsFile == null){
            Log.logger.severe("Conditions file not defined");
        }
    }

    public void setModules() throws ParseException {
        boolean anyPackages = false;
        for(String module : modules){
            if(RModule.availableModules.get(module) instanceof AnalysisModule){
                anyPackages = true;
            }
            Collection<String> required_mods = RModule.availableModules.get(module).requiredScripts;
            int number_of_required_mods = RModule.availableModules.get(module).requiredScripts.size();
            int number_of_required_mods_installed = 0;
            if(number_of_required_mods > 0){
                for(String dep : required_mods){
                    if(modules.contains(dep) == false){
                        if(RModule.availableModules.get(module).needsAllDependencies){
                            throw new ParseException("Error: " + module + " depends on " + dep +
                                    ", but " + dep + " was not chosen to be executed.");
                        }
                    }else{
                        number_of_required_mods_installed += 1;
                    }
                }
                if(number_of_required_mods_installed < 1){
                    throw new ParseException("Error: None of " + module + "'s dependencies have been chosen, so it cannot be executed.");
                }
            }

        }
        if(anyPackages){
            analysisFile.scriptsToExec = modules;
            hasModules = true;
        }else{
            throw new ParseException("Error: No AnalysisData module was chosen.");
        }
    }

    public Map<String, Class> getRequiredParamsFromModules(){
        Map<String, Class> req = new HashMap<>();
        for(String modName : analysisFile.scriptsToExec){
            for(Map.Entry<String, Class> pair : RModule.availableModules.
                    get(modName).requiredParameters.entrySet()){
                req.put(pair.getKey(), pair.getValue());
            }
        }
        return req;
    }

    public AnalysisParameters getAnalysisParamsFromMap(Map<String, String> params,
                                                       Map<String, Class> requiredParams){
        AnalysisParameters ap = new AnalysisParameters();
        if(requiredParams == null){
            ap.requiredParameters = getRequiredParamsFromModules();
        }
        for(Map.Entry<String, String> pair : this.params.entrySet()){
            Object value = null;
            if(paramTypes.get(pair.getKey()) == Float.class){
                value = Float.valueOf(pair.getValue());
            }else if(paramTypes.get(pair.getKey()) == Integer.class){
                value = Integer.valueOf(pair.getValue());
            }else if(paramTypes.get(pair.getKey()) == GeneIdType.class){
                value = new GeneIdType(pair.getValue());
            }else if(paramTypes.get(pair.getKey()) == Organism.class){
                value = new Organism(pair.getValue());
            }else if(paramTypes.get(pair.getKey()) == ConsensusThreshold.class){
                try{
                    value = new ConsensusThreshold(pair.getValue());
                }catch(NumberFormatException ex){
                    ex.printStackTrace();
                    Log.logger.warning("Invalid Consensus Threshold value: '" + pair.getValue() + "', using default value.");
                    value = new ConsensusThreshold();
                }
            }
            ap.passParameter(pair.getKey(), value);
        }
        return ap;
    }

    public Map<String, AnalysisParameters> getSpecificParams(){
        Map<String, Class> req = getRequiredParamsFromModules();
        HashMap<String, AnalysisParameters> sParams = new HashMap<>();
        for(Map.Entry<String, Map<String, String>> pair : specificParams.entrySet()){
            sParams.put(pair.getKey(), getAnalysisParamsFromMap(pair.getValue(), req));
        }
        return sParams;
    }

    public final static String dataStr = "data";
    public final static String conditionsStr = "conditions";
    public final static String modulesStr = "modules";
    public final static String paramsStr = "parameters";
    public final static String paramModSeparator = "::";
    public final static String labelsOnFirstColStr = "labels";
    public final static String headerOnFirstLineStr = "header";
    public final static String saveAtStr = "output";
    public final static String thresholdStr = "count-reads-threshold";
    public final static String roundingModeStr = "rounding-mode";
    public final static String sepStr = "separator";

    public static class ParseException extends Exception{
        public ParseException(String message){
            super(message);
        }
    }
}
