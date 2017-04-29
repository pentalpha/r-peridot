/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import peridot.AnalysisParameters;
import peridot.Log;
import peridot.Output;
import peridot.RNASeq;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author pentalpha
 */
public class Task {
    
    public Set<String> scriptsToExec; 
    public AnalysisParameters params;
    public Map<String, AnalysisParameters> specificParams;
    public RNASeq expression;
    
    public AtomicBoolean packagesFinishedFlag;
    //The actual status of the processing.
    //-2 = Not started, -1 = Processing, 0 = failed, 1 = some failed, 2 = all success
    public AtomicInteger processingStatus;
    public Runnable statusWatcher;
    public Runnable zombieFinder;
    
    //populate only on start:
    public ConcurrentLinkedDeque<String> successfulScripts;
    public ConcurrentLinkedDeque<String> failedResults;
    public ConcurrentLinkedDeque<String> failedScripts;
    public ConcurrentLinkedDeque<String> runningScripts;
    public ConcurrentLinkedDeque<String> finishedScripts;
    public ConcurrentHashMap<String, ScriptExec> scriptExecs;
    public ConcurrentHashMap<String, WaitState> waitState;
    public ConcurrentHashMap<String, Output> scriptOutputs;
    //private ConcurrentHashMap<ScriptExec, ScriptProgressMonitorPanel> scriptMonitor;
    private int remainingAnalysisScripts;
    protected Thread scriptsStatusWatcher;
    //public static Task _instance;
    
    public Task(Set<String> scriptsToExec, AnalysisParameters params,
                            Map<String, AnalysisParameters> specificParams,
                            RNASeq expression)
    {
        //_instance = this;
        this.scriptsToExec = scriptsToExec;
        this.params = params;
        this.specificParams = specificParams;
        this.expression = expression;
        
        packagesFinishedFlag = new AtomicBoolean(false);
        processingStatus = new AtomicInteger(-2);
        //MainGUI.goToProcessingPanel();
        successfulScripts = new ConcurrentLinkedDeque<>();
        failedScripts = new ConcurrentLinkedDeque<>();
        runningScripts = new ConcurrentLinkedDeque<>();
        finishedScripts = new ConcurrentLinkedDeque<>();
        scriptExecs = new ConcurrentHashMap<String, ScriptExec>();
        waitState = new ConcurrentHashMap<String, WaitState>();
        scriptOutputs = new ConcurrentHashMap<String, Output>();
        //scriptMonitor = new ConcurrentHashMap<>();
        packagesFinishedFlag = new AtomicBoolean(false);
        defineStatusWatcher();
        defineZombieFinder();
    }
    
    public void start(){
        RScript.removeScriptResults();
        failedResults = new ConcurrentLinkedDeque<>();
        //ProcessingPanel.cleanMonitorPanels();
        remainingAnalysisScripts = 0;
        for(String name : scriptsToExec){
            RScript script = RScript.availableScripts.get(name);
            if(script != null){
                script.passParameters(params);
                if(specificParams.containsKey(name)){
                    script.passParameters(specificParams.get(name));
                }
                boolean createdConfig = script.createConfig();
                if(createdConfig == false){
                    Log.logger.info("An error ocurred while creating the config.txt for " + name);
                }else{
                    if(script instanceof AnalysisScript){
                        remainingAnalysisScripts++;
                    }else{
                        Log.logger.info(name + " isnt an analysis script");
                    }
                    Output output = new Output();
                    ScriptExec exec = new ScriptExec(script, output, this);
                    scriptOutputs.put(name, output);
                    scriptExecs.put(name, exec);
                    
                    waitState.put(name, WaitState.WAITING);
                }
            }
        }
        updateStates();
        updateStatus();
        playReady();
        //scriptsStatusWatcher = new Thread(statusWatcher);
        //scriptsStatusWatcher.start();
        new Thread(zombieFinder).start();
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
    private void updateStatus(){
        if(finishedScripts.size() == scriptExecs.size()){
            if(failedScripts.size() == 0){
                processingStatus.set(2);
            }else if(successfulScripts.size() > 0){
                processingStatus.set(1);
            }else{
                processingStatus.set(0);
            }
        }else{
            processingStatus.set(-1);
        }
    }
    
    public synchronized void addFinished(String name, boolean prefailed){
        Log.logger.info("Adding " + name + " to finished scripts.");
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
            Log.logger.info(name + " is sucessful");
        }else{
            Set<String> faileds = scriptExecs.get(name).script.getNotExistantResults();
            for(String result : faileds){
                Log.logger.severe(result + " wont be generated anymore.");
                this.failedResults.add(result);
            }
            failedScripts.add(name);
            Log.logger.info(name + " is not sucessful");
        }
    }
    
    private void playReady(){
        Set<String> started = new HashSet<String>();
        for(Map.Entry<String, WaitState> pair : waitState.entrySet()){
            if(pair.getValue().equals(WaitState.READY)){
                scriptExecs.get(pair.getKey()).start();
                started.add(pair.getKey());
                runningScripts.add(pair.getKey());
            }
        }
        for(String name : started){
            waitState.replace(name, WaitState.STARTED);
            Log.logger.info(name + " has started");
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
                    Log.logger.info("Checking if " + pair.getKey() + " is ready...");
                    if(exec.script.requirementsSufficed()){
                        toPutReady.add(pair.getKey());
                    }else{
                        Log.logger.info("...not ready;");
                    }
                }
            }
        }
        for(String name : toPutReady){
            waitState.replace(name, WaitState.READY);
            Log.logger.info(name + " is now ready");
        }
    }
    
    public enum WaitState{
        WAITING, PRE_FAILED, READY, STARTED
    }
}
