package peridot.CLI.Commands;

import peridot.Archiver.Places;
import peridot.CLI.AnalysisFile;
import peridot.CLI.AnalysisFileParser;
import peridot.CLI.Command;
import peridot.CLI.PeridotCmd;
import peridot.Log;
import peridot.script.Task;

import java.io.File;

/**
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
                System.out.println(AnalysisFile.getSpecification());
            }else{
                File file = new File(args[0]);
                AnalysisFile analysisFile = AnalysisFileParser.make(file);
                if(analysisFile.isValid() == false){
                    Log.logger.severe("Fatal Error: Invalid info. on '"
                            + file.getAbsolutePath() + "'" +
                            ", cannot run analysis.");
                }else if(analysisFile.hasAllInfo() == false){
                    Log.logger.severe("Fatal Error: '" + file.getName() + "'" +
                            "does not have all the necessary information to run an" +
                            " analysis.");
                }else{
                    peridot.script.Task task = PeridotCmd.start(analysisFile);
                    waitForEnd(task, analysisFile);
                }
            }
        }else if(args.length == 2){
            File countReadsFile = new File(args[1]);
            AnalysisFile.createExampleFileFor(countReadsFile);
        }
    }

    public void waitForEnd (Task task, AnalysisFile analysisFile){
        //System.out.print("Waiting for processing to finish");
        while(task.isProcessing()){

        }
        System.out.println("\nFinished processing task.");
        System.out.println("Failed: ");
        if(task.failedScripts.size() == 0){
            System.out.println("\tNone");
        }else{
            for(String s : task.failedScripts){
                System.out.println("\t" + s);
            }
        }

        System.out.println("Success: ");
        if(task.successfulScripts.size() == 0){
            System.out.println("\tNone");
        }else{
            for(String s : task.successfulScripts){
                System.out.println("\t" + s);
            }
            if(task.noDiffExpFound.size() > 0){
                System.out.println("The following modules did not found differential expression:");
                for(String modName : task.noDiffExpFound){
                    System.out.println("\t" + modName);
                }
            }
        }

        boolean success = PeridotCmd.saveResultsAt(analysisFile.outputFolder);
        if(!success){
            Log.logger.severe("Could not save the results to '" + analysisFile.outputFolder.getAbsolutePath() + "'." +
                    " The results are temporarily stored at " + Places.finalResultsDir.getAbsolutePath());
        }else{
            System.out.println("Output directory is " + analysisFile.outputFolder.getAbsolutePath());
        }
    }

    public void defineCmdNameAndDetails(){
        this.commandStr = "run";
        this.detail = "R-Peridot's Command: run\n" +
                "Options:\n" +
                "\trun path/to/analysis/file\n" +
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
