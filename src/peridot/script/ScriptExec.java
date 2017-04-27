/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import peridot.Log;
import peridot.Output;
/**
 *
 * @author pentalpha
 */
public class ScriptExec {
    public RScript script;
    public AtomicBoolean running;
    public AtomicBoolean started;
    public AtomicBoolean savingFlag;
    public AtomicBoolean successFlag;
    public Process process;
    
    private Thread scriptThread;
    private ScriptRunnable runnable;
    public Thread runningUpdater;
    Runnable isRunningRunnable;
    Task task;
    public Output output;
    
    //public ScriptProgressMonitorPanel monitor;
    public ScriptExec(RScript script, Output output, Task task){
        this.task = task;
        this.script = script;
        this.output = output;
        savingFlag = new AtomicBoolean();
        savingFlag.set(false);
        running = new AtomicBoolean();
        running.set(false);
        started = new AtomicBoolean();
        started.set(false);
        successFlag = new AtomicBoolean();
        started.set(false);
        process = null;
        
        runnable = new ScriptRunnable(this);
        scriptThread = new Thread(runnable);
        
        
        defineIsRunningRunnable();
        runningUpdater = new Thread(isRunningRunnable);
    }
    
    private void defineIsRunningRunnable(){
        isRunningRunnable = () -> {
            Integer exitStatus = new Integer(-999);
            try{
                exitStatus = new Integer(process.waitFor());
                Log.logger.warning("Process of " + script.name + " finished with exit status " + exitStatus.toString());
            }catch(java.lang.InterruptedException ex){
                output.appendLine("Process Interrupted");
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
            
            if(exitStatus != null){
                Log.logger.info(script.name + " finished: " + exitStatus.intValue());
            }
            
            started.set(true);
            onEnd();  
        };
    }
    
    public void updateOutput(){
        InputStream iStream = process.getInputStream();
        InputStreamReader iStreamReader = new InputStreamReader(iStream);
        BufferedReader buffReader = new BufferedReader(iStreamReader);
        try{
            int c;
            while((c = buffReader.read()) != -1 
                    && process.isAlive() 
                    && this.running.get()){
                output.appendChar((char)c);
            }
            if(!process.isAlive()){
                Log.logger.info("Process of " + script.name + " ended.");
            }else if(!this.running.get()){
                Log.logger.info(script.name + " not running anymore.");
            }else{
                Log.logger.info("Buffer ended for " + script.name);
            }
        }catch(IOException ex){
            output.appendLine("IOException in "+script.name+": ");
            output.appendLine(ex.getMessage());
            output.appendLine(ex.toString());
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        
        if(running.get() == true){
            onEnd();
        }
    }
    
    public void start(){
        scriptThread.start();
    }
    
    public void afterStart(){
        this.process = runnable.process;
        running.set(true);
        started.set(true);
        runningUpdater.start();
        new Thread(() ->{
            updateOutput();
        }).start();
    }
    
    public synchronized void onEnd(){
        if(!running.get()){
            return;
        }
        output.appendLine("end of input");
        Log.logger.info("Process of " + script.name + " finished.");
        savingFlag.set(true);
        if(script.verifyResults()){
            successFlag.set(true);
            Log.logger.info("Saving results of " + script.name);
            script.saveResults();
        }else{
            successFlag.set(false);
        }
        script.cleanLocalResults();
        savingFlag.set(false);
        
        started.set(true);
        running.set(false);
        
        task.addFinished(script.name, false);
    }
    
    public String getName(){
        return script.name;
    }
    
    public void abort(){
        Log.logger.info("trying to abort " + script.name);
        if(running.get() || process.isAlive()){
            process.destroy();
            process.destroyForcibly();
        }
        onEnd();
    }
    
    public boolean verifyResults(){
        return script.verifyResults();
    }
}
