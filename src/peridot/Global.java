/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import peridot.Archiver.Places;

/**
 *
 * @author pentalpha
 */
public final class Global {
    private Global(){
        throw new AssertionError();
    }
    public static boolean stringIsLettersAndDigits(String s){
        String text = s;
        for(int i = 0; i < text.length(); i++){
            if(Character.isLetterOrDigit(text.charAt(i)) == false){
                return false;
            }
        }
        return true;
    }
    public static void openFileWithSysApp(File file){
        String cmd = "";
        if(SystemUtils.IS_OS_WINDOWS){
            cmd = "CMD /C START " + file.getAbsolutePath();
        }else if(SystemUtils.IS_OS_LINUX){
            cmd = "xdg-open " + file.getAbsolutePath();
        }
        try{
            Runtime.getRuntime().exec(cmd);
        }catch (Exception ex){
            Log.logger.severe("Could not open " + file.getName() + " with external program.");
            Log.logger.severe("Failed to: " + cmd);
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    public static boolean deleteScriptsFolder(){
        try{
            FileHandler f = null;
            for(Handler h : Log.logger.getHandlers()){
                if(h instanceof FileHandler){
                    f = (FileHandler)h;
                }
                Log.logger.removeHandler(h);
            }
            f.close();
            new File(Places.sgsDir + File.separator + "log.txt").delete();
            FileUtils.deleteDirectory(Places.sgsDir);
            return true;
        }catch(IOException ex){
            Log.logger.severe("Could not delete sgs-remake-files");
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            return false;
        }
    }
    
    public static void setTimeout(Runnable runnable, int delay){
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            }
            catch (Exception e){
                System.err.println(e);
            }
        }).start();
        
    }
    
    public static String noSpaces(String string)
    {
        string = string.replaceAll(" ", "_");
        return string;
    }
    
    public static String[] splitWithTabOrCur(String line){
        String[] row = line.split("\t");
        String[] row2 = line.split(",");
        if(row2.length > row.length){
            row = row2;
        }
        return row;
    }
    
    public static boolean wordIsGeneID(String word){
        return (word.equals("gene-id")
            || word.equals("gene_id")
            || word.equals("gene")
            || word.equals("gene id")
            || word.equals("id"));
    }
    
    public static boolean wordIsMicroArrayID(String word){
        return (word.toLowerCase().equals("micro array")
            || word.toLowerCase().equals("micro_array")
            || word.toLowerCase().equals("micro")
            || word.toLowerCase().equals("micro-array")
            || word.toLowerCase().equals("array")
            || word.toLowerCase().equals("id"));
    }
    
    public static boolean lineIsWords(String[] row){
        int countWords = 0;
        for(int i = 0; i < row.length; i++){
            try{
                int value = Integer.parseInt(row[i]);
            }catch(Exception ex){
                try{
                    double value = Double.parseDouble(row[i]);
                }catch(Exception ex2){
                    countWords++;
                }
            }
        }
        return countWords >= 2;
    }
    
    public static boolean lineIsDoubles(String[] row){
        int countDoubles = 0;
        
        for(int i = 0; i < row.length; i++){
            try{
                double value = Double.parseDouble(row[i]);
                if(value != Math.ceil(value) || value != Math.floor(value)){
                    countDoubles++;
                }
            }catch(Exception ex){
                
            }
        }
        
        return countDoubles > 0;
    }
    
    
    public static String getNaturallyWritenString(String string){
        String res = "";
        for(int i = 0; i < string.length(); i++){
            char c = string.charAt(i);
            if(i == 0){
                if(Character.isLowerCase(c)){
                    c = Character.toUpperCase(c);
                }
            }else{
                if(Character.isUpperCase(c) || c == '-'){
                    res += " ";
                }
            }
            if(c != '-'){
                res += c;
            }
        }
        return res;
    }
    
    public static void printArray(int[] array){
        String s = "";
        for(int i = 0; i < array.length; i++){
            s+= "" + array[i];
            if(i != array.length-1){
                s+= ", ";
            }
        }
        Log.logger.info(s);
    }
    
    public static void printArray(String[] array){
        String s = "";
        for(int i = 0; i < array.length; i++){
            s+= array[i];
            if(i != array.length-1){
                s+= ", ";
            }
        }
        Log.logger.info(s);
    }

    
}
