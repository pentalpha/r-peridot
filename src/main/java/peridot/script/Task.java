/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * TODO:
       Tests with datasets
       Delete useless code
 */
package peridot.script;

import peridot.AnalysisData;
import peridot.AnalysisParameters;
import peridot.Archiver.PeridotConfig;
import peridot.ConsensusThreshold;
//import peridot.Archiver.Manager;
//import peridot.Archiver.Places;
import peridot.Log;
import peridot.Archiver.Manager;
import peridot.Archiver.Places;
import peridot.script.r.Interpreter;
import peridot.script.r.Package;
import peridot.tree.*;

//import java.io.File;
//import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.io.File;
//import java.util.Collection;
import java.util.ArrayList;
import java.util.Collection;
//import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author pentalpha
 */
public class Task {

    public static Task getRunningTask(){
        return _instance;
    }

    private static Task _instance;
    
    
    //public Set<String> modulesToExec;
    public AnalysisParameters params;
    public Map<String, AnalysisParameters> specificParams;
    public AnalysisData expression;
    
    public AtomicBoolean packagesFinishedFlag, abortAllFlag;
    //The actual status of the processing.
    //-2 = Not started, -1 = Processing, 0 = failed, 1 = some failed, 2 = all success
    public enum Processing_Status{
        NOT_STARTED,
        PROCESSING,
        FAILED,
        SOME_FAILED,
        ALL_SUCCESS

        /*private final int valor;
        Processing_Status(int n){
            valor = n;
        }
        public int getValor(){
            return valor;
        }*/
    }

    public Processing_Status processingStatus;
    //public Runnable statusWatcher;
    //public Runnable zombieFinder;
    
    //populate only on start and later:
    //public ConcurrentLinkedDeque<String> successfulScripts;
    //ublic ConcurrentLinkedDeque<String> noDiffExpFound;
    //public ConcurrentLinkedDeque<String> failedResults;
    //public ConcurrentLinkedDeque<String> failedScripts;
    //public ConcurrentLinkedDeque<String> runningScripts;
    //public ConcurrentLinkedDeque<String> finishedScripts;
    //public ConcurrentHashMap<String, ScriptExec> scriptExecs;
    //public ConcurrentHashMap<String, WaitState> waitState;
    //public ConcurrentHashMap<String, Output> scriptOutputs;
    public ModuleWorker[] workers;
    public Thread[] threads;
    private PipelineGraph pipeline;
    //private int remainingAnalysisScripts;
    //protected Thread scriptsStatusWatcher;
    
    public Task(Set<String> scriptsToExec, AnalysisParameters params,
                            Map<String, AnalysisParameters> specificParams,
                            AnalysisData expression)
    {
        _instance = this;
        //Set<String> queryModules = scriptsToExec;
        this.params = params;
        this.specificParams = specificParams;
        this.expression = expression;
        PeridotConfig.get().lastInputDir = expression.expressionFile.getParentFile().getAbsolutePath();
        
        //packagesFinishedFlag = new AtomicBoolean(false);
        processingStatus = Processing_Status.NOT_STARTED;
        //this.modulesToExec = new TreeSet<>();
        //successfulScripts = new ConcurrentLinkedDeque<>();
        //noDiffExpFound = new ConcurrentLinkedDeque<>();
        //failedScripts = new ConcurrentLinkedDeque<>();
        //runningScripts = new ConcurrentLinkedDeque<>();
        //finishedScripts = new ConcurrentLinkedDeque<>();
        //scriptExecs = new ConcurrentHashMap<String, ScriptExec>();
        //waitState = new ConcurrentHashMap<String, WaitState>();
        //scriptOutputs = new ConcurrentHashMap<String, Output>();
        //packagesFinishedFlag = new AtomicBoolean(false);
        //defineStatusWatcher();
        //defineZombieFinder();

        
        Set<String> validQueryModules = new TreeSet<>();
        for(String name : scriptsToExec){
            if(evaluateScriptInput(name)){
                validQueryModules.add(name);
            }
        }

        ArrayList<RModule> modules = new ArrayList<>();
        for(String scriptName : validQueryModules){
            boolean canExecute = evaluateScriptForExecution(scriptName, validQueryModules);
            if(canExecute){
                //modulesToExec.add(scriptName);
                modules.add(RModule.availableModules.get(scriptName));
            }
        }

        ConsensusThreshold.updateNumberOfPackages(modules);

        //Collection<RModule> modules = RModule.availableModules.values();
        pipeline = new PipelineGraph();
        pipeline.addNodes(modules);

        int cpus = Runtime.getRuntime().availableProcessors();
        Log.logger.info(cpus + " workers");
        workers = new ModuleWorker[cpus];
        threads = new Thread[cpus];
        //Log.logger.info("Starting workers");
        for(int i = 0; i < cpus; i++){
            workers[i] = new ModuleWorker(pipeline, Interpreter.defaultInterpreter, 400);
        }
        
        //Log.logger.info("creating threads");
        for(int i = 0; i < cpus; i++){
            threads[i] = new Thread(workers[i]);
        }
    }
    
    public void start(){
        Log.logger.info("Starting to run analysis");
        RModule.removeScriptResults();
        _instance = this;
        for(int i = 0; i < threads.length; i++){
            threads[i].start();
        }
    }

    public void join(){
        try{
            for(int i = 0; i < threads.length; i++){
                threads[i].join();
            }
        }catch(InterruptedException ex){
            ex.printStackTrace();
        }
        this.updateStatus();
    }

    private boolean evaluateScriptInput(String name){
        RModule script = RModule.availableModules.get(name);
        if(script != null){
            script.passParameters(params);
            if(specificParams.containsKey(name)){
                script.passParameters(specificParams.get(name));
            }
            if(script.parametersSufficed()){
                return true;
            }else{
                String paramsList = "";
                for(String entry : script.requiredParameters.keySet()){
                    paramsList += entry + "\n";
                }
                Log.logger.severe(name + "'s parameters not sufficed. Required parameters are:\n " +
                        paramsList + "Read the module details for more information.");
            }
        }else{
            Log.logger.severe("No module named '" + name + "' is available.");
        }
        return false;
    }

    private boolean evaluateScriptForExecution(String name, Set<String> validQueryModules){
        RModule script = RModule.availableModules.get(name);
        HashSet<String> modulesNotFound = new HashSet<>();
        for(String module : script.requiredScripts){
            if(!validQueryModules.contains(module)){
                modulesNotFound.add(module);
            }
        }
        if(modulesNotFound.size() == 0 || (!script.needsAllDependencies && 
        (modulesNotFound.size() < script.requiredScripts.size())))
        {
            if(script.requiredPackagesInstalled()){
                boolean createdConfig = script.createConfig();
                if(createdConfig){
                    return true;
                }else{
                    Log.logger.severe("An error occurred while creating config.txt, the parameters file, for " + name
                            + ". The module will not be executed.");
                }
            }else{
                Set<Package> notInstalled = script.requiredPackagesNotInstalled();
                String notFoundList = "";
                for(Package entry : notInstalled){
                    notFoundList += entry.name + "\t" + entry.version + "\n";
                }
                Log.logger.severe(name + "'s requirements not sufficed." +
                        " The following required packages are not installed in the R environment:\n " +
                        notFoundList + "The module will not be executed. Please install the required packages in the R environment.");
            }
        }else{
            String notFoundList = "";
            for(String entry : modulesNotFound){
                notFoundList += entry + "\n";
            }
            Log.logger.severe(name + "'s requirements not sufficed." +
                    " The following required modules were not selected or have invalid input:\n " +
                    notFoundList + "The module will not be executed. Read the modules details for more information.");
        }
        return false;
    }

    public boolean isFinished(){
        return (isSuccess() || isSomeFailed() || isFailed());
    }

    public boolean isNotStarted(){
        return processingStatus == Processing_Status.NOT_STARTED;
    }
    
    public boolean isProcessing(){
        return processingStatus == Processing_Status.PROCESSING;
    }
    
    public boolean isFailed(){
        return processingStatus == Processing_Status.FAILED;
    }
    
    public boolean isSomeFailed(){
        return processingStatus == Processing_Status.SOME_FAILED;
    }
    
    public boolean isSuccess(){
        return processingStatus == Processing_Status.ALL_SUCCESS;
    }

    /**
     * Synchronized method to update the atomic status of the task.
     */
    public synchronized void updateStatus(){
        if(pipeline.number_of_nodes() == pipeline.number_of_finished_nodes()){
            if(pipeline.number_of_nodes_at_status(PipelineNode.Status.FAILED) == 0){
                processingStatus = Processing_Status.ALL_SUCCESS;
            }else if(pipeline.number_of_nodes_at_status(PipelineNode.Status.DONE) > 0){
                processingStatus = Processing_Status.SOME_FAILED;
            }else{
                processingStatus = Processing_Status.FAILED;
            }
            _instance = null;
        }else if (pipeline.number_of_nodes_at_status(PipelineNode.Status.RUNNING) > 0){
            processingStatus = Processing_Status.PROCESSING;
        }else{
            processingStatus = Processing_Status.NOT_STARTED;
        }
    }

    public void abortAll(){
        abortAllFlag.set(true);
        pipeline.abortAll();
    }

    public Collection<String> getModules(){
        ArrayList<String> list = new ArrayList<>();
        for(PipelineNode node : pipeline.getNodes()){
            list.add(node.getKey());
        }
        return list;
    }

    public Map<String, PipelineNode.Status> getModuleStatus(){
        HashMap<String, PipelineNode.Status> status = new HashMap<>();
        for(PipelineNode node : pipeline.getNodes()){
            status.put(node.getKey(), node.getStatus());
        }
        return status;
    }

    public Map<String, Set<String>> getScriptSets(){
        HashMap<String, Set<String>> sets = new HashMap<>();
        sets.put("Successful", new TreeSet<String>());
        sets.put("No Differential Expression Found", new TreeSet<String>());
        sets.put("Failed", new TreeSet<String>());
        for(PipelineNode node : pipeline.getNodes()){
            String name = node.getKey();
            if(node.getStatus() == PipelineNode.Status.DONE){
                sets.get("Successful").add(name);
                if(RModule.getAvailableAnalysisModules().contains(name)){
                    String path = Places.finalResultsDir.getAbsolutePath() + File.separator
                            + name + ".AnalysisModule" + File.separator + "res.tsv";
                    if(Manager.fileExists(path)){
                        try{
                            if(Manager.countLines(path) <= 1){
                                sets.get("No Differential Expression Found").add(name);
                            }
                        }catch(Exception ex){
                            ex.printStackTrace();
                        }
                    }
                }
                //Log.logger.info(name + " is successful");
            }else{
                sets.get("Failed").add(name);
                //Log.logger.info(name + " is not successful");
            }
        }
        return sets;
    }

    public PipelineGraph getPipeline(){
        return pipeline;
    }


}
