package peridot.script;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import peridot.Archiver.Places;
import peridot.ConsensusThreshold;
import peridot.GeneIdType;
import peridot.Log;
import peridot.Organism;
import peridot.script.r.Interpreter;
import peridot.script.r.Package;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public abstract class AbstractModule {
    public static HashMap<String, RModule> availableModules;
    public static Map<String, Class> availableParamTypes = defineAvailableParamTypes();
    public static final String moduleFileName = "module.json";

    private static Map<String, Class> defineAvailableParamTypes(){
        Map<String, Class> params = new TreeMap<>();
        params.put(Integer.class.getSimpleName(), Integer.class);
        params.put(Float.class.getSimpleName(), Float.class);
        params.put(GeneIdType.class.getSimpleName(), GeneIdType.class);
        params.put(Organism.class.getSimpleName(), Organism.class);
        params.put(ConsensusThreshold.class.getSimpleName(), ConsensusThreshold.class);
        return params;
    }

    public static void removeScriptResults(){
        File dir = Places.finalResultsDir;
        if(dir.exists()){
            for(File file : FileUtils.listFilesAndDirs(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)){
                if(file.isDirectory() && (file.getAbsolutePath().equals(dir.getAbsolutePath()) == false)){
                    try{
                        FileUtils.deleteDirectory(file);
                        //file.delete();
                    }catch(IOException ex){
                        ex.printStackTrace();
                        Log.logger.info("Cant delete " + file.getName() + " results.");
                    }
                }else if(file.isFile() && file.getAbsolutePath().contains(".output")){
                    FileUtils.deleteQuietly(file);
                }
            }
        }
    }

    public static Vector<String> getAvailableAnalysisModules(){
        Vector<String> scripts = new Vector<>();
        for(Map.Entry<String, RModule> pair : RModule.availableModules.entrySet()){
            if(pair.getValue().getWorkingDirectory().getName().contains(".AnalysisModule")){
                scripts.add(pair.getKey());
                //Log.logger.info("fount result: " + pair.getKey());
            }
        }
        return scripts;
    }

    public static Vector<String> getAvailablePostAnalysisModules(){
        Vector<String> scripts = new Vector<>();
        for(Map.Entry<String, RModule> pair : RModule.availableModules.entrySet()){
            if(pair.getValue().getWorkingDirectory().getName().contains(".PostAnalysisModule")){
                scripts.add(pair.getKey());
                //Log.logger.info("fount result: " + pair.getKey());
            }
        }
        return scripts;
    }

    public static Vector<String> getAvailableModules(){
        Vector<String> scripts = new Vector<>();
        for(Map.Entry<String, RModule> pair : RModule.availableModules.entrySet()){
            //if(pair.getValue() instanceof PostAnalysisModule){
            //    scripts.add(pair.getKey());
            //}
            scripts.add(pair.getKey());
        }
        return scripts;
    }

    public static Set<String> modulesWithUnmetDependencies(){
        if(Interpreter.isDefaultInterpreterDefined()){
            return modulesWithUnmetDependencies(Interpreter.defaultInterpreter);
        }else{
            HashSet<String> set = new HashSet<String>();
            set.addAll(getAvailableModules());
            return set;
        }
    }

    public static Set<String> modulesWithUnmetDependencies(Interpreter interpreter){
        Set<String> cannotRun = new HashSet<>();
        for(String module : RModule.getAvailableModules()){
            if(!RModule.availableModules.get(module).requiredPackagesInstalled(interpreter)){
                cannotRun.add(module);
            }
        }
        return cannotRun;
    }

    public static void updateUserScripts(){
        loadUserScripts();
    }

    public static boolean deleteScript(String script){
        try{
            //new File(Places.sgsDir + File.separator + "log.txt").delete();
            FileUtils.deleteDirectory(RModule.availableModules.get(script).getWorkingDirectory());
            Log.logger.info("Deleting " + RModule.availableModules.get(script).getWorkingDirectory().getAbsolutePath());
        }catch(IOException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            return false;
        }

        boolean reallyDeleted = RModule.availableModules.get(script).getWorkingDirectory().exists() == false;
        if(!reallyDeleted){
            Log.logger.severe("Could not delete "
                    + RModule.availableModules.get(script).workingDirectory.getName());
        }

        return reallyDeleted;
    }

    //load all the scripts in the temp folder and sort them
    public static void loadUserScripts(){
        HashMap<String, RModule> loadedScripts = new HashMap<String, RModule>();
        Set<File> subFiles;
        Set<File> subDirs = new TreeSet<File>();
        //Log.logger.info("trying to get subfiles");
        subFiles = new TreeSet<File>(FileUtils.listFilesAndDirs(Places.modulesDir,
                TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        //Log.logger.info("iterating subfiles subfiles");
        for(File file : subFiles){
            String filePath = file.getAbsolutePath();
            if((filePath.equals(Places.modulesDir.getAbsolutePath()) == false)
                    && (file.getAbsolutePath().contains("results") == false)
                    && (file.getAbsolutePath().contains("results"+File.separator) == false)
                    && (file.isDirectory() == true))
            {
                subDirs.add(file);
            }
        }


        for(File file : subDirs){
            //Log.logger.info("iterate subdir " + file.getAbsolutePath());
            try{
                if(file.getName().contains(".PostAnalysisModule")){
                    PostAnalysisModule script = new PostAnalysisModule(file);
                    loadedScripts.put(script.name, script);
                }else if(file.getName().contains(".AnalysisModule")){
                    //Log.logger.info("creating " + file.getName());
                    AnalysisModule script = new AnalysisModule(file);
                    //Log.logger.info("created");
                    loadedScripts.put(script.name, script);
                }
            }catch(Exception ex){
                Log.logger.info("ERROR\nCould not import " + file.getName() + ", because: ");
                Log.logger.info(ex.getMessage());
                ex.printStackTrace();
            }
        }

        availableModules = loadedScripts;
        if(loadedScripts.size() <= 0){
            Log.logger.severe("Could not load any script from user folder.");
        }
        Collection<RModule> modules = availableModules.values();
        for (RModule module : modules){
            module.setChildren();
        }
        //sortAvailableScripts();
        //return loadedScripts;
    }

    public static Map<String, Class> getRequiredParametersFromModules(){
        Map<String, Class> params = new HashMap<>();
        for(RModule script : availableModules.values()){
            for(Map.Entry<String, Class> param : script.requiredParameters.entrySet()){
                params.put(param.getKey(), param.getValue());
            }
        }
        return params;
    }

    public static Set<Package> requiredPackages(){
        Set<Package> req = new HashSet<>();
        for(RModule module : RModule.availableModules.values()){
            req.addAll(module.requiredPackages);
        }
        return req;
    }
}
