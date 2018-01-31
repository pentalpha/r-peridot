package peridot.Archiver;

import peridot.Log;
import peridot.script.r.Interpreter;
import peridot.script.r.VersionNumber;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pentalpha on 24/01/2018.
 */
public class PeridotConfig implements Serializable{
    public String defaultInterpreter;
    public Set<String> availableInterpreters;
    public String packagesRepository;
    public static VersionNumber preferredRVersion = new VersionNumber("3.4.0");
    public static VersionNumber minimalRVersion = new VersionNumber("3.0.0");


    public PeridotConfig(){
        this.packagesRepository = "http://www.bioinformatics-brazil.org/r-peridot/R/drat/";
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
        this.availableInterpreters = new HashSet<>();
        for(Interpreter i : inters){
            this.availableInterpreters.add(i.exe);
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
