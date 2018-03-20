package peridot.CLI;

import peridot.AnalysisData;
import peridot.AnalysisParameters;
import peridot.Archiver.Spreadsheet;
import peridot.IndexedString;
import peridot.Log;
import peridot.script.RModule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Level;

/**
 * A file with all the definitions necessary to do an analysis.
 * Created by pentalpha on 01/05/17.
 */
public class AnalysisFile {
    public boolean valid, allInfo;

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
        return allInfo;
    }

    public static void createExampleFileFor(File countReadsFile){
        try {
            String content = "";
            Spreadsheet.Info info = new Spreadsheet.Info(countReadsFile);
            File parentDir = countReadsFile.getParentFile();
            if (parentDir == null){
                parentDir = new File(peridot.Archiver.Manager.getCurrentWorkingDir());
            }
            content += AnalysisFileParser.dataStr + " " + countReadsFile.getAbsolutePath() + "\n";
            content += AnalysisFileParser.sepStr + " " + info.separator;
            SortedMap<IndexedString, String> conditions = AnalysisData.getConditionsFromExpressionFile(countReadsFile, info);
            File condFile = new File(countReadsFile.getAbsolutePath() + ".conditions");
            AnalysisData.createConditionsFile(condFile, conditions, false, true);
            content += AnalysisFileParser.thresholdStr + " 5\n";
            content += AnalysisFileParser.roundingModeStr + " HALF_UP\n";
            //content += AnalysisFileParser.integersOnlyStr + " " + (info.dataType == Spreadsheet.DataType.Int) + "\n";
            content += AnalysisFileParser.labelsOnFirstColStr + " " + info.getLabelsOnFirstCol() + "\n";
            content += AnalysisFileParser.headerOnFirstLineStr + " " + info.getHeaderOnFirstLine() + "\n\n";
            content += AnalysisFileParser.conditionsStr + " " + condFile.getAbsolutePath() + "\n\n";
            content += AnalysisFileParser.saveAtStr + " "
                    + parentDir.getAbsolutePath()
                    + File.separator + countReadsFile.getName() + "-output" + "\n\n";

            content += AnalysisFileParser.modulesStartStr + "\n";
            for(String pack : RModule.getAvailableAnalysisModules()){
                content += "#" + pack + "\n";
            }
            for(String script : RModule.getAvailablePostAnalysisModules()){
                content += "#" + script + "\n";
            }
            content += AnalysisFileParser.endStr + "\n\n";

            content += AnalysisFileParser.paramsStartStr + "\n";
            Map<String, Object> defaultValues = AnalysisParameters.getDefaultValues();
            for(Map.Entry<String, Class> entry : RModule.getRequiredParametersFromModules().entrySet()){
                if(defaultValues.containsKey(entry.getKey())){
                    content += entry.getValue().getSimpleName() + " "
                            + entry.getKey() + "=" + defaultValues.get(entry.getKey()).toString() + "\n";
                }else{
                    content += entry.getValue().getSimpleName() + " "
                            + entry.getKey() + "=INSERT_VALUE" + "\n";
                }

            }
            content += AnalysisFileParser.endStr + "\n\n";

            File exampleFile = new File(countReadsFile.getAbsolutePath() + ".af");
            if(exampleFile.exists()){
                exampleFile.delete();
            }
            exampleFile.createNewFile();
            FileWriter writer = new FileWriter(exampleFile);
            BufferedWriter buffWriter = new BufferedWriter(writer);

            buffWriter.write(content);

            buffWriter.close();
            writer.close();

            System.out.println("Created " + exampleFile.getAbsolutePath());
        }catch(IOException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static String getSpecification(){
        String string = "\n\n\t---SPECIFICATION---\n" +
                "# Comments start with '#'\n";

        string += "" +
                "[data] path/to/file\n" +
                "# Specify count reads table file. ##############\n" +
                "# Only plain text with cells separated by ######\n" +
                "# tabs, spaces, commas and semi-colons are #####\n" +
                "# supported. ###################################\n" +
                "[separator] \",\"\n" +
                "# Cell separator character, can be \" \", \"\\t\",####\n" +
                "# \",\" or \";\". ##################################\n";
        string += "" +
                AnalysisFileParser.thresholdStr + " Integer\n" +
                "# Minimum count read value to be considered. ###\n" +
                "# Lines without values equal/above are ignored #\n" +
                AnalysisFileParser.roundingModeStr + " HALF_UP|HALF_DOWN|UP|DOWN\n" +
                "# The diff. expression packages require int ####\n" +
                "# values, but the values at the count reads ####\n" +
                "# can be real numbers. In that case, r-peridot #\n" +
                "# rounds the number according to a rounding ####\n" +
                "# rule: ########################################\n" +
                "#     UP: Round away from zero #################\n" +
                "#     DOWN: Round towards zero #################\n" +
                "#     HALF_UP: Round to the closest integer ####\n" +
                "#         but if the value ends with *.5, ######\n" +
                "#         round UP. ############################\n" +
                "#     HALF_DOWN: Round to the closest integer ##\n" +
                "#         but if the value ends with *.5, ######\n" +
                "#         round DOWN. ##########################\n";
        string +="" +
                "# Meta-data about the count reads file:\n" +
                //AnalysisFileParser.integersOnlyStr + " True|False\n" +
                //"# True if there are only integers on the file, #\n" +
                //"# False otherwise. #############################\n" +
                AnalysisFileParser.labelsOnFirstColStr + " True|False\n" +
                "# True if there are labels on the first column,#\n" +
                "# False otherwise. #############################\n" +
                AnalysisFileParser.headerOnFirstLineStr + " True|False\n" +
                "# True if there is a header on the first line, #\n" +
                "# False otherwise. #############################\n\n";
        string += "" +
                "# Specify conditions (groups) of samples:\n" +
                "[conditions] path/to/file.conditions\n";
        string += "" +
                "# Directory where the results will be saved: \n" +
                AnalysisFileParser.saveAtStr + " path/to/dir/\n" +
                "# The directory must exist #####################\n\n";
        string += "" +
                "################################################\n" +
                "### Example Conditions File: ###################\n" +
                "sample1\tgroupX\n" +
                "sample2\tgroupX\n" +
                "sample3\tgroupZ\n" +
                "sample4\tgroupY\n" +
                "sample5\tgroupY\n" +
                "sample6\tnot-use\n" +
                "### Use tabulation, not ',' or ' ' to separate #\n" +
                "### sample from condition. #####################\n" +
                "### Sample names must be on their original #####\n" +
                "### order. #####################################\n" +
                "### No comments on this file, only content. ####\n" +
                "### The 'not-use' condition is ignored. ########\n" +
                "################################################\n\n";
        string += "" +
                "# Specifying Modules:\n" +
                "[modules]\n" +
                "moduleName1\n" +
                "moduleName2\n" +
                "moduleName3\n" +
                "[/end]\n" +
                "### Use r-peridot ls <module-name> to verify ###\n" +
                "### if a module depends on the results of ######\n" +
                "### other module. If it does, the module it ####\n" +
                "### depends on must be listed too. #############\n\n";
        string += "" +
                "# Specifying parameters (general and specific):\n" +
                "[parameters]\n" +
                "Type parameterName=value\n" +
                "Type otherParameter=otherValue\n" +
                "Type targetModule::aSpecificParameter=anotherValue\n" +
                "[/end]\n" +
                "### All parameters needed by the modules ######\n" +
                "### must have values specified for them here. #\n" +
                "### Type can be Float, Integer or GeneIdType. #\n" +
                "### 'targetModule' can be any module. #########\n";
        string += "" +
                "######################################################\n" +
                "### Don't be scared, it's not that hard. The command #\n" +
                "### 'r-peridot run --create-example path/to/file' ####\n" +
                "### can create a file with most of these #############\n" +
                "### descriptions ready. You only have to modify the ##\n" +
                "### conditions of each sample and the parameter's ####\n" +
                "### default values. ##################################\n" +
                "######################################################\n";
        return string;
    }

}
