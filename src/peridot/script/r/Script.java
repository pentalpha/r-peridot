package peridot.script.r;

import org.apache.commons.lang3.SystemUtils;
import peridot.Archiver.Places;
import peridot.Global;
import peridot.Log;
import peridot.Output;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class Script {
    private Output output;
    public File scriptFile;
    String[] commandArray;
    ProcessBuilder processBuilder;
    Process process;
    AtomicBoolean stopListeningFlag;

    public Script (File scriptFile){
        this.scriptFile = scriptFile;
    }

    public void run(Interpreter interpreter) throws Exception{
        defineCommand(interpreter);
        processBuilder = makeProcessBuilder(commandArray, scriptFile.getParentFile());
        if(processBuilder == null){
            Log.logger.severe("ProcessBuilder is null, not executing analysis.");
            throw new Interpreter.InvalidExeException();
        }
        process = processBuilder.start();
        if(process == null){
            throw new NullPointerException("Fatal Error: Failed to create "
                    + scriptFile.getName() + "'s process.");
        }
        afterStart();
    }

    private void afterStart(){

    }

    public String waitForOutput() throws Exception{
        return waitForOutput(-1);
    }

    public String waitForOutput(int secondsToWait){
        output = new Output();
        Thread stopperThread = new Thread(() ->{
            if(secondsToWait > 0){
                try {
                    Thread.sleep(secondsToWait * 1000);
                }catch (InterruptedException ex){
                    return;
                }
                if(stopListeningFlag != null){
                    stopListeningFlag.set(true);
                    output.appendLine("[SCRIPT_TIMEOUT]");
                }
            }
        });

        if(process != null){
            InputStream iStream = process.getInputStream();
            InputStreamReader iStreamReader = new InputStreamReader(iStream);
            BufferedReader buffReader = new BufferedReader(iStreamReader);
            try{
                stopListeningFlag = new AtomicBoolean();
                stopListeningFlag.set(false);
                stopperThread.run();
                int c;
                while((c = buffReader.read()) != -1 && stopListeningFlag.get() == false){
                    output.appendChar((char)c);
                }
            }catch(IOException ex){
                output.appendLine( "IOException in "+scriptFile.getName()+" instance. ");
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            }

            try{
                stopperThread.join();
            }catch (InterruptedException ex){

            }
        }
        return output.getText();
    }

    public String getOutput(){
        return output.getText();
    }

    public void defineCommand(Interpreter interpreter){
        boolean needsEnv = false;
        String rPath = interpreter.exe;
        if(new File(rPath).exists()){
            if(SystemUtils.IS_OS_LINUX){
                needsEnv = true;
            }
        }

        String[] c = {rPath,
                "--no-save",
                "--no-restore",
                "--quiet",
                "--file="+scriptFile.getAbsolutePath()
        };
        commandArray = c;

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

        Log.logger.fine(commandArrayToCommandString(commandArray));
    }

    public static String commandArrayToCommandString(String[] commandArray){
        String commandString = "";
        for(int i = 0; i < commandArray.length; i++){
            commandString += commandArray[i];
            if(i != commandArray.length -1){
                commandString += " ";
            }
        }
        return commandString;
    }

    public static ProcessBuilder makeProcessBuilderWindows(String[] commandArray){
        return new ProcessBuilder(commandArray);
    }

    public static ProcessBuilder makeProcessBuilderUnix(String[] commandArray, File dir){
        File executeScript = new File(dir.getAbsolutePath()
                + File.separator + "run.sh");
        try {
            if (executeScript.exists()) {
                executeScript.delete();
            }
            executeScript.createNewFile();
            PrintWriter out = new PrintWriter(executeScript);
            out.println(commandArrayToCommandString(commandArray));
            out.close();
            //Log.logger.info(executeScript.getAbsolutePath() + " has been created");
        }catch(IOException ex) {
            Log.logger.severe("Error, could not create " + executeScript.getAbsolutePath());
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }

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
        return new ProcessBuilder(bashCmdArray);
    }

    public static ProcessBuilder makeProcessBuilder(String[] commandArray, File dir){
        ProcessBuilder processBuilder = null;

        if(SystemUtils.IS_OS_WINDOWS){
            processBuilder = makeProcessBuilderWindows(commandArray);
        }else {
            processBuilder = makeProcessBuilderUnix(commandArray, dir);
        }
        processBuilder.redirectErrorStream(true);
        return processBuilder;
    }
}
