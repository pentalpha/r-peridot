package peridot.script.r;

import org.apache.commons.lang3.SystemUtils;
import peridot.Log;
import peridot.Output;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class Script {
    public Output output;
    public File scriptFile;
    public AtomicBoolean running;
    String[] commandArray;
    ProcessBuilder processBuilder;
    Process process;
    AtomicBoolean stopListeningFlag;
    private String[] userArgs;

    public Script (File scriptFile){
        output = new Output();
        running = new AtomicBoolean(false);
        this.scriptFile = scriptFile;
        userArgs = new String[0];
    }

    public Script (File scriptFile, String[] args, boolean autoPrintToBash){
        output = new Output(autoPrintToBash);
        running = new AtomicBoolean(false);
        this.scriptFile = scriptFile;
        userArgs = args;
    }

    public void run(Interpreter interpreter, boolean wait) throws Exception{
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
        }else{
            afterStart(wait);
        }
    }

    private void update(){
        if(process != null){
            running.set(true);
            int lines = 0;
            InputStream iStream = process.getInputStream();
            InputStreamReader iStreamReader = new InputStreamReader(iStream);
            BufferedReader buffReader = new BufferedReader(iStreamReader);
            try{
                stopListeningFlag = new AtomicBoolean();
                stopListeningFlag.set(false);
                //stopperThread.run();
                String c;
                while((c = buffReader.readLine()) != null && stopListeningFlag.get() == false){
                    output.appendLine(c);
                    lines += 1;
                }
            }catch(IOException ex){
                output.appendLine( "IOException in "+scriptFile.getName()+" instance. ");
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
            afterEnd();
        }
    }

    private void afterStart(boolean wait){
        output = new Output();
        if(!wait){
            new Thread(() ->{
                update();
            }).start();
        }else{
            update();
        }
    }

    public Output waitForOutput(){
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
        return output.getText();
    }

    public Output getOutputStream() {return output;}

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
