package peridot.Archiver;

import org.json.JSONArray;
import peridot.Log;
import peridot.script.r.Interpreter;
import peridot.script.r.VersionNumber;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import org.json.JSONObject;

import java.util.*;

/**
 * Changing, saving and loading configurations like preferred R environment and packages repository
 * @author pentalpha
 */
public class PeridotConfig implements Serializable{
    public String defaultInterpreter;
    public Set<String> availableInterpreters;
    public String packagesRepository;
    public String lastInputDir;
    public String rPeridotWebSite = "http://www.bioinformatics-brazil.org/r-peridot/";
    public static VersionNumber preferredRVersion = new VersionNumber("3.4");
    public static VersionNumber minimalRVersion = new VersionNumber("3.3");

    public PeridotConfig(){
        setDefaults();
    }

    public PeridotConfig(boolean set_defaults){
        if(set_defaults){
            setDefaults();
        }
    }

    public void setDefaults(){
        this.packagesRepository = rPeridotWebSite + "R/drat/";
        this.availableInterpreters = Places.getDefaultRexecs();
        this.defaultInterpreter = null;
        this.lastInputDir = null;
    }

    public void updateValues(){
        if(Interpreter.isDefaultInterpreterDefined()){
            this.defaultInterpreter = Interpreter.defaultInterpreter.exe;
            //Log.logger.severe("Saving default R interpreter: " + defaultInterpreter);
        }else{
            //Log.logger.severe("Saving no default R interpreter");
            this.defaultInterpreter = null;
        }

        List<Interpreter> inters = Interpreter.interpreters;
        if(inters != null){
            this.availableInterpreters = new HashSet<>();
            for(Interpreter i : inters){
                this.availableInterpreters.add(i.exe);
            }
        }
    }

    private static PeridotConfig _instance = getConfigs();
    private static PeridotConfig getConfigs(){
        PeridotConfig loadedConfig = null;
        if(Places.peridotConfigFile.exists()){
            loadedConfig = parseJsonConfig(Places.peridotConfigFile.getAbsolutePath());
        }
        if(loadedConfig == null){
            return new PeridotConfig();
        }else{
            if(loadedConfig.defaultInterpreter == null){
                Log.logger.severe("loaded no default interpreter, returning new instance of PeridotConfig");
                return new PeridotConfig();
            }else{
                Log.logger.info("loaded default interpreter: " + loadedConfig.defaultInterpreter);
            }
            return loadedConfig;
        }
    }
    public static PeridotConfig get(){
        return _instance;
    }

    public static void save() throws IOException{
        _instance.updateValues();
        saveJsonConfig(Places.peridotConfigFile.getAbsolutePath(), _instance);
    }

    public static PeridotConfig parseJsonConfig(String path){
        File f = new File(path);
        String content = Manager.fileToString(f).toString();
        JSONObject json = new JSONObject(content);
        PeridotConfig config = new PeridotConfig(false);
        if (json.has("lastInputDir")){
            config.lastInputDir = json.getString("lastInputDir");
        }
        config.defaultInterpreter = json.getString("defaultInterpreter");
        config.packagesRepository = json.getString("packagesRepository");
        config.rPeridotWebSite = json.getString("rPeridotWebSite");
        config.preferredRVersion = new VersionNumber(json.getString("preferredRVersion"));
        config.minimalRVersion= new VersionNumber(json.getString("minimalRVersion"));

        Collection<Object> availableInterpretersList = json.getJSONArray("availableInterpreters").toList();
        config.availableInterpreters = new HashSet<>();
        for (Object inter : availableInterpretersList){
            config.availableInterpreters.add(inter.toString());
        }

        return config;
    }

    public static void saveJsonConfig(String path, PeridotConfig instance){
        JSONObject json = new JSONObject();
        if(instance.lastInputDir != null) {
            json.put("lastInputDir", instance.lastInputDir);
        }
        json.put("defaultInterpreter",instance.defaultInterpreter);
        json.put("packagesRepository",instance.packagesRepository);
        json.put("rPeridotWebSite",instance.rPeridotWebSite);

        json.put("preferredRVersion",instance.preferredRVersion.toString());
        json.put("minimalRVersion",instance.minimalRVersion.toString());

        JSONArray array = new JSONArray();
        for(String inter : instance.availableInterpreters){
            array.put(inter);
        }
        json.put("availableInterpreters", array);

        String toPrint = json.toString(4);
        Manager.stringToFile(path, toPrint);
    }
}
