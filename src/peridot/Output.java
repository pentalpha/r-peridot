/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author pentalpha
 */
public class Output {
    String buffer;
    public LocalDateTime lastUpdate;
    ConcurrentLinkedQueue<String> toAdd;
    boolean printInConsole;
    //public AtomicInteger charsAdded;

    public Output(boolean printInConsole){
        make();
        this.printInConsole = printInConsole;
    }

    public Output(){
        make();
    }

    private void make(){
        toAdd = new ConcurrentLinkedQueue<String>();
        buffer = "";
        storeTimeOfUpdate();
        //charsAdded = new AtomicInteger(0);
        this.printInConsole = false;
    }
    
    public void storeTimeOfUpdate(){
        lastUpdate = LocalDateTime.now();
    }
    
    public synchronized void appendLine(String text){
        toAdd.add(text+System.lineSeparator());
        //charsAdded.addAndGet(text.length());
        storeTimeOfUpdate();
        getFromQueue();
    }
    
    public synchronized void appendChar(char text){
        toAdd.add(text+"");
        //charsAdded.incrementAndGet();
        storeTimeOfUpdate();
        getFromQueue();
    }
    
    public synchronized void setText(String text){
        buffer = "";
        toAdd.add(text);
        //charsAdded.set(text.length());
        storeTimeOfUpdate();
        getFromQueue();
    }
    
    public String getText(){
        while(!toAdd.isEmpty()){
            buffer += toAdd.poll();
        }
        return buffer;
    }
    
    public synchronized void getFromQueue(){
        while(!toAdd.isEmpty()){
            String str = toAdd.poll();
            buffer += str;
            if(printInConsole){
                System.out.print(str);
            }
        }
    }

    public HashMap<String, List<String>> getCommands(){
        //wait for sync
        while(!toAdd.isEmpty()){

        }

        HashMap<String, List<String>> commands;
        String[] infoLines = buffer.split(System.lineSeparator());

        String currentCommand = null;
        List<String> commandOutput = null;
        commands = new HashMap<>();
        for(int i = 0; i < infoLines.length; i++){
            if(infoLines[i].length() > 0){
                //String[] splice = Global.spliceBySpacesAndTabs(infoLines[i]);
                if(infoLines[i].charAt(0) == '>' || i == 0){
                    if(currentCommand != null){
                        commands.put(currentCommand, commandOutput);
                    }
                    currentCommand = infoLines[i];
                    commandOutput = new ArrayList<>();
                }else{
                    commandOutput.add(infoLines[i]);
                }
            }
        }

        return commands;
    }
}
