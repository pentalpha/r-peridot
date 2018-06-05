package peridot;

import peridot.Archiver.PeridotConfig;
import peridot.CLI.PeridotCmd;
import peridot.CLI.UserInterface;
import peridot.script.RModule;
import peridot.script.Task;
import peridot.script.r.InstallationBatch;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import peridot.script.r.Package;

public class Main {

    public static AtomicBoolean finished;
    public static void main(String[] args) {
        finished = new AtomicBoolean(false);

        //Manages SIGINTs. Stops any installation or task.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                Task task = Task.getRunningTask();
                System.out.println("\n[USER-INTERRUPT]\n");
                //System.out.println("3 - Waiting for task to finish");
                if(task != null){
                    if(task.processingStatus.get() < 0){
                        //System.out.println("\n[USER-INTERRUPT]\n");
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
                //System.out.println("2 - Checking for ongoing installations");
                if(InstallationBatch.lastInstallation != null){
                    InstallationBatch.lastInstallation.stop();
                    //System.out.println("1 - Waiting for installations to finish");
                    long startedWaiting = System.currentTimeMillis();
                    while(InstallationBatch.lastInstallation.isRunning()){
                        try {
                            Thread.sleep(100);
                        }catch (InterruptedException ex){
                            //do nothing
                        }
                        if (System.currentTimeMillis() - (1000 * 5) >= startedWaiting) {
                            System.out.println("Waited 5 seconds for installations to finish, exiting anyway.");
                            break;
                        }
                    }
                }
                //System.out.println("0 - Finished shutdown hook");
            }
        });

        Operations.createNecessaryDirs();
        if(Operations.loadModules()){
            if(args.length == 0){
                UserInterface.printNoCommand();
            }else{
                UserInterface ui = new peridot.CLI.UserInterface(args);
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
