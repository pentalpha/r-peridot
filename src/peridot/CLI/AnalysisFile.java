package peridot.CLI;

import peridot.AnalysisParameters;
import peridot.Archiver.Spreadsheet;
import peridot.Global;
import peridot.Log;
import peridot.RNASeq;
import peridot.script.RScript;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Created by pentalpha on 01/05/17.
 */
public class AnalysisFile {
    public boolean valid, allInfo;

    public Set<String> scriptsToExec;
    public AnalysisParameters params;
    public Map<String, AnalysisParameters> specificParams;
    public RNASeq expression;

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

    }

    public static String getSpecification(){
        String string = "\n\n\t---SPECIFICATION---\n" +
                "# Comments start with '#'\n";

        string += "" +
                "# Specify count reads:\n" +
                "[data] path/to/file.tsv\n" +
                "### TSV and CSV are supported. #################\n\n";
        string +="" +
                "# Meta-data about the count reads file:\n" +
                AnalysisFileParser.integersOnlyStr + " True|False\n" +
                "# True if there are only integers on the file, #\n" +
                "# False otherwise. #############################\n" +
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
