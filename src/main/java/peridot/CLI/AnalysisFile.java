package peridot.CLI;

import org.json.JSONArray;
import org.json.JSONObject;
import peridot.AnalysisData;
import peridot.AnalysisParameters;
import peridot.Archiver.Manager;
import peridot.Archiver.Spreadsheet;
import peridot.IndexedString;
import peridot.Log;
import peridot.script.RModule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * A file with all the definitions necessary to do an analysis.
 * Created by pentalpha on 01/05/17.
 */
public class AnalysisFile {
    public boolean valid, allInfo;
    public boolean hasData, hasConditions, hasModules, hasParams;

    public Set<String> scriptsToExec;
    public AnalysisParameters params;
    public Map<String, AnalysisParameters> specificParams;
    public AnalysisData expression;
    public File outputFolder;

    public AnalysisFile(File file){
        valid = true;
        allInfo = false;

    }

    public AnalysisFile(){

    }

    public boolean isValid(){
        return valid;
    }

    public boolean hasAllInfo() {
        return hasData && hasConditions && hasModules && hasParams;
    }

    public static void createExampleFileFor(File countReadsFile){
        try {
            File parentDir = countReadsFile.getParentFile();
            if (parentDir == null) {
                parentDir = new File(peridot.Archiver.Manager.getCurrentWorkingDir());
            }
            Spreadsheet.Info info = new Spreadsheet.Info(countReadsFile);

            JSONObject json = new JSONObject();
            json.put(AnalysisFileParser.dataStr, countReadsFile.getAbsolutePath());
            json.put(AnalysisFileParser.sepStr, info.separator);

            SortedMap<IndexedString, String> conditions;
            File condFile = new File(countReadsFile.getAbsolutePath() + ".conditions");
            if (condFile.exists()) {
                conditions = AnalysisData.loadConditionsFromFile(
                        condFile, AnalysisData.getIndexedSamplesFromFile(countReadsFile, info)
                );
            } else {
                conditions = AnalysisData.getConditionsFromExpressionFile(countReadsFile, info);
                AnalysisData.createConditionsFile(condFile, conditions, false, true);
            }
            json.put(AnalysisFileParser.conditionsStr, condFile.getAbsolutePath());
            json.put(AnalysisFileParser.thresholdStr, 5);
            json.put(AnalysisFileParser.roundingModeStr, "HALF_UP");
            json.put(AnalysisFileParser.labelsOnFirstColStr, info.getLabelsOnFirstCol());
            json.put(AnalysisFileParser.headerOnFirstLineStr, info.getHeaderOnFirstLine());
            json.put(AnalysisFileParser.saveAtStr, parentDir.getAbsolutePath()
                    + File.separator + countReadsFile.getName() + "-output");

            JSONArray modulesArray = new JSONArray();
            for (String pack : RModule.getAvailableAnalysisModules()) {
                modulesArray.put("#" + pack);
            }
            for (String pack : RModule.getAvailablePostAnalysisModules()) {
                if(!pack.equals("VennDiagram")){
                    modulesArray.put("#" + pack);
                }else{
                    modulesArray.put(pack);
                }

            }
            json.put(AnalysisFileParser.modulesStr, modulesArray);

            Map<String, Object> defaultValues = AnalysisParameters.getDefaultValues();
            JSONObject params = new JSONObject();
            for (Map.Entry<String, Class> entry : RModule.getRequiredParametersFromModules().entrySet()) {
                List<String> value = new ArrayList<>();

                if (defaultValues.containsKey(entry.getKey())) {
                    value.add(defaultValues.get(entry.getKey()).toString());
                } else {
                    Log.logger.severe(entry.getKey() + " has no default value, writing 'INSERT_VALUE', instead.");
                    value.add("INSERT_VALUE");
                }
                value.add(entry.getValue().getSimpleName());
                JSONArray valueArray = new JSONArray(value);
                params.put(entry.getKey(), valueArray);
            }

            json.put(AnalysisFileParser.paramsStr, params);

            File exampleFile = new File(countReadsFile.getAbsolutePath() + "-analysis_file.json");
            if (exampleFile.exists()) {
                exampleFile.delete();
            }
            exampleFile.createNewFile();
            FileWriter writer = new FileWriter(exampleFile);
            BufferedWriter buffWriter = new BufferedWriter(writer);

            buffWriter.write(json.toString(4));

            buffWriter.close();
            writer.close();

            System.out.println("Created " + exampleFile.getAbsolutePath());
        }catch(Exception ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static String getSpecification(){
        String string = "\n---Analysis File Fields Specification---\n";
        string += "" +
                "\"" + AnalysisFileParser.dataStr + "\": \"path/to/file\"\n" +
                "# Specify count reads table file.\n" +
                "# Only plain text with cells separated by\n" +
                "# tabs, spaces, commas and semi-colons are\n" +
                "# supported.\n" +
                "\"" + AnalysisFileParser.sepStr + "\": \",\"\n" +
                "# Cell separator character, can be \" \", \"\\t\",\n" +
                "# \",\" or \";\".\n";
        string += "" +
                "\"" + AnalysisFileParser.thresholdStr + "\": \"[0<x<10]\"\n" +
                "# Minimum count read value to be considered.\n" +
                "# Lines without values equal/above are ignored \n" +
                "\"" + AnalysisFileParser.roundingModeStr + "\": \"[HALF_UP|HALF_DOWN|UP|DOWN]\"\n" +
                "# The diff. expression packages require int\n" +
                "# values, but the values at the count reads\n" +
                "# can be real numbers. In that case, r-peridot\n" +
                "# rounds the number according to a rounding\n" +
                "# rule: \n" +
                "#     UP: Round away from zero\n" +
                "#     DOWN: Round towards zero\n" +
                "#     HALF_UP: Round to the closest integer\n" +
                "#         but if the value ends with *.5,\n" +
                "#         round UP.\n" +
                "#     HALF_DOWN: Round to the closest integer\n" +
                "#         but if the value ends with *.5,\n" +
                "#         round DOWN.\n";
        string +="" +
                "# Meta-data about the count reads file:\n" +
                "\"" + AnalysisFileParser.labelsOnFirstColStr + "\": \"[True|False]\"\n" +
                "# True if there are labels on the first column,\n" +
                "# False otherwise.\n" +
                "\"" + AnalysisFileParser.headerOnFirstLineStr + "\": \"[True|False]\"\n" +
                "# True if there is a header on the first line,\n" +
                "# False otherwise.\n\n";
        string += "" +
                "# Specify conditions (groups) of samples:\n" +
                "\"" + AnalysisFileParser.conditionsStr + "\": \"path/to/file.conditions\"\n";
        string += "\n" +
                "#Example Conditions File: \n" +
                "sample1\tgroupX\n" +
                "sample2\tgroupX\n" +
                "sample3\tgroupZ\n" +
                "sample4\tgroupY\n" +
                "sample5\tgroupY\n" +
                "sample6\tnot-use\n" +
                "# Use tabulation, not ',' or ' ' to separate\n" +
                "# sample from condition.\n" +
                "# Sample names must be on their original\n" +
                "# order.\n" +
                "# No comments on this file, only content.\n" +
                "# The conditions marked as 'not-use' is ignored.\n\n";
        string += "" +
                "# Directory where the results will be saved: \n" +
                "\"" + AnalysisFileParser.saveAtStr + "\": \"path/to/dir/\"\n" +
                "# The parent directory must exist.\n";
        string += "" +
                "# Specifying Modules:\n" +
                "\"" + AnalysisFileParser.modulesStr + "\": [\n" +
                "\t\"moduleName1\",\n" +
                "\t\"moduleName2\",\n" +
                "\t\"#unusedModule\",\n" +
                "\t\"moduleName3\"\n" +
                "]\n" +
                "# Use r-peridot ls <module-name> to verify\n" +
                "# if a module depends on the results of\n" +
                "# other module. If it does, the module it\n" +
                "# depends on must be listed too.\n" +
                "# The example Analysis File created by R-Peridot\n" +
                "# has all module names starting with an '#', this\n" +
                "# means the module is not selected and will be ignored.\n" +
                "# remove the '#' to actually select a module for execution.\n\n";
        string += "" +
                "# Specifying parameters (general and specific):\n" +
                "\"" + AnalysisFileParser.paramsStr + "\" {\n" +
                "\t\"parameterName\": [\"value\", \"Type\"],\n" +
                "\t\"otherParameter\": [\"otherValue\", \"Type\"],\n" +
                "\t\"targetModule::aSpecificParameter\": [\"anotherValue\", \"Type\"]\n" +
                "}\n" +
                "# All parameters needed by the modules\n" +
                "# must have values specified for them here.\n" +
                "# Type can be Float, Integer,\n" +
                "# ConsensusThreshold, Organism or GeneIdType.\n" +
                "# 'targetModule' can be any module.\n";
        string += "\n" +
                "# Don't be scared, it's not that hard. The command \n" +
                "# 'r-peridot run --create-example path/to/file' \n" +
                "# can create a file with most of these \n" +
                "# descriptions ready, using JSON format. You only have to modify the \n" +
                "# conditions of each sample and the parameter's\n" +
                "# default values.\n\n";
        return string;
    }

}
