package peridot;

import org.apache.commons.io.FileUtils;
import peridot.Archiver.Manager;
import peridot.Archiver.Places;
import peridot.CLI.AnalysisFile;
import peridot.script.AnalysisModule;
import peridot.script.PostAnalysisModule;
import peridot.script.RModule;
import peridot.script.Task;
import peridot.script.r.InstallationBatch;
import peridot.script.r.Interpreter;
import peridot.script.r.Package;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Main operations for R-Peridot.
 * This class is supposed to be used by the CLI and the GUI,
 * as a common high-level interface for both.
 * Created by pentalpha on 20/02/2018.
 */
public class Operations {
    public static String loadingState = "not loaded";

    public static void changeLoadingState(String newState){
        loadingState = newState;
        System.out.println("["+newState+"]");
    }

    public static void prepare(AnalysisFile analysisFile) throws NumberFormatException
    {
        prepare(analysisFile.expression);
    }

    public static void prepare(AnalysisData expression) throws NumberFormatException
    {
        clean();
        RModule.removeScriptResults();
        expression.writeExpression(true);
    }

    public static Task start(AnalysisFile analysisFile)
    {
        return start(analysisFile.scriptsToExec,
                analysisFile.params, analysisFile.specificParams,
                analysisFile.expression);
    }

    public static Task start(Set<String> scriptsToExec, AnalysisParameters params,
                             Map<String, AnalysisParameters> specificParams,
                             AnalysisData expression)
    {
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

    public static boolean loadInterpreters(java.util.function.BooleanSupplier function){

        changeLoadingState("Loading R Environments");
        Interpreter.getAvailableInterpreters();
        Interpreter.loadDefaultInterpreter();
        if(Interpreter.isDefaultInterpreterDefined() == false){
            System.out.println("No R environment chosen yet. Choose one of the following:");
            if(function.getAsBoolean() == false){
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

    public static InstallationBatch makeInstallationBatch(Interpreter interpreter){
        Collection<Package> toInstall = interpreter.getPackagesToInstall();
        return new InstallationBatch(toInstall, interpreter);
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


    public static void clean(){
        for(Map.Entry<String, RModule> pair : RModule.availableModules.entrySet()){
            pair.getValue().cleanTempFiles();
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

    /*public static boolean openR(Interpreter interpreter){
        try{
            if(SystemUtils.IS_OS_WINDOWS){
                Runtime.getRuntime().exec("cmd.exe /c start " + interpreter.exe + " --no-save");
            }else if(SystemUtils.IS_OS_UNIX){
                Runtime.getRuntime().exec("xterm " + interpreter.exe + " --no-save");
            }

            return true;
        }catch (Exception ex){
            Log.logger.severe("Could not launch R environment: " + interpreter.exe);
            ex.printStackTrace();
            return false;
        }
    }*/
}
