package peridot.script;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import peridot.Archiver.Places;
import peridot.Global;
import peridot.Log;
import peridot.script.RModule;
import peridot.tree.PipelineGraph;
import peridot.script.r.Interpreter;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.Files;
import java.lang.ProcessBuilder;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.TreeSet;

public class ModuleWorker implements Runnable {

    private static String[] defineCommand(RModule script, boolean firstRun, Interpreter interpreter){
        boolean needsEnv = false;
        String rPath = interpreter.exe;
        /*if(Places.rExec != null){
            if(Places.rExec.exists()){
                rPath = Places.rExec.getAbsolutePath();
                if(SystemUtils.IS_OS_UNIX){
                    needsEnv = true;
                }
            }else{
                rPath = "R";
            }
        }else{
            rPath = "R";
        }*/

        String notFirstExec;
        if(firstRun){
            notFirstExec = "0";
        }else{
            notFirstExec = "1";
        }
        String[] c = {rPath,
            "--no-save",
            "--no-restore",
            "--quiet",
            "--file="+script.scriptFile.getAbsolutePath(),
            "--args",
            script.workingDirectory.getAbsolutePath(),
            Places.finalResultsDir.getAbsolutePath(),
            script.resultsFolder.getAbsolutePath(),
            notFirstExec
        };
        String[] commandArray = c;

        if(needsEnv){
            String[] envVars = interpreter.getLinuxEnvVars();
            String[] commandArray2 = new String[commandArray.length+1+envVars.length];
            commandArray2[0] = "env";
            for(int i = 0; i < envVars.length; i++){
                commandArray2[i+1] = envVars[i];
            }
            for(int i = 0; i < commandArray.length; i++){
                commandArray2[i+1+envVars.length] = commandArray[i];
            }
            commandArray = commandArray2;
        }

        return commandArray;
    }

    private static String commandArrayToString(String[] commandArray){
        String commandString = "";
        for(int i = 0; i < commandArray.length; i++){
            commandString += commandArray[i];
            if(i != commandArray.length -1){
                commandString += " ";
            }
        }
        return commandString;
    }

    private static boolean detectPreviousRunn(RModule script){
        boolean firstRun = true;
        //Log.logger.info("checking for previous execution:");
        Iterator<File> it = FileUtils.iterateFiles(script.workingDirectory, null, false);
        while(it.hasNext()){
            if(it.next().getName().contains(".RData")){
                //Log.logger.info("Detected .RData in " + exec.script.workingDirectory);
                firstRun = false;
                break;
            }else{
                //Log.logger.info("Executing for the first time in " + exec.script.workingDirectory);
            }
        }
        return !firstRun;
    }

    private static ProcessBuilder makeProcessBuilder(RModule script, String[] commandArray){
        ProcessBuilder processBuilder = null;
        String commandString = commandArrayToString(commandArray);
        Log.logger.fine(commandString);
        if(SystemUtils.IS_OS_WINDOWS){
            processBuilder = new ProcessBuilder(commandArray);
        }else {
            File executeScript = new File(script.workingDirectory.getAbsolutePath()
                    + File.separator + "run.sh");
            try {
                if (executeScript.exists()) {
                    executeScript.delete();
                }
                executeScript.createNewFile();
                PrintWriter out = new PrintWriter(executeScript);
                out.println(commandString);
                out.close();
                //Log.logger.info(executeScript.getAbsolutePath() + " has been created");
            }catch(IOException ex) {
                Log.logger.severe("Error, could not create " + executeScript.getAbsolutePath());
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                return null;
            }

            /*StringBuilder sBuilder = peridot.Archiver.Manager.fileToString(executeScript);
            if(sBuilder == null){
                Log.logger.severe("Could not read the contents of " + executeScript.getAbsolutePath());
                return null;
            }
            System.out.println(sBuilder.toString());*/

            TreeSet<PosixFilePermission> permsSet = new TreeSet<>();
            permsSet.add(PosixFilePermission.OWNER_EXECUTE);
            permsSet.add(PosixFilePermission.OWNER_READ);
            try {
                Files.setPosixFilePermissions(executeScript.toPath(), permsSet);
                //runtime.exec("chmod 755 " + executeScript.getAbsolutePath());
                //Log.logger.info("Permissions have been granted to "
                //       + executeScript.getAbsolutePath());
            }catch(UnsupportedOperationException ex){
                Log.logger.severe("Error, could not grant permissions to "
                        + executeScript.getParentFile().getName()
                + " because the file does not support PosixFileAttributeView");
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                return null;
            }catch(ClassCastException ex){
                Log.logger.severe("Error, could not grant permissions to "
                        + executeScript.getParentFile().getName()
                        + " because some of the permissions granted are invalid.");
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                return null;
            }catch(SecurityException ex){
                Log.logger.severe("Error, could not grant permissions to "
                        + executeScript.getParentFile().getName()
                        + " because of an SecurityException.");
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                return null;
            }catch(IOException ex) {
                Log.logger.severe("Error, could not grant permissions to "
                        + executeScript.getParentFile().getName()
                + " because of an I/O Exception.");
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                return null;
            }
            String[] bashCmdArray = {"/bin/bash", executeScript.getAbsolutePath()};
            processBuilder = new ProcessBuilder(bashCmdArray);
        }
        processBuilder.redirectErrorStream(true);
        String saveIn = script.resultsFolder + File.separator + "output.txt";
        processBuilder.redirectOutput(new File(saveIn));
        return processBuilder;
    }

    private PipelineGraph pipeline;
    public Process process;
    public enum Status{
        WAITING_NEXT,
        RUNNING,
        FINISHED
    }

    public Status status;
    private RModule currentModule;
    //private Output currentOutput;
    private Interpreter interpreter;
    private int waiting_time;

    public ModuleWorker(PipelineGraph pipeline, Interpreter interpreter, int waiting_time){
        super();
        /*runtime = Runtime.getRuntime();
        this.exec = exec;
        preFailed = false;
        ready = false;*/
        //detectPreviousRunn();
        //defineCommand();
        //boolean pbMade = makeProcessBuilder();
        process = null;
        status = Status.WAITING_NEXT;
        this.pipeline = pipeline;
        this.interpreter = interpreter;
        this.waiting_time = waiting_time;
    }

    @Override
    public void run(){
        while(status != Status.FINISHED){
            try{Thread.sleep(waiting_time);}catch(InterruptedException ex){}
            if(status == Status.WAITING_NEXT){
                retrieve_module();
            }else if(status == Status.RUNNING){
                update_module();
            }
        }
    }

    private void retrieve_module(){
        RModule next = pipeline.getNext();
        if(next == null && pipeline.isFinished()){
            this.status = Status.FINISHED;
        }else if(next == null){
            //not finished, but no module available for now
        }else if(next != null){
            //start the execution of a module
            startModule(next);
        }
    }

    private void startModule(RModule next){
        boolean previous_run = detectPreviousRunn(next);
        String[] command = defineCommand(next, !previous_run, interpreter);

        ProcessBuilder processBuilder = makeProcessBuilder(next, command);
        if(processBuilder == null){
            //process definition failed
            module_failed(next);
        }else{
            //this.currentOutput = pipeline.getOutput(next.name);
            try{
                process = processBuilder.start();
                if(process == null){
                    throw new NullPointerException("Fatal Error: Failed to create "
                            + next.name + "'s process.");
                }else{
                    this.status = Status.RUNNING;
                    this.currentModule = next;
                    
                    //currentOutput.appendLine("Command line:\n" + Global.listOfWordsToLine(processBuilder.command())
                    //                        + "\n---------------");
                    pipeline.set_process(currentModule.name, process);
                    Log.logger.info(next.name + "'s process started.");
                }
            }catch(IndexOutOfBoundsException ex){
                //currentOutput.appendLine("IndexOutOfBoundsException: ");
                //currentOutput.appendLine(ex.getMessage());
                //currentOutput.appendLine(ex.toString());
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                module_failed(next);
                on_end();
            }catch(SecurityException ex){
                //currentOutput.appendLine("Security exception: ");
                //currentOutput.appendLine(ex.getMessage());
                //currentOutput.appendLine(ex.toString());
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                module_failed(next);
                on_end();
            }catch(IOException ex){
                ///currentOutput.appendLine("IO exception: ");
                //currentOutput.appendLine(ex.getMessage());
                //currentOutput.appendLine(ex.toString());
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                module_failed(next);
                on_end();
            }catch(NullPointerException ex){
                //currentOutput.appendLine("NullPointerException: ");
                //currentOutput.appendLine(ex.getMessage());
                //currentOutput.appendLine(ex.toString());
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                module_failed(next);
                on_end();
            }
        }
    }

    private void module_failed(RModule module){
        pipeline.markAsFailed(module.name);
    }

    private void module_sucess(RModule module){
        pipeline.markAsDone(module.name);
    }

    private void update_module(){
        /*InputStream iStream = process.getInputStream();
        InputStreamReader iStreamReader = new InputStreamReader(iStream);
        BufferedReader buffReader = new BufferedReader(iStreamReader);

        int chars_per_update = 50;
        int c = 0;
        try{
            c = buffReader.read();
            for (int i = 1; i <= chars_per_update; i++){
        */
        //if(pipeline.abort_flag.get(currentModule.name).booleanValue() == true)
        /*        if(c != -1){
                    currentOutput.appendChar((char)c);
                    if(c == '\n'){
                        break;
                    }
                }else{
                    break;
                }
                c = buffReader.read();
            }
        }catch(IOException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            c = -1;
            process.destroyForcibly();
            try{
                process.wait();
            }catch(InterruptedException ex2){
                //there's probably nothing more to do here
            }
        }*/

        if(!process.isAlive()){
            //process_ended
            on_end();
        }
    }

    private synchronized void saveResults(){
        //savingFlag.set(true);
        //currentOutput.appendLine("\n[End of input]");
        //peridot.Archiver.Manager.stringToFile(Places.finalResultsDir
        //        + File.separator + currentModule.name + ".output", currentOutput.getText());
        if(currentModule.verifyResults()){
            module_sucess(currentModule);
            //System.out.println("Saving results of " + script.name);
            currentModule.saveResults();
        }else{
            //successFlag.set(false);
            module_failed(currentModule);
        }
        currentModule.cleanLocalResults();
        //savingFlag.set(false);
    }

    private void on_end(){
        String exitMessage = currentModule.name + " finished.";
        Log.logger.info(exitMessage);
        saveResults();
        this.status = Status.WAITING_NEXT;
    }
}