package peridot.script.r;

import peridot.Archiver.PeridotConfig;
import peridot.Archiver.Places;
import peridot.CLI.PeridotCmd;
import peridot.Log;
import peridot.Global;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * R package installer for R environments
 * Created by pentalpha on 19/02/2018.
 */
public class PackageInstaller {

    public enum Status {
        NOT_STARTED,
        INSTALLING,
        ALREADY_INSTALLED,
        FAILED,
        NO_PERMISSION,
        INSTALLED
    }

    private Interpreter interpreter;
    private Package pack;
    public Status status;
    public Script script;
    private AtomicBoolean running;
    public boolean writeAccessError;

    public PackageInstaller(Interpreter interpreter, Package pack){
        this.interpreter = interpreter;
        this.pack = pack;
        status = Status.NOT_STARTED;
        String[] args = {pack.name, "0.0.0.0"};
        if(interpreter.getRVersion().into(PeridotConfig.preferredRVersion)){
            String[] args2 = {pack.name, PeridotConfig.get().packagesRepository};
            args = args2;
        }
        script = new Script(Places.installPackageScript, args, true);
        running = new AtomicBoolean(false);
    }

    public String getPackageName(){
        return pack.name;
    }

    public Package getPackage(){return pack;}

    public boolean finished(){
        return status != Status.NOT_STARTED && status != Status.INSTALLING;
    }

    public boolean install(){
        if(status == Status.FAILED || status == Status.NO_PERMISSION){
            return false;
        }
        try{
            status = Status.INSTALLING;
            running.set(true);
            script.run(interpreter, true);
            String output = script.getOutputString();
            parseOutput(output);
            running.set(false);
            return true;
        }catch(Exception e){
            status = Status.FAILED;
            e.printStackTrace();
            return false;
        }
    }

    private boolean lookForPermissionError(String output){
        if(output != null){
            return output.contains("not writable");
        }
        return false;
    }

    private void parseOutput(String output){
        writeAccessError = lookForPermissionError(output);
        HashMap<String, List<String>> commands = Global.commandsFromOutput(output);
        List<String> ifCommand = commands.get("> if(packIsInstalled(packageToInstall)){");
        if(ifCommand != null){
            String resultLine = null;
            for(String line : ifCommand){
                if(line.contains("[1]")){
                    resultLine = line;
                    break;
                }
            }
            if(resultLine.contains("Package already installed")){
                status = Status.ALREADY_INSTALLED;
            }else if(resultLine.contains("The package could not")){
                status = Status.FAILED;
                if(writeAccessError){
                    Log.logger.severe("This installation probably failed because you do not have access to the" +
                            " packages library of the current R environment. Try using r-peridot as root next time.");
                    status = Status.NO_PERMISSION;
                }
            }else if(resultLine.contains("Package successfully installed")){
                status = Status.INSTALLED;
            }else{
                reportInvalidOutput();
            }
        }else{
            reportInvalidOutput();
        }
    }

    public String getOutputStr(){
        return script.getOutputString();
    }

    private void reportInvalidOutput(){
        Log.logger.severe("Invalid output from installation script");
        status = Status.FAILED;
    }

    public void stop(){
        status = Status.FAILED;
        script.kill();
    }

}
