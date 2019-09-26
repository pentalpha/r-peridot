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
 * Logging of errors, warnings and other information.
 * This is the preferred method to sending text output,
 * the use of System.out directly is only recommended for the CLI classes.
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
            logger.setLevel(Level.INFO);
            //logger.setLevel(Level.WARNING);
            logger.finest("Init R-Peridot Log");
        }catch (Exception e) {  
            e.printStackTrace();  
        }
        return logger;
    }
}
