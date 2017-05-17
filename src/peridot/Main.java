package peridot;

import peridot.CLI.PeridotCmd;
import peridot.CLI.UserInterface;
import peridot.script.Task;

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
            }
        });

        if(args.length == 0){
            UserInterface.printNoCommand();
        }else{
            boolean success = PeridotCmd.loadAll();
            if(success) {
                UserInterface ui = new peridot.CLI.UserInterface(args);
            }
        }
        finished.set(true);
    }
}
