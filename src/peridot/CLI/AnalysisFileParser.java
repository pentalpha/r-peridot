package peridot.CLI;

import peridot.*;
import peridot.Archiver.Spreadsheet;
import peridot.script.RScript;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Created by pentalpha on 02/05/17.
 */
public class AnalysisFileParser {
    AnalysisFile analysisFile = null;

    boolean valid, allInfo;
    boolean hasData;
    boolean hasConditions;
    boolean hasModules;
    boolean hasParams;
    File countReadsFile;
    File conditionsFile;
    Spreadsheet.Info info;
    Set<String> modules;
    Map<String, Class> paramTypes;
    Map<String, String> params;
    Map<String, Map<String, String>> specificParams;

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

        parse(file);
    }

    public static AnalysisFile make(File file){
        AnalysisFileParser parser = new AnalysisFileParser(file);
        return parser.analysisFile;
    }

    private void parse(File file){
        Scanner scanner;
        String word = null;
        String line = null;
        try {
            scanner = new Scanner(file);
            while(scanner.hasNextLine()) {
                line = scanner.nextLine();
                if(line.length() > 1){
                    if(line.charAt(0) != '#'){
                        String[] words = Global.firstWordAndTheRest(line);
                        word = words[1];
                        if(words[1] == null){
                            Set<String> all = new TreeSet<>();
                            while(true){
                                line = scanner.nextLine();
                                if(line.contains(endStr)){
                                    break;
                                }else{
                                    all.add(line);
                                }
                            }
                            parseParamsAndModules(word, all);
                        }else{
                            String second = words[1];
                            parseSingleLineInfo(word, second);
                        }
                    }
                }
            }
            scanner.close();

            setExpression();
            setModules();
            analysisFile.params = getAnalysisParamsFromMap(params, null);
            analysisFile.specificParams = getSpecificParams();
        }
        catch (ParseException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            valid = false;
        }
        catch(IOException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            valid = false;
        }

        analysisFile.allInfo = hasData && hasConditions && hasModules && hasParams;
        analysisFile.valid = valid;
    }

    public void setExpression() throws IOException{
        if(conditionsFile != null && countReadsFile != null && info.allInfoSet()){
            info.setFirstCellPresent(!info.getHeaderOnFirstLine() || !info.getLabelsOnFirstCol());
            analysisFile.expression = new RNASeq(countReadsFile, conditionsFile, info);
        }
    }

    public void setModules() throws ParseException {
        boolean anyPackages = false;
        for(String module : modules){
            if(RScript.getAvailablePackages().contains(module)){
                anyPackages = true;
            }
            for(String dep : RScript.availableScripts.get(module).requiredScripts){
                if(modules.contains(dep) == false){
                    throw new ParseException("Error: " + module + " depends on " + dep +
                            ", but " + dep + " was not chosen to be executed.");
                }
            }
        }
        if(anyPackages){
            analysisFile.scriptsToExec = modules;
        }else{
            throw new ParseException("Error: No Analysis module was chosen.");
        }
    }

    public Map<String, Class> getRequiredParamsFromModules(){
        Map<String, Class> req = new HashMap<>();
        for(String modName : analysisFile.scriptsToExec){
            for(Map.Entry<String, Class> pair : RScript.availableScripts.
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
                value = new Float(pair.getValue());
            }else if(paramTypes.get(pair.getKey()) == Integer.class){
                value = new Integer(pair.getValue());
            }else if(paramTypes.get(pair.getKey()) == GeneIdType.class){
                value = new GeneIdType(pair.getValue());
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

    public void parseParamsAndModules(String type, Set<String> values) throws ParseException {
        if(type.equals(modulesStartStr)){
            for(String word : values){
                if(!RScript.availableScripts.containsKey(word)){
                    throw new ParseException("Error: " + word + " is not a" +
                            "valid module.");
                }else{
                    modules.add(word);
                }
            }
        }else if(type.equals(paramsStartStr)){
            parseParams(values);
        }else{
            throw new ParseException("Error: Expected [modules] or [parameters]: " + type);
        }
    }

    public void parseParams(Set<String> values) throws ParseException {
        for(String param : values){
            String[] typeAndParam = param.split(" ");
            if(typeAndParam.length != 2){
                throw new ParseException("Error: No parameter type: " + param);
            }
            Class paramType = null;
            if(typeAndParam[0].equals("Float")){
                paramType = Float.class;
            }else if(typeAndParam[0].equals("Integer")){
                paramType = Integer.class;
            }else if(typeAndParam[0].equals("GeneIdType")){
                paramType = GeneIdType.class;
            }else{
                throw new ParseException("Error: The type " + typeAndParam[0] + " is not valid.");
            }
            String[] attributeAndValue = param.split(typeAndParam[1]);
            if(attributeAndValue.length != 2){
                throw new ParseException("Error: Invalid parameter attribution: " + param);
            }
            String attr = attributeAndValue[0];
            String value = attributeAndValue[1];

            if(attr.contains(paramModSeparator)){
                String[] modAndAttr = attr.split(paramModSeparator);
                if(specificParams.containsKey(modAndAttr[0]) == false){
                    specificParams.put(modAndAttr[0], new HashMap<>());
                }
                specificParams.get(modAndAttr[0]).put(modAndAttr[1], value);
                paramTypes.put(modAndAttr[0], paramType);
            }else{
                this.params.put(attr, value);
                paramTypes.put(attr, paramType);
            }
        }
    }

    public void parseSingleLineInfo(String word, String second) throws ParseException {
        if(word.equals(dataStr)){
            countReadsFile = new File(second);
            if(countReadsFile.exists() == false){
                throw new ParseException(dataStr + " file does not exists.");
            }
        }else if(word.equals(conditionsStr)){
            conditionsFile = new File(second);
            if(conditionsFile.exists() == false){
                throw new ParseException(conditionsStr + " file does not exists.");
            }
        }else if(word.equals(integersOnlyStr)){
            boolean value = Boolean.parseBoolean(second);
            if(value) {
                info.dataType = Spreadsheet.DataType.Int;
            }else{
                info.dataType = Spreadsheet.DataType.Float;
            }
        }else if(word.equals(labelsOnFirstColStr)){
            boolean value = Boolean.parseBoolean(second);
            info.setLabelsOnFirstCol(value);
        }else if(word.equals(headerOnFirstLineStr)){
            boolean value = Boolean.parseBoolean(second);
            info.setHeaderOnFirstLine(value);
        }
    }

    public final static String dataStr = "[data]";
    public final static String conditionsStr = "[conditions]";
    public final static String modulesStartStr = "[modules]";
    public final static String paramsStartStr = "[parameters]";
    public final static String endStr = "[/end]";
    public final static String paramEqualStr = "=";
    public final static String paramModSeparator = "::";
    public final static String integersOnlyStr = "[only-integers]";
    public final static String labelsOnFirstColStr = "[labels]";
    public final static String headerOnFirstLineStr = "[header]";

    public static class ParseException extends Exception{
        public ParseException(String message){
            super(message);
        }
    }
}
