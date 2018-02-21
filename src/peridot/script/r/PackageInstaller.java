package peridot.script.r;

import peridot.Archiver.PeridotConfig;
import peridot.Archiver.Places;
import peridot.Log;
import peridot.Output;

import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
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
        INSTALLED
    }

    private Interpreter interpreter;
    private Package pack;
    public Status status;
    private Script script;
    private AtomicBoolean running;

    public PackageInstaller(Interpreter interpreter, Package pack){
        this.interpreter = interpreter;
        this.pack = pack;
        status = Status.NOT_STARTED;
        String[] args = {pack.name, PeridotConfig.get().packagesRepository};
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
        if(status == Status.FAILED){
            return false;
        }
        try{
            status = Status.INSTALLING;
            running.set(true);
            script.run(interpreter, true);
            Output output = script.getOutputStream();
            parseOutput(output.getCommands());
            running.set(false);
            return true;
        }catch(Exception e){
            status = Status.FAILED;
            e.printStackTrace();
            return false;
        }
    }

    private void parseOutput(HashMap<String, List<String>> commands){
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
