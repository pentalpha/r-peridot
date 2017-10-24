/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import peridot.AnalysisData;
import peridot.AnalysisParameters;
import peridot.Archiver.Manager;
import peridot.Archiver.Places;
import peridot.Log;
import peridot.Output;
import sun.util.logging.PlatformLogger;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author pentalpha
 */
public class Task {
    
    public Set<String> queryModules;
    public Set<String> validQueryModules;
    public Set<String> modulesToExec;
    public AnalysisParameters params;
    public Map<String, AnalysisParameters> specificParams;
    public AnalysisData expression;
    
    public AtomicBoolean packagesFinishedFlag, abortAllFlag;
    //The actual status of the processing.
    //-2 = Not started, -1 = Processing, 0 = failed, 1 = some failed, 2 = all success
    public AtomicInteger processingStatus;
    public Runnable statusWatcher;
    public Runnable zombieFinder;
    
    //populate only on start and later:
    public ConcurrentLinkedDeque<String> successfulScripts;
    public ConcurrentLinkedDeque<String> noDiffExpFound;
    public ConcurrentLinkedDeque<String> failedResults;
    public ConcurrentLinkedDeque<String> failedScripts;
    public ConcurrentLinkedDeque<String> runningScripts;
    public ConcurrentLinkedDeque<String> finishedScripts;
    public ConcurrentHashMap<String, ScriptExec> scriptExecs;
    public ConcurrentHashMap<String, WaitState> waitState;
    public ConcurrentHashMap<String, Output> scriptOutputs;
    private int remainingAnalysisScripts;
    protected Thread scriptsStatusWatcher;
    //public static Task _instance;
    
    public Task(Set<String> scriptsToExec, AnalysisParameters params,
                            Map<String, AnalysisParameters> specificParams,
                            AnalysisData expression)
    {
        //_instance = this;
        this.queryModules = scriptsToExec;
        this.params = params;
        this.specificParams = specificParams;
        this.expression = expression;
        
        packagesFinishedFlag = new AtomicBoolean(false);
        processingStatus = new AtomicInteger(-2);
        this.modulesToExec = new TreeSet<>();
        this.validQueryModules = new TreeSet<>();
        successfulScripts = new ConcurrentLinkedDeque<>();
        noDiffExpFound = new ConcurrentLinkedDeque<>();
        failedScripts = new ConcurrentLinkedDeque<>();
        runningScripts = new ConcurrentLinkedDeque<>();
        finishedScripts = new ConcurrentLinkedDeque<>();
        scriptExecs = new ConcurrentHashMap<String, ScriptExec>();
        waitState = new ConcurrentHashMap<String, WaitState>();
        scriptOutputs = new ConcurrentHashMap<String, Output>();
        packagesFinishedFlag = new AtomicBoolean(false);
        defineStatusWatcher();
        defineZombieFinder();

        for(String name : scriptsToExec){
            if(evaluateScriptInput(name)){
                validQueryModules.add(name);
            }
        }

        for(String scriptName : validQueryModules){
            boolean canExecute = evaluateScriptForExecution(scriptName);
            if(canExecute){
                modulesToExec.add(scriptName);
            }
        }
    }
    
    public void start(){
        abortAllFlag = new AtomicBoolean();
        abortAllFlag.set(false);
        _instance = this;
        RScript.removeScriptResults();
        failedResults = new ConcurrentLinkedDeque<>();
        //ProcessingPanel.cleanMonitorPanels();
        remainingAnalysisScripts = 0;
        for(String name : modulesToExec){
            queueScriptForExecution(name);
        }
        updateStates();
        updateStatus();
        playReady();
        //scriptsStatusWatcher = new Thread(statusWatcher);
        //scriptsStatusWatcher.start();
        new Thread(zombieFinder).start();
    }

    private boolean evaluateScriptInput(String name){
        RScript script = RScript.availableScripts.get(name);
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

    private boolean evaluateScriptForExecution(String name){
        RScript script = RScript.availableScripts.get(name);
        HashSet<String> modulesNotFound = new HashSet<>();
        for(String module : script.requiredScripts){
            if(!validQueryModules.contains(module)){
                modulesNotFound.add(module);
            }
        }
        if(modulesNotFound.size() == 0){
            boolean createdConfig = script.createConfig();
            if(createdConfig){
                return true;
            }else{
                Log.logger.severe("An error occurred while creating config.txt, the parameters file, for " + name
                        + ". The module will not be executed.");
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

    private void queueScriptForExecution(String name){
        RScript script = RScript.availableScripts.get(name);
        if(script instanceof AnalysisScript){
            remainingAnalysisScripts++;
        }
        Output output = new Output();
        ScriptExec exec = new ScriptExec(script, output, this);
        scriptOutputs.put(name, output);
        scriptExecs.put(name, exec);

        waitState.put(name, WaitState.WAITING);
    }
    
    public boolean isNotStarted(){
        return processingStatus.get() == -2;
    }
    
    public boolean isProcessing(){
        return processingStatus.get() == -1;
    }
    
    public boolean isFailed(){
        return processingStatus.get() == 0;
    }
    
    public boolean isSomeFailed(){
        return processingStatus.get() == 1;
    }
    
    public boolean isSuccess(){
        return processingStatus.get() == 2;
    }
    
    private void defineStatusWatcher(){
        statusWatcher = () -> {
            while(processingStatus.get() < 0){
                //updateStatus();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            //updateStatus();
            //packagesFinishedFlag.set(true);
        };
    }
    
    private void defineZombieFinder(){
        zombieFinder = () -> {
            while(processingStatus.get() < 0){
                try{
                    Thread.sleep(3000);
                }catch (java.lang.InterruptedException ex){
                    break;
                }
                updateStatus();
                playReady();
                for(String s : runningScripts){
                    LocalDateTime lastUpdate = scriptOutputs.get(s).lastUpdate;
                    if(lastUpdate.isBefore(LocalDateTime.now().minusMinutes(2))){
                        scriptOutputs.get(s).appendLine("[PERIDOT WARNING] This process may not be responding.");
                        Log.logger.warning(s + " process may not be responding.");
                    }
                    //ScriptExec exec = scriptExecs.get(s);
                    //if(exec.)
                }
            }
        };
    }
    
    /**
     * 
     * @return  
     */
    private synchronized void updateStatus(){
        if(finishedScripts.size() == scriptExecs.size()){
            if(failedScripts.size() == 0){
                processingStatus.set(2);
            }else if(successfulScripts.size() > 0){
                processingStatus.set(1);
            }else{
                processingStatus.set(0);
            }
            _instance = null;
        }else{
            /*System.out.println("\n");
            for(String name : scriptsToExec){
                if(finishedScripts.contains(name) == false) {
                    System.out.println("Remaining: " + name);
                }
            }

            for(String name : runningScripts){
                System.out.println("Running: " + name);
            }*/
            processingStatus.set(-1);
        }
    }
    
    public synchronized void addFinished(String name, boolean prefailed){
        Log.logger.finer("Adding " + name + " to finished scripts.");
        if(RScript.getAvailablePackages().contains(name)){
            remainingAnalysisScripts--;
            Log.logger.fine(remainingAnalysisScripts + " analysis scripts remaining.");
            if(remainingAnalysisScripts <= 0){
                packagesFinishedFlag.set(true);
                Log.logger.fine("Finished all analysis scripts.");
            }
        }
        finishedScripts.add(name);
        checkForSuccess(name);
        if(prefailed == false){
            runningScripts.remove(name);
        }else{
            ScriptExec exec = scriptExecs.get(name);
            exec.started.set(true);
            exec.running.set(false);
            exec.process = null;
            exec.successFlag.set(false);
            scriptOutputs.get(name).appendLine("preFailed");
        }
        updateStatus();
        updateStates();
        playReady();
    }
    
    private void checkForSuccess(String name){
        if(scriptExecs.get(name).successFlag.get()){
            successfulScripts.add(name);
            if(RScript.getAvailablePackages().contains(name)){
                String path = Places.finalResultsDir.getAbsolutePath() + File.separator
                        + name + ".AnalysisScript" + File.separator + "res.tsv";
                if(Manager.fileExists(path)){
                    try{
                        if(Manager.countLines(path) <= 1){
                            noDiffExpFound.add(name);
                        }
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                }

            }
            Log.logger.info(name + " is successful");
        }else{
            Set<String> faileds = scriptExecs.get(name).script.getNotExistantResults();
            for(String result : faileds){
                //Log.logger.severe(result + " wont be generated anymore.");
                this.failedResults.add(result);
            }
            failedScripts.add(name);
            Log.logger.info(name + " is not successful");
        }
    }
    
    private synchronized void playReady(){
        boolean found;
        String foundName;
        while(true){
            found = false;
            foundName = "";
            for(Map.Entry<String, WaitState> pair : waitState.entrySet()){
                if(abortAllFlag.get()){
                    if(pair.getValue().equals(WaitState.WAITING)
                            || pair.getValue().equals(WaitState.WAITING)){
                        found = true;
                        foundName = pair.getKey();
                        break;
                    }
                }else{
                    if(pair.getValue().equals(WaitState.READY)){
                        found = true;
                        foundName = pair.getKey();
                        break;
                    }
                }
            }
            if(found){
                waitState.replace(foundName, WaitState.STARTED);
                if(abortAllFlag.get()) {
                    scriptExecs.get(foundName).cancel();
                }else{
                    scriptExecs.get(foundName).start();
                    Log.logger.info(foundName + " has started");
                }
                runningScripts.add(foundName);
            }else{
                break;
            }
        }
    }
    
    private void preFailed(String name){
        this.addFinished(name, true);
        //failedScripts.add(name);
        //ScriptExec  exec = scriptExecs.get(name);
        //exec.started.set(true);
        //exec.running.set(false);
        //exec.process = null;
        //scriptOutputs.get(name).appendLine("preFailed");
    }
    
    private synchronized void updateStates(){
        boolean foundPreFailed;
        do{
            foundPreFailed = false;
            Set<String> toPutPrefailed = new HashSet<>();
            for(Map.Entry<String, WaitState> pair : waitState.entrySet()){
                if(pair.getValue().equals(WaitState.WAITING)){
                    //ScriptExec exec = scriptExecs.get(pair.getKey());
                    if(scriptExecs.get(pair.getKey()).script.filesAreInRequirements(failedResults).size() > 0){
                        toPutPrefailed.add(pair.getKey());
                        foundPreFailed = true;
                        break;
                    }
                }
            }
            for(String name : toPutPrefailed){
                waitState.replace(name, WaitState.PRE_FAILED);
                Log.logger.severe(name + " has failed previously to execution");
                preFailed(name);
            }
        }while(foundPreFailed);
        
        Set<String> toPutReady = new HashSet<String>();
        for(Map.Entry<String, WaitState> pair : waitState.entrySet()){
            if(pair.getValue().equals(WaitState.WAITING)){
                ScriptExec exec = scriptExecs.get(pair.getKey());
                if(exec.script instanceof AnalysisScript)
                {
                    toPutReady.add(pair.getKey());
                }else if(this.packagesFinishedFlag.get()){
                    Log.logger.finer("Checking if " + pair.getKey() + " is ready...");
                    if(exec.script.requirementsSufficed()){
                        toPutReady.add(pair.getKey());
                    }else{
                        Log.logger.finer("...not ready;");
                    }
                }
            }
        }
        for(String name : toPutReady){
            waitState.replace(name, WaitState.READY);
            Log.logger.finer(name + " is now ready");
        }
    }

    public void abortAll(){
        abortAllFlag.set(true);
        for(ScriptExec exec : scriptExecs.values()){
            if(exec.running.get()){
                exec.abort();
            }
        }
    }

    public static Task getRunningTask(){
        return _instance;
    }

    private static Task _instance;

    public enum WaitState{
        WAITING, PRE_FAILED, READY, STARTED
    }
}
