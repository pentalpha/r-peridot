package peridot.Archiver;

import peridot.script.r.Interpreter;
import peridot.script.r.VersionNumber;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pentalpha on 24/01/2018.
 */
public class PeridotConfig {
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
            return loadedConfig;
        }
    }
    public static PeridotConfig get(){
        return _instance;
    }

    public static void save() throws IOException{
        Persistence.saveObjectAsBin(Places.peridotConfigFile, _instance);
    }
}
