/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import peridot.Archiver.Places;
import peridot.Log;

/**
 *
 * @author pentalpha
 */
public class ScriptRunnable implements Runnable {
    ScriptExec exec;
    boolean preFailed;
    boolean ready;
    Runtime runtime;     
    String[] commandArray;
    String commandString;
    boolean firstRun;
    ProcessBuilder processBuilder;
    public Process process;
    ScriptRunnable(ScriptExec exec){
        super();
        runtime = Runtime.getRuntime();
        this.exec = exec;
        preFailed = false;
        ready = false;
        detectPreviousRunn();
        defineCommand();
        makeProcessBuilder();
        process = null;
    }

    @Override
    public void run(){
        if(processBuilder == null){
            exec.output.appendLine("ProcessBuilder is null, not executing");
            return;
        }
        try{
            process = processBuilder.start();
            if(process == null){
                throw new NullPointerException("Null process returned");
            }
            exec.afterStart();
        }catch(NullPointerException ex){
            exec.output.appendLine("NullPointerException: ");
            exec.output.appendLine(ex.getMessage());
            exec.output.appendLine(ex.toString());
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }catch(IndexOutOfBoundsException ex){
            exec.output.appendLine("IndexOutOfBoundsException: ");
            exec.output.appendLine(ex.getMessage());
            exec.output.appendLine(ex.toString());
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }catch(SecurityException ex){
            exec.output.appendLine("Security xception: ");
            exec.output.appendLine(ex.getMessage());
            exec.output.appendLine(ex.toString());
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }catch(IOException ex){
            exec.output.appendLine("IO exception: ");
            exec.output.appendLine(ex.getMessage());
            exec.output.appendLine(ex.toString());
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void detectPreviousRunn(){
        firstRun = true;
        Log.logger.info("checking for previous execution:");
        Iterator<File> it = FileUtils.iterateFiles(exec.script.workingDirectory, null, false);
        while(it.hasNext()){
            if(it.next().getName().contains(".RData")){
                Log.logger.info("Detected .RData in " + exec.script.workingDirectory);
                firstRun = false;
                break;
            }else{
                Log.logger.info("Executing for the first time in " + exec.script.workingDirectory);
            }
        }
    }

    public void defineCommand(){
        boolean needsEnv = false;
        String rPath;
        if(Places.rExec != null){
            if(Places.rExec.exists()){
                rPath = Places.rExec.getAbsolutePath();
                if(SystemUtils.IS_OS_LINUX){
                    needsEnv = true;
                }
            }else{
                rPath = "R";
            }
        }else{
            rPath = "R";
        }
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
            "--file="+exec.script.scriptFile.getAbsolutePath(),
            "--args",
            exec.script.workingDirectory.getAbsolutePath(),
            Places.finalResultsDir.getAbsolutePath(),
            exec.script.resultsFolder.getAbsolutePath(),
            notFirstExec
        };
        commandArray = c;

        if(needsEnv){
            String[] envVars = Places.getLinuxEnvForRPortable();
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

        commandString = "";
        for(int i = 0; i < commandArray.length; i++){
            commandString += commandArray[i];
            if(i != commandArray.length -1){
                commandString += " ";
            }
        }
        Log.logger.info(commandString);
    }
    
    public boolean makeProcessBuilder(){
        processBuilder = null;

        if(SystemUtils.IS_OS_WINDOWS){
            processBuilder = new ProcessBuilder(commandArray);
            processBuilder.redirectErrorStream(true);
        }else {
            File executeScript = new File(exec.script.workingDirectory.getAbsolutePath() + File.separator + "run.sh");
            try {
                if(executeScript.exists()){
                    executeScript.delete();
                }
                executeScript.createNewFile();
                PrintWriter out = new PrintWriter(executeScript);
                out.println(commandString);
                out.close();
                TreeSet<PosixFilePermission> permsSet = new TreeSet<>();
                permsSet.add(PosixFilePermission.OWNER_EXECUTE);
                permsSet.add(PosixFilePermission.OWNER_READ);
                Files.setPosixFilePermissions(executeScript.toPath(), permsSet);
                //runtime.exec("chmod 755 " + executeScript.getAbsolutePath());
                processBuilder = new ProcessBuilder(executeScript.getAbsolutePath());
                processBuilder.redirectErrorStream(true);
                
                
            }catch(IOException ex){
                Log.logger.info("Error, could not create " + executeScript.getAbsolutePath());
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return processBuilder != null;
    }
}