package peridot.CLI.Commands;

import peridot.CLI.Command;
import peridot.CLI.PeridotCmd;
import peridot.script.RScript;
import peridot.script.Task;

import java.io.File;

/**
 * Created by pentalpha on 01/05/17.
 */
public class RUN extends Command{
    public static String specification = getSpecification();
    public static final String createExampleOperation = "--create-example";
    public static final String specifyOperation = "--specification";

    public RUN(String[] args){
        super(args);
    }



    public void evaluateArgs() throws Command.CmdParseException {
        if(args.length != 1 && args.length != 2){
            fail("Invalid arguments, 'r-peridot run [args]' takes one or two arguments, only.");
        }else if(args.length == 1) {
            String arg0 = new String(args[0]);
            if (isHelpArg(args[0])) {

            }else if(arg0.equals(specifyOperation)){

            }else{
                File analysisFile = new File(args[0]);
                if(analysisFile.exists() == false){
                    fail("The file '" + analysisFile.getAbsolutePath() + "' does not exists.");
                }
            }
        }else if(args.length == 2){
            if(args[0].equals(createExampleOperation) == false){
                fail("Invalid argument: '" + args[0] +"'. Did you try '--create-example'?");
            }else{
                File countReadsFile = new File(args[1]);
                if (countReadsFile.exists() == false){
                   fail("The file '" + countReadsFile.getAbsolutePath() + "' does not exists.");
                }else if (peridot.Archiver.Spreadsheet.fileIsCSVorTSV(countReadsFile) == false){
                    fail(countReadsFile.getName() + ": invalid file extension." +
                            " Try again with a .csv or .tsv file");
                }
            }
        }

    }

    public void run(){
        if(args.length == 1){
            if(isHelpArg(args[0])){
                this.printDetails();
            }else if (args[0].equals(specifyOperation)){
                System.out.println(getSpecification());
            }else{
                File file = new File(args[0]);
                /*AnalysisFile analysisFile = new AnalysisFile(file);
                if(analysisFile.isValid() == false){

                }else if(analysisFile.hasAllInfo() == false){

                }else{
                    peridot.script.Task task = PeridotCmd.start(analysisFile);
                }*/
            }
        }else if(args.length == 2){
            File countReadsFile = new File(args[1]);
            //PeridotCmd.createAnalysisFileFor(countReadsFile);
        }
    }

    public void defineCmdNameAndDetails(){
        this.commandStr = "run";
        this.detail = "R-Peridot's Command: run\n" +
                "Options:\n" +
                "\trun path/to/analysis/file\n" +
                "\t\tMake an analysis based on a file with\n" +
                "\t\tall the necessary information.\n" +
                "\trun --create-example path/to/count/reads/file\n" +
                "\t\tCreates a file with most of the\n" +
                "\t\tinformation ready. The user only have \n" +
                "\t\tto modify the conditions of each sample,\n" +
                "\t\tchoose between the modules listed\n" +
                "\t\tand modify the parameter's default values.\n" +
                "\t\tThe conditions are created on the file\n." +
                "\t\tpath/to/count/reads/file.conditions.\n\n" +
                "\trun --specification\n" +
                "\t\tPrints the specification of the analysis file.\n\n" +
                "\trun --help\n\t\tDisplays this message\n\n";
    }

    public static String getSpecification(){
        String string = "\n\n\t---SPECIFICATION---\n# Comments start with '#'\n";

        string += "" +
                "# Specify count reads:\n" +
                "[data] path/to/file.tsv\n" +
                "### TSV and CSV are supported. #################\n\n";
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
                "[/modules]\n" +
                "### Use r-peridot ls <module-name> to verify ###\n" +
                "### if a module depends on the results of ######\n" +
                "### other module. If it does, the module it ####\n" +
                "### depends on must be listed too. #############\n\n";
        string += "" +
                "# Specifying parameters (general and specific):\n" +
                "[parameters]\n" +
                "parameterName=value\n" +
                "otherParameter=otherValue\n" +
                "aSpecificParameter::targetModule=anotherValue\n" +
                "[/parameters]\n" +
                "### All parameters needed by the modules ######\n" +
                "### must have values specified for them here. #\n\n";
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
