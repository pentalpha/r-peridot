package peridot.script.r;

import org.apache.commons.lang3.SystemUtils;
import peridot.Log;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class Script {
    public String output;
    public File scriptFile;
    public AtomicBoolean running;
    String[] commandArray;
    ProcessBuilder processBuilder;
    Process process;
    AtomicBoolean stopListeningFlag;
    private String[] userArgs;
    public String outputFilePath;
    protected StringBuilder builder;

    public Script (File scriptFile){
        output = "";
        running = new AtomicBoolean(false);
        this.scriptFile = scriptFile;
        userArgs = new String[0];
        builder = new StringBuilder();
    }

    public Script (File scriptFile, String[] args, boolean autoPrintToBash){
        output = "";
        running = new AtomicBoolean(false);
        this.scriptFile = scriptFile;
        userArgs = args;
        builder = new StringBuilder();
    }

    public void run(Interpreter interpreter, boolean wait) throws Exception{
        defineCommand(interpreter);
        processBuilder = makeProcessBuilder(scriptFile.getAbsolutePath(), 
            commandArray, scriptFile.getParentFile(), !wait);
        if(processBuilder == null){
            Log.logger.severe("ProcessBuilder is null, not executing analysis.");
            throw new Interpreter.InvalidExeException();
        }
        process = processBuilder.start();
        if(process == null){
            throw new NullPointerException("Fatal Error: Failed to create "
                    + scriptFile.getName() + "'s process.");
        }else{
            afterStart(wait);
        }
    }

    private void update(){
        update(true);
    }

    private void readOutput(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null;
        try{
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
        }catch(IOException ex){
            Log.logger.severe(ex.getMessage());
        }
        output = builder.toString();
        afterEnd();
    }

    private void update(boolean wait){
        if(process != null){
            running.set(true);
            if(!wait){
                Log.logger.info("No waiting, launching read output thread.");
                new Thread(() -> {
                    readOutput();
                }).start();
            }else{
                Log.logger.info("Waiting for output from script.");
                readOutput();
            }
        }else{
            Log.logger.info("Null process, not reading output");
        }
    }

    private void afterStart(boolean wait){
        running.set(true);
        update(wait);
    }

    public String waitForOutput(){
        while(running.get()){
            //waiting
        }
        return output;
    }

    private void afterEnd(){
        if(running.get() == false){
            return;
        }
        running.set(false);
    }

    public String getOutputString(){
        if (output.length() == 0){
            return builder.toString();
        }else {
            return output;
        }
    }

    //public Output getOutputStream() {return output;}

    private String[] getCommandArray(String rPath){
        String[] c = {rPath,
                "--no-save",
                "--no-restore",
                "--quiet",
                "--file="+scriptFile.getAbsolutePath(),
                "--args"
        };

        String[] cmds = new String[c.length + userArgs.length];
        for(int i = 0; i < c.length;i++){
            cmds[i] = c[i];
        }

        for(int i = 0; i < userArgs.length; i++){
            cmds[i+c.length] = userArgs[i];
        }
        return cmds;
    }

    public void defineCommand(Interpreter interpreter){
        boolean needsEnv = false;
        String rPath = interpreter.exe;
        if(new File(rPath).exists()){
            if(SystemUtils.IS_OS_UNIX){
                needsEnv = true;
            }
        }

        commandArray = getCommandArray(rPath);

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

    public ProcessBuilder makeProcessBuilder(String scriptFile, String[] commandArray, File dir, boolean redirectOutputToFile){
        ProcessBuilder processBuilder = null;

        if(SystemUtils.IS_OS_WINDOWS){
            processBuilder = makeProcessBuilderWindows(commandArray);
        }else {
            processBuilder = makeProcessBuilderUnix(commandArray, dir);
        }
        processBuilder.redirectErrorStream(true);
        if(redirectOutputToFile){
            outputFilePath = getOutputFile(scriptFile);
            processBuilder.redirectOutput(new File(outputFilePath));
        }
        return processBuilder;
    }

    private static String getOutputFile(String scriptFile){
        return getOutputFile(scriptFile, 0);
    }

    private static String getOutputFile(String scriptFile, int i){
        String path = scriptFile + ".output." + i;
        if(new File(path).exists()){
            return getOutputFile(scriptFile, i+1);
        }else{
            return path;
        }
    }

    public void kill(){
        if(process != null){
            if(process.isAlive() && running.get()){
                process.destroy();
                if(process.isAlive()){
                    process.destroyForcibly();
                }
            }
        }
    }
}
