/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author pentalpha
 */
public class Output {
    String buffer;
    public LocalDateTime lastUpdate;
    ConcurrentLinkedQueue<String> toAdd;
    
    public Output(){
        toAdd = new ConcurrentLinkedQueue<String>();
        buffer = "";
        storeTimeOfUpdate();
    }
    
    public void storeTimeOfUpdate(){
        lastUpdate = LocalDateTime.now();
    }
    
    public synchronized void appendLine(String text){
        toAdd.add(text+"\n");
        storeTimeOfUpdate();
        getFromQueue();
    }
    
    public synchronized void appendChar(char text){
        toAdd.add(text+"");
        storeTimeOfUpdate();
        getFromQueue();
    }
    
    public synchronized void setText(String text){
        buffer = "";
        toAdd.add(text);
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
            buffer += toAdd.poll();
        }
    }
}
