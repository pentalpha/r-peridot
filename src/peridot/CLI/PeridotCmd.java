package peridot.CLI;

import peridot.Log;
import peridot.Operations;
import peridot.script.RModule;
import peridot.script.r.InstallationBatch;
import peridot.script.r.Interpreter;
import peridot.script.r.Package;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public final class PeridotCmd {

    private PeridotCmd(){
        throw new AssertionError();
    }

    public static void scriptDetails(String modName){
        RModule s = RModule.availableModules.get(modName);
        if(s == null){
            Log.logger.severe(modName + " is not a valid module.");
            return;
        }
        File descriptionFile = s.getDescriptionFile();
        StringBuilder bString = peridot.Archiver.Manager.fileToString(descriptionFile);
        System.out.println("Module details:\n" + bString.toString());
        System.out.println("Script file location: \n" + s.getScriptFile().getAbsolutePath());
    }

    public static void listModules(){
        System.out.println("\n- AnalysisData Modules: ");
        for(String name : RModule.getAvailableAnalysisModules()){
            System.out.println("\t" + name);
        }
        System.out.println("\n- Post-AnalysisData Modules: ");
        for(String name : RModule.getAvailablePostAnalysisModules()){
            System.out.println("\t" + name);
        }
    }


    public static void addInterpreter(String env){
        if(Interpreter.addInterpreter(env)){
            System.out.println("'" + env + "' Added successfully!");
        }else{
            System.out.println("Invalid R environment, not adding it.");
        }
        //System.out.println("R environment addition not available yet");
    }

    private static int getInterpreterNumber(){
        int n = -1;
        while(true){
            Scanner keyboard = new Scanner(System.in);
            System.out.println("Number of the R environment: ");
            n = keyboard.nextInt();
            if(n > 0 || n <= Interpreter.interpreters.size()){
                break;
            }else{
                System.out.println("Invalid R environment number.");
            }
        }
        return n;
    }

    public static void listInterpreters(){
        System.out.println("Available R environments:");
        System.out.println(Interpreter.getInterpretersStr());
    }

    public static void removeInterpreter(){
        System.out.println("Choose a R environment to be removed from R-Peridot.");
        System.out.println(Interpreter.getInterpretersStr());
        int n = getInterpreterNumber();
        if(Interpreter.removeInterpreter(n-1)){
            System.out.println("Successfully removed from R-Peridot.");
        }else{
            System.out.println("Invalid environment to remove from R-Peridot.");
        }
    }

    public static boolean setDefaultInterpreter(){
        if(Interpreter.interpreters.size() <= 0){
            Log.logger.severe("No R Environments could be found in this system." +
            " If you have installed an R environment, please use 'r-peridot r add <path-to-exe>'" +
            " to inform R-Peridot about the location of R.");
            return false;
        }
        System.out.println("Choose a R environment to be used by R-Peridot.");
        System.out.println(Interpreter.getInterpretersStr());
        int n = getInterpreterNumber();
        if(n < 1 || n > Interpreter.interpreters.size()){
            return false;
        }else{
            Interpreter.setDefault(n-1);
            if(Interpreter.defaultInterpreter.getPackagesToInstall().size() > 0){
                askToUpdateInterpreter(n);
            }
            return true;
        }
    }

    public static String getPackageListStr(Collection<Package> packs){
        String str = "";
        for(Package pack : packs){
            str += "\t\t"+pack.name+System.lineSeparator();
        }
        return str;
    }

    public static boolean updateInterpreter(int index){
        Interpreter interpreter = Interpreter.interpreters.get(index-1);
        System.out.println("Starting to update " + interpreter.exe);
        InstallationBatch batch = Operations.makeInstallationBatch(interpreter);
        batch.startInstallations();
        batch.waitToFinish();

        HashMap<String, Collection<Package>> packageLists = new HashMap<>();
        packageLists.put("Successful installations", batch.getSuccessful());
        packageLists.put("Already installed", batch.getAlreadyInstalled());
        packageLists.put("Failed", batch.getFailed());

        for(Map.Entry<String, Collection<Package>> list : packageLists.entrySet()){
            if(list.getValue().size() > 0){
                System.out.println(list.getKey() + ":");
                System.out.println(getPackageListStr(list.getValue()));
            }
        }

        return batch.getFailed().size() == 0;
    }

    public static void askToUpdateInterpreter(int i){
        System.out.println("The current R environment is missing some required packages.\n"
                + "Would you like to install them from our repository? [y/n]");
        Scanner keyboard = new Scanner(System.in);
        String str = keyboard.next();
        if(str.equals("y") || str.equals("yes") || str.equals("Y") || str.equals("Yes")){
            updateInterpreter(i);
        } else{
            System.out.println("Not updating environment.");
        }
    }

    public static boolean updateInterpreter(){
        System.out.println("Choose an environment to update:");
        System.out.println(Interpreter.getInterpretersStr());
        int n = getInterpreterNumber();
        if(n < 1 || n > Interpreter.interpreters.size()){
            return false;
        }else{
            updateInterpreter(n);
            return true;
        }
    }

    //public static void runDefaultR(){
    //    Operations.openR(Interpreter.defaultInterpreter);
    //}
}
