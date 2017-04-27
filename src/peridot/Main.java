package peridot;

import peridot.Archiver.Places;
import peridot.script.RScript;
import peridot.script.Task;

import javax.swing.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static void main(String[] args) {
	    // write your code here
        boolean success = loadAll();
        if(!success) {
            return;
        }

        Main.clean();
    }

    public static void start(Set<String> scriptsToExec, AnalysisParameters params,
                             Map<String, AnalysisParameters> specificParams,
                             RNASeq expression){

        task = new Task(scriptsToExec, params, specificParams, expression);
        task.start();
    }

    public static boolean loadAll(){
        Log.logger.info("Start MainGUI");
        Places.createSgsDir();
        if(Places.scriptsDir.exists() == false){
            Places.createScriptsDir();
        }
        //Util.getRPath();

        //Log.logger.info("trying to load scripts");
        RScript.loadUserScripts();
        if(RScript.getAvailableScripts().size() == 0){
            Log.logger.severe("Scripts could not be loaded. We recommend using Menu > Tools > Reset User Scripts.");
            return false;
        }
        //Log.logger.info("scripts loaded");
        return true;
    }

    public static void clean(){
        for(Map.Entry<String, RScript> pair : RScript.availableScripts.entrySet()){
            pair.getValue().cleanTempFiles();
        }
    }
}
