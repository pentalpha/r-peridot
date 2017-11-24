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
                             AnalysisData expression){
        PeridotCmd.clean();
        RModule.removeScriptResults();

        expression.writeExpression();
        //expression.writeFinalConditions();

        Task task = new Task(scriptsToExec, params, specificParams, expression);
        task.start();
        return task;
    }

    public static boolean loadAll(){
        Places.createPeridotDir();
        Places.updateModulesDir(false);

        RModule.loadUserScripts();
        if(RModule.getAvailableScripts().size() == 0){
            Log.logger.severe("Fatal Error: Modules could not be loaded. " +
                    "We recommend reloading the modules folder.");
            return false;
        }
        Log.logger.finest("Modules loaded");
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
        RModule s = RModule.availableScripts.get(modName);
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
            RModule.availableScripts.put(script.name, script);
        }else{
            Log.logger.log(Level.SEVERE, "Could not load RModule binary. Unknown type.");
        }
    }

    public static void scriptDetails(String modName){
        RModule s = RModule.availableScripts.get(modName);
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
        for(Map.Entry<String, RModule> pair : RModule.availableScripts.entrySet()){
            pair.getValue().cleanTempFiles();
        }
    }

    public static void listModules(){
        System.out.println("\n- AnalysisData Modules: ");
        for(String name : RModule.getAvailablePackages()){
            System.out.println("\t" + name);
        }
        System.out.println("\n- Post-AnalysisData Modules: ");
        for(String name : RModule.getAvailablePostAnalysisScripts()){
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
}
