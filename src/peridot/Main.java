package peridot;

import peridot.Archiver.PeridotConfig;
import peridot.CLI.PeridotCmd;
import peridot.CLI.UserInterface;
import peridot.script.Task;
import peridot.script.r.InstallationBatch;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class Main {

    public static AtomicBoolean finished;
    public static void main(String[] args) {
        finished = new AtomicBoolean(false);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                Task task = Task.getRunningTask();
                if(task != null){
                    if(task.processingStatus.get() < 0){
                        System.out.println("\n[USER-INTERRUPT]\n");
                        task.abortAll();
                        try{
                            //System.out.println("Joining mainThread");
                            while(finished.get() == false){

                            }
                        }catch(Exception ex){
                            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                        }
                    }
                }
                if(InstallationBatch.lastInstallation != null){
                    InstallationBatch.lastInstallation.stop();
                    while(InstallationBatch.lastInstallation.isRunning()){

                    }
                }
            }
        });

        Operations.createNecessaryDirs();
        if(Operations.loadModules()){
            if(Operations.loadInterpreters(() -> PeridotCmd.setDefaultInterpreter())){
                if(args.length == 0){
                    UserInterface.printNoCommand();
                }else{
                    UserInterface ui = new peridot.CLI.UserInterface(args);
                }
            }
        }

        try {
            PeridotConfig.save();
        }catch (IOException ex){
            Log.logger.severe("Error while saving the current configurations");
            ex.printStackTrace();
        }
        finished.set(true);
    }
}
