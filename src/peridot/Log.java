/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author pentalpha
 */
public final class Log {
    public static final Logger logger = getLogger();
    
    private Log(){
        throw new AssertionError();
    }
    
    private static Logger getLogger(){
        Logger logger = null;
        FileHandler fh;
        try {
            // This block configure the logger with handler and formatter
            File file = new File(System.getProperty("user.home") + File.separator 
                + ".r-peridot-files"
                + File.separator + "log.txt");
            file.getParentFile().mkdirs();
            logger = Logger.getLogger("R-Peridot Logger");
            //Places.getUserHomePath();
            fh = new FileHandler(file.getAbsolutePath(), true); 
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);
            logger.addHandler(fh);
            logger.setLevel(Level.ALL);
            //logger.setLevel(Level.WARNING);
            logger.info("Init R-Peridot Log");
        }catch (Exception e) {  
            e.printStackTrace();  
        }
        return logger;
    }
    
    /*public static void info(String text){
        assertInit();
        logger.info(text);
    }
    public static void config(String text){
        assertInit();
        logger.config(text);
    }
    public static void fine(String text){
        assertInit();
        logger.fine(text);
    }
    public static void warning(String text){
        assertInit();
        logger.warning(text);
    }
    public static void severe(String text){
        assertInit();
        logger.severe(text);
    }*/
    /*public static void tryToStoreLog(){
        if(logString == null){
            logString = new Vector<String>();
        }
        boolean sgsDirExists = false;
        if(Places.sgsDir != null){
            if(Places.sgsDir.exists()){
                sgsDirExists = true;
            }
        }
        if(sgsDirExists){
            if(logFile == null){
                logFile = new File(Places.getUserHomePath() + File.separator 
                + ".sgs-remake-files" 
                + File.separator + "log.txt");
            }
            if(logFile.exists() == false){
                try{
                    logFile.createNewFile();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            try{
                Files.write(logFile.toPath(), logString, StandardOpenOption.APPEND);
                logString.clear();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }*/
}
