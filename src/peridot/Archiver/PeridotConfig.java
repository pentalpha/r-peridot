package peridot.Archiver;

import peridot.Log;
import peridot.script.r.Interpreter;
import peridot.script.r.VersionNumber;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Changing, saving and loading configurations like preferred R environment and packages repository
 * @author pentalpha
 */
public class PeridotConfig implements Serializable{
    public String defaultInterpreter;
    public Set<String> availableInterpreters;
    public String packagesRepository;
    public String rPeridotWebSite = "http://www.bioinformatics-brazil.org/r-peridot/";
    public static VersionNumber preferredRVersion = new VersionNumber("3.4");
    public static VersionNumber minimalRVersion = new VersionNumber("3.4");

    public PeridotConfig(){
        this.packagesRepository = rPeridotWebSite + "R/drat/";
        this.availableInterpreters = Places.getDefaultRexecs();
        this.defaultInterpreter = null;
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
            loadedConfig =
                    (PeridotConfig) Persistence.loadObjectFromBin(
                            Places.peridotConfigFile.getAbsolutePath()
                    );
        }
        if(loadedConfig == null){
            return new PeridotConfig();
        }else{
            if(loadedConfig.defaultInterpreter == null){
                //Log.logger.severe("loaded no default interpreter");
            }else{
                //Log.logger.info("loaded default interpreter: " + loadedConfig.defaultInterpreter);
            }
            return loadedConfig;
        }
    }
    public static PeridotConfig get(){
        return _instance;
    }

    public static void save() throws IOException{
        _instance.updateValues();
        Persistence.saveObjectAsBin(Places.peridotConfigFile, _instance);
    }
}
