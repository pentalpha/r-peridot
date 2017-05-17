/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import peridot.Archiver.Places;
import peridot.Log;
import peridot.Output;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
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
    public Process process = null;
    public Integer exitStatus;

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
            try{
                this.exitStatus = new Integer(process.waitFor());
                if(this.exitStatus.intValue() != 0){
                    Log.logger.severe(script.name + " exit status: " + exitStatus.intValue());
                }
            }catch(java.lang.InterruptedException ex){
                output.appendLine("Process Interrupted");
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            }catch(java.lang.NullPointerException ex){
                output.appendLine("No process to monitor.");
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
            
            started.set(true);
            onEnd();  
        };
    }
    
    public void updateOutput(){
        if(process != null){
            InputStream iStream = process.getInputStream();
            InputStreamReader iStreamReader = new InputStreamReader(iStream);
            BufferedReader buffReader = new BufferedReader(iStreamReader);
            try{
                int c;
                while((c = buffReader.read()) != -1){
                    output.appendChar((char)c);
                }
                if(!process.isAlive()){
                    //Log.logger.info("Buffer ended: Process of " + script.name + " is dead.");
                }else if(!this.running.get()){
                    //Log.logger.info("Buffer ended: " + script.name + " not running anymore.");
                }else{
                    //Log.logger.info("Buffer ended for " + script.name);
                }
            }catch(IOException ex){
                output.appendLine("IOException in "+script.name+": ");
                output.appendLine(ex.getMessage());
                output.appendLine(ex.toString());
                Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }else{
            output.appendLine("No process started!");
        }

        if(running.get() == true){
            onEnd();
        }
    }

    public void cancel(){
        afterStart();
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
        String exitMessage = script.name + " finished.";
        Log.logger.info(exitMessage);

        savingFlag.set(true);
        output.appendLine("\n[End of input]");
        peridot.Archiver.Manager.stringToFile(Places.finalResultsDir
                + File.separator + script.name + ".output", output.getText());
        if(script.verifyResults()){
            successFlag.set(true);
            Log.logger.finer("Saving results of " + script.name);
            script.saveResults();
        }else{
            successFlag.set(false);
        }
        script.cleanLocalResults();
        savingFlag.set(false);
        
        started.set(true);
        running.set(false);
        
        task.addFinished(script.name, false);
        //System.out.println(script.name + " onEnd()");
    }
    
    public String getName(){
        return script.name;
    }
    
    public void abort(){
        //Log.logger.info("Trying to abort " + script.name);
        if(running.get() || process.isAlive()){
            //Log.logger.info("Aborting " + script.name);
            if (process != null) {
                process.destroyForcibly();
            }
            //Log.logger.info("Aborted " + script.name);
        }
        onEnd();
    }
    
    public boolean verifyResults(){
        return script.verifyResults();
    }
}
