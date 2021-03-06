package peridot.CLI.Commands;

import peridot.CLI.AnalysisFile;
import peridot.CLI.AnalysisFileParser;
import peridot.CLI.Command;
import peridot.Log;
import peridot.Operations;
import peridot.script.Task;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Command to create a example Analysis File or run an Analysis File to make an analysis.
 * Created by pentalpha on 01/05/17.
 */
public class RUN extends Command{
    public static String specification = AnalysisFile.getSpecification();
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
                }
            }
        }

    }

    public void run(){
        if(args.length == 1){
            if(isHelpArg(args[0])){
                this.printDetails();
            }else if (args[0].equals(specifyOperation)){
                System.out.println(AnalysisFile.getSpecification());
            }else{
                File file = new File(args[0]);
                AnalysisFile analysisFile = AnalysisFileParser.make(file);
                if(analysisFile.isValid() == false){
                    Log.logger.severe("Fatal Error: Invalid info. on '"
                            + file.getAbsolutePath() + "'" +
                            ", cannot run analysis.");
                }else if(analysisFile.hasAllInfo() == false){
                    String errorMsg = "Fatal Error: '" + file.getName() + "'" +
                    " does not have all the necessary information to run an" +
                    " analysis.";
                    if(!analysisFile.hasConditions){
                        errorMsg += "\n\t- Conditions not defined.";
                    }else if(!analysisFile.hasData){
                        errorMsg += "\n\t- Count reads not defined.";
                    } else if(!analysisFile.hasModules){
                        errorMsg += "\n\t- Modules not chosen.";
                    } else if(!analysisFile.hasParams){
                        errorMsg += "\n\t- Invalid parameters.";
                    }
                    Log.logger.severe(errorMsg);
                }else{
                    try{
                        Operations.prepare(analysisFile);
                    }catch(NumberFormatException ex){
                        ex.printStackTrace();
                        Log.logger.severe(ex.getMessage());
                        return;
                    }
                    peridot.script.Task task = Operations.start(analysisFile);
                    waitForEnd(task, analysisFile);
                }
            }
        }else if(args.length == 2){
            File countReadsFile = new File(args[1]);
            AnalysisFile.createExampleFileFor(countReadsFile);
        }
    }

    public void waitForEnd (Task task, AnalysisFile analysisFile){
        task.join();
        System.out.println("\nFinished processing task.");

        Map<String, Set<String>> modSets = task.getScriptSets();
        Collection<String> successfulScripts = modSets.get("Successful");
        Collection<String> noDiffExpFound = modSets.get("No Differential Expression Found");
        Collection<String> failedScripts = modSets.get("Failed");
        System.out.println("Failed: ");
        if(failedScripts.size() == 0){
            System.out.println("\tNone");
        }else{
            for(String s : failedScripts){
                System.out.println("\t" + s);
            }
        }

        System.out.println("Success: ");
        if(successfulScripts.size() == 0){
            System.out.println("\tNone");
        }else{
            for(String s : successfulScripts){
                System.out.println("\t" + s);
            }
            if(noDiffExpFound.size() > 0){
                System.out.println("The following modules did not found differential expression:");
                for(String modName : noDiffExpFound){
                    System.out.println("\t" + modName);
                }
            }
        }

        boolean success = Operations.saveResultsAt(analysisFile.outputFolder);
    }

    public void defineCmdNameAndDetails(){
        this.commandStr = "run";
        this.detail = "R-Peridot's Command: run\n" +
                "Options:\n" +
                "\trun path/to/analysis/file.af\n" +
                "\t\tMake an analysis based on a file (.af) with\n" +
                "\t\tall the necessary information.\n" +
                "\trun --create-example path/to/count/reads/file\n" +
                "\t\tCreates a '.af', named path/to/count/reads/file.af,\n" +
                "\t\tfile with most of the information ready.\n" +
                "\t\tThe user only has to modify the conditions of\n" +
                "\t\teach sample, choose between the modules listed\n" +
                "\t\tand modify the parameter's default values.\n" +
                "\t\tThe conditions file is created at\n." +
                "\t\tpath/to/count/reads/file.conditions.\n" +
                "\t\tAll samples are set to not be used, you have to\n" +
                "\t\tedit this file to set the conditions.\n\n" +
                "\trun --specification\n" +
                "\t\tPrints the specification of the analysis file (.af).\n\n" +
                "\trun --help\n\t\tDisplays this message\n\n";
    }
}
