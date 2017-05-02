package peridot.CLI;

import peridot.AnalysisParameters;
import peridot.Archiver.Places;
import peridot.Global;
import peridot.Log;
import peridot.RNASeq;
import peridot.script.AnalysisScript;
import peridot.script.PostAnalysisScript;
import peridot.script.RScript;
import peridot.script.Task;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class PeridotCmd {

    private PeridotCmd(){
        throw new AssertionError();
    }


    public static Task start(AnalysisFile analysisFile){
        return PeridotCmd.start(analysisFile.scriptsToExec,
                analysisFile.params, analysisFile.specificParams,
                analysisFile.expression);
    }

    public static Task start(Set<String> scriptsToExec, AnalysisParameters params,
                             Map<String, AnalysisParameters> specificParams,
                             RNASeq expression){
        expression.writeExpression();
        expression.writeFinalConditions();

        Task task = new Task(scriptsToExec, params, specificParams, expression);
        task.start();
        return task;
    }

    public static boolean loadAll(){
        Places.createPeridotDir();
        Places.updateModulesDir(false);

        Log.logger.info("Starting to load modules...");
        RScript.loadUserScripts();
        if(RScript.getAvailableScripts().size() == 0){
            Log.logger.severe("Modules could not be loaded. We recommend using resetting the modules folder..");
            return false;
        }
        Log.logger.info("Modules loaded");
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
            Log.logger.severe("Could not delete " + Places.scriptsDir);
            return false;
        }
    }

    public static void exportModule(String modName, String folderName){
        File folder = new File(folderName);
        if(folder.exists() == false || folder.isFile()){
            Log.logger.severe("Error: " + folderName + " is not a folder or does not exists.");
            return;
        }
        RScript s = RScript.availableScripts.get(modName);
        if(s == null){
            Log.logger.severe("Error: " + modName + " is not an existent module.");
            return;
        }
        File file = new File(folderName + File.separator + s.name + "." + RScript.binExtension);
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
            Log.logger.log(Level.SEVERE, "Could not load RScript binary. Maybe you don't have" +
                            " permission to read this file, or the file is corrupt.");
            return;
        }

        if(bin instanceof AnalysisScript || bin instanceof PostAnalysisScript){
            RScript script = null;
            if(bin instanceof AnalysisScript){
                script = (AnalysisScript)bin;
            }else{
                script = (PostAnalysisScript)bin;
            }
            script.createEnvironment(null);
            RScript.availableScripts.put(script.name, script);
        }else{
            Log.logger.log(Level.SEVERE, "Could not load RScript binary. Unknown type.");
        }
    }

    public static void scriptDetails(String modName){
        RScript s = RScript.availableScripts.get(modName);
        if(s == null){
            Log.logger.severe(modName + " is not a valid module.");
            return;
        }
        File descriptionFile = s.getDescriptionFile();
        StringBuilder bString = peridot.Archiver.Manager.fileToString(descriptionFile);
        System.out.println("Module details:\n" + bString.toString().replace("[LINE-BREAK]", "\n"));
        System.out.println("Script file location: \n" + s.getScriptFile().getAbsolutePath());
    }

    public static void clean(){
        for(Map.Entry<String, RScript> pair : RScript.availableScripts.entrySet()){
            pair.getValue().cleanTempFiles();
        }
    }

    public static void listModules(){
        System.out.println("\n- Analysis Modules: ");
        for(String name : RScript.getAvailablePackages()){
            System.out.println("\t" + name);
        }
        System.out.println("\n- Post-Analysis Modules: ");
        for(String name : RScript.getAvailablePostAnalysisScripts()){
            System.out.println("\t" + name);
        }
    }
}
