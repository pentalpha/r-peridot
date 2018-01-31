package peridot.CLI;

import org.apache.commons.io.FileUtils;
import peridot.AnalysisParameters;
import peridot.Archiver.Manager;
import peridot.Archiver.Places;
import peridot.Global;
import peridot.Log;
import peridot.AnalysisData;
import peridot.script.AnalysisModule;
import peridot.script.PostAnalysisModule;
import peridot.script.RModule;
import peridot.script.Task;
import peridot.script.r.Interpreter;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;

public final class PeridotCmd {

    private PeridotCmd(){
        throw new AssertionError();
    }

    public static String loadingState = "not loaded";

    public static void changeLoadingState(String newState){
        loadingState = newState;
        System.out.println("["+newState+"]");
    }

    public static Task start(AnalysisFile analysisFile){
        return PeridotCmd.start(analysisFile.scriptsToExec,
                analysisFile.params, analysisFile.specificParams,
                analysisFile.expression);
    }

    public static Task start(Set<String> scriptsToExec, AnalysisParameters params,
                             Map<String, AnalysisParameters> specificParams,
                             AnalysisData expression){
        PeridotCmd.clean();
        RModule.removeScriptResults();

        expression.writeExpression();
        //expression.writeFinalConditions();

        Task task = new Task(scriptsToExec, params, specificParams, expression);
        task.start();
        return task;
    }

    public static void createNecessaryDirs(){
        //this should happen statically and before everything else!
        changeLoadingState("Preparing directories");
        Places.createPeridotDir();
        Places.updateModulesDir(false);
    }

    public static boolean loadModules(){
        changeLoadingState("Loading Modules");
        RModule.loadUserScripts();
        if(RModule.getAvailableModules().size() == 0){
            Log.logger.severe("Fatal Error: Modules could not be loaded. " +
                    "We recommend to reset the modules folder.");
            return false;
        }
        Log.logger.finest("Modules loaded");
        return true;
    }

    public static boolean loadInterpreters(){
        changeLoadingState("Loading R Environments");
        Interpreter.getAvailableInterpreters();
        Interpreter.loadDefaultInterpreter();
        if(Interpreter.isDefaultInterpreterDefined() == false){
            System.out.println("No R environment chosen yet. Choose one of the following:");
            if(PeridotCmd.setDefaultInterpreter() == false){
                System.out.println("No valid R environment chosen, exiting.");
                return false;
            }
        }
        System.out.println("Current R environment:\n\t"+Interpreter.defaultInterpreter.exe);
        return true;
    }

    public static void updateDefaultModules(){
        Places.updateModulesDir(true);
    }

    public static boolean resetAllModules(){
        boolean result = Global.deletePeridotFolder();
        if(result){
            return true;
        }else{
            Log.logger.severe("Could not delete " + Places.modulesDir);
            return false;
        }
    }

    public static void exportModule(String modName, String folderName){
        File folder = new File(folderName);
        if(folder.exists() == false || folder.isFile()){
            Log.logger.severe("Error: " + folderName + " is not a folder or does not exists.");
            return;
        }
        RModule s = RModule.availableModules.get(modName);
        if(s == null){
            Log.logger.severe("Error: " + modName + " is not an existent module.");
            return;
        }
        File file = new File(folderName + File.separator + s.name + "." + RModule.binExtension);
        if(file.exists()){
            file.delete();
        }
        try{
            System.out.println(file.getAbsolutePath());
            s.toBin(file);
        }catch (IOException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static void importModule(String filePath){
        File binFile = new File(filePath);
        if(binFile.isDirectory()){
            Log.logger.severe("Error: "+ filePath + " is not a file.");
            return;
        }
        if(binFile.exists()){
            Log.logger.severe("Error: "+ filePath + " already exists.");
            return;
        }
        Object bin = peridot.Archiver.Persistence.loadObjectFromBin(binFile.getAbsolutePath());
        if(bin == null){
            Log.logger.log(Level.SEVERE, "Could not load RModule binary. Maybe you don't have" +
                            " permission to read this file, or the file is corrupt.");
            return;
        }

        if(bin instanceof AnalysisModule || bin instanceof PostAnalysisModule){
            RModule script = null;
            if(bin instanceof AnalysisModule){
                script = (AnalysisModule)bin;
            }else{
                script = (PostAnalysisModule)bin;
            }
            script.createEnvironment(null);
            RModule.availableModules.put(script.name, script);
        }else{
            Log.logger.log(Level.SEVERE, "Could not load RModule binary. Unknown type.");
        }
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

    public static void clean(){
        for(Map.Entry<String, RModule> pair : RModule.availableModules.entrySet()){
            pair.getValue().cleanTempFiles();
        }
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

    public static boolean saveResultsAt(File file){
        //System.out.println("Trying to save results at " + file.getAbsolutePath());
        File saveFolder = file;
        if(saveFolder.isFile()){
            return false;
        }
        try {
            if (saveFolder.exists() == false) {
                //FileUtils.forceMkdirParent(saveFolder);
                FileUtils.forceMkdir(saveFolder);
            }else if (!Manager.isDirEmpty(saveFolder.toPath())){
                Log.logger.info(saveFolder.getPath()
                        + " is not empty, trying to save results in a different directory.");
                File newSaveFolder = Manager.getAlternativeFileName(saveFolder);
                return saveResultsAt(newSaveFolder);
            }
            FileUtils.copyDirectory(Places.finalResultsDir, saveFolder);
            System.out.println("Output directory is " + saveFolder.getAbsolutePath());
            return true;
        } catch (Exception ex) {
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            Log.logger.severe("Could not save the results to '" + saveFolder.getAbsolutePath() + "'." +
                    " The results are temporarily stored at " + Places.finalResultsDir.getAbsolutePath());
            return false;
        }
    }

    public static void listInterpreters(){
        System.out.println("Available R environments:");
        System.out.println(Interpreter.getInterpretersStr());
    }

    public static void addInterpreter(String env){
        if(Interpreter.addInterpreter(env)){
            System.out.println("'" + env + "' Added successfully!");
        }else{
            System.out.println("Invalid R environment, not adding it.");
        }
        //System.out.println("R environment addition not available yet");
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

    public static void updateInterpreter(){
        System.out.println("R package installation/updating not available yet.");
    }

    public static boolean setDefaultInterpreter(){
        System.out.println("Choose a R environment to be used by R-Peridot.");
        System.out.println(Interpreter.getInterpretersStr());
        int n = getInterpreterNumber();
        if(n < 1 || n > Interpreter.interpreters.size()){
            return false;
        }else{
            Interpreter.setDefault(n-1);
            return true;
        }
    }

    private static int getInterpreterNumber(){
        int n = -1;
        boolean repeat = true;
        while(repeat){
            Scanner keyboard = new Scanner(System.in);
            System.out.println("Number of the R environment: ");
            n = keyboard.nextInt();
            if(n < 0 || n > Interpreter.interpreters.size()){
                repeat = true;
                System.out.println("Invalid R environment number.");
            }else{
                repeat = false;
            }
        }
        return n;
    }
}
