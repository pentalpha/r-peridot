package peridot.script.r;

import peridot.Log;

import java.util.Collection;
import java.util.Deque;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by pentalpha on 20/02/2018.
 */
public class InstallationBatch {
    public enum Status{
        WAITING,
        INSTALLING,
        STOPPED,
        FINISHED
    }

    public Deque<PackageInstaller> concluded;
    public Queue<PackageInstaller> installationQueue;
    public PackageInstaller currentInstallation;
    public AtomicBoolean stopFlag;
    public Status status;
    public AtomicBoolean running;
    private Interpreter installingTo;

    public static InstallationBatch lastInstallation = null;

    public InstallationBatch(Collection<Package> packsToInstall, Interpreter installTo) {
        status = Status.WAITING;
        installingTo = installTo;
        concluded = new ConcurrentLinkedDeque<>();
        currentInstallation = null;
        installationQueue = new ConcurrentLinkedQueue<>();
        stopFlag = new AtomicBoolean(false);
        running = new AtomicBoolean(false);
        for (Package pack : packsToInstall){
            addToQueue(new PackageInstaller(installTo,pack));
        }
    }

    public void addToQueue(PackageInstaller installer){
        installationQueue.add(installer);
    }

    public void installationThread(){
        running.set(true);
        //System.out.println("Starting installationThread");
        while(status == Status.INSTALLING){
           //System.out.println("Installing");
           if(stopFlag.get()){
               cancelInstallations();
               status = Status.STOPPED;
               //System.out.println("Stopflag has been set, breaking the installationThread");
               break;
           }else{
               if(!installationQueue.isEmpty()){
                   PackageInstaller installation = installationQueue.poll();
                   this.currentInstallation = installation;
                   Log.logger.info("Installing: " + installation.getPackageName());
                   installation.install();
                   currentInstallation = null;
                   concluded.add(installation);
                   int total = concluded.size() + installationQueue.size();
                   String totalStr = " (" + concluded.size() + "/" + total + ")";
                   if(installation.status == PackageInstaller.Status.INSTALLED){
                       Log.logger.info("Installed: " + installation.getPackageName() + totalStr);
                   }else if(installation.status == PackageInstaller.Status.ALREADY_INSTALLED){
                       Log.logger.info("Already installed: " + installation.getPackageName() + totalStr);
                   }else if(installation.status == PackageInstaller.Status.FAILED){
                       Log.logger.severe(installation.getOutputStr());
                       Log.logger.severe("Failed to install: " + installation.getPackageName() + totalStr);
                   }else if(installation.status == PackageInstaller.Status.NO_PERMISSION){
                       Log.logger.info("This installation probably failed because you do not have access to the" +
                               " packages library of the current R environment. Try using r-peridot as root next time.");
                   }
               }else{
                   status = Status.FINISHED;
                   //System.out.println("No more installations, breaking the installationThread");
                   break;
               }
           }
        }
        //System.out.println("Exited the installationThread loop");
        installingTo.analyseInterpreter();
        //Log.logger.info("exiting installationThread");
        running.set(false);
    }

    public void startInstallations(){
        if(lastInstallation != null){
            while(lastInstallation.isRunning()){
            }
        }

        lastInstallation = this;
        status = Status.INSTALLING;
        running.set(true);
        new Thread(() ->{
            installationThread();
        }).start();
    }

    private void cancelInstallations(){
        if(currentInstallation != null){
            currentInstallation.stop();
            concluded.add(currentInstallation);
            currentInstallation = null;
        }
        for(PackageInstaller installer : installationQueue){
            installer.stop();
            concluded.add(installer);
        }
    }

    public int nFinished(){
        return concluded.size();
    }

    public int nToInstall(){
        int res = installationQueue.size();
        if(currentInstallation != null){
            res += 1;
        }
        return res;
    }

    public void stop(){
        if(currentInstallation != null){
            currentInstallation.stop();
        }
        stopFlag.set(true);
    }

    public boolean isRunning(){
        return status == Status.INSTALLING;
    }

    public void waitToFinish() {
        while(running.get()){

        }
    }

    public Collection<Package> getAlreadyInstalled(){
        Vector<Package> success = new Vector<>();
        for(PackageInstaller installer : concluded){
            if(installer.status == PackageInstaller.Status.ALREADY_INSTALLED){
                success.add(installer.getPackage());
            }
        }

        return success;
    }

    public Collection<Package> getSuccessful(){
        Vector<Package> success = new Vector<>();
        for(PackageInstaller installer : concluded){
            if(installer.status == PackageInstaller.Status.INSTALLED){
                success.add(installer.getPackage());
            }
        }

        return success;
    }

    public Collection<Package> getFailed(){
        Vector<Package> failures = new Vector<>();
        for(PackageInstaller installer : concluded){
            if(installer.status == PackageInstaller.Status.FAILED){
                failures.add(installer.getPackage());
            }
        }

        return failures;
    }

    public Collection<Package> getNoPermission(){
        Vector<Package> noPerm = new Vector<>();
        for(PackageInstaller installer : concluded){
            if(installer.status == PackageInstaller.Status.NO_PERMISSION){
                noPerm.add(installer.getPackage());
            }
        }

        return noPerm;
    }
}
