/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.Archiver;

import peridot.Log;
import peridot.script.RScript;
import org.apache.commons.lang3.*;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
/**
 *
 * @author pentalpha
 */
public final class Places {
    public static File sgsDir = new File(getUserHomePath() + File.separator 
            + ".sgs-remake-files");
    public static File finalResultsDir = new File(getUserHomePath() + File.separator 
            + ".sgs-remake-files" 
            + File.separator + "results");
    public static File scriptsDir = new File(getUserHomePath() + File.separator 
            + ".sgs-remake-files" 
            + File.separator + "scripts");
    public static File rPortableDir = getRPortableFolder();
    public static String rnaSeqInputFileName = "rna-seq-input.tsv";
    public static File rnaSeqInputFile = new File(getUserHomePath() + File.separator 
            + ".sgs-remake-files" 
            + File.separator + "results" 
            + File.separator + rnaSeqInputFileName);
    public static String conditionInputFileName = "condition-input.tsv";
    public static File conditionInputFile = new File(getUserHomePath() + File.separator 
            + ".sgs-remake-files" 
            + File.separator + "results" 
            + File.separator + conditionInputFileName);
    public static String geneListInputFileName = "gene-list-input.txt";
    public static File geneListInputFile = new File(getUserHomePath() + File.separator 
            + ".sgs-remake-files" 
            + File.separator + "results" 
            + File.separator + geneListInputFileName);
    public static File rExec = getRExec();
    public static File jarFolder = getJarFolder();
    private Places(){
        throw new AssertionError();
    }
    
    public static String getUserHomePath(){
        return System.getProperty("user.home");
    }
    
    public static void createSgsDir(){
        if(sgsDir.exists() == false){
            Log.logger.info("sgs-remake-files not found, creating it.");
            sgsDir.mkdirs();
            sgsDir.mkdir();
            if(!sgsDir.exists()){
                Log.logger.severe("Could not create it");
            }
        }
        if(finalResultsDir.exists() == false){
            Log.logger.info("Results dir not found, creating it.");
            finalResultsDir.mkdirs();
            finalResultsDir.mkdir();
            if(!finalResultsDir.exists()){
                Log.logger.severe("Could not create it");
            }
        }
    }
    
    public static void createScriptsDir(){
        try{
            if(scriptsDir.exists()){
                FileUtils.deleteDirectory(scriptsDir);
            }
            scriptsDir.mkdir();
            Log.logger.info("creating scripts folder");
            RScript.makeDefaultScriptsFolders();
        }
        catch(Exception ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    public static File getRPortableFolder(){
        File userRPortable = new File(getUserHomePath() + File.separator 
            + "r-portable");
        File jarFolder = getJarFolder();
        File localRPortable = new File(jarFolder.getAbsolutePath() + File.separator
                + "r-portable");
        
        if(localRPortable.exists()){
            Log.logger.info("Local r-portable foulder found.");
            return localRPortable;
        }else if (userRPortable.exists()){
            Log.logger.warning("Local r-portable foulder not found.");
            Log.logger.info("User r-portable foulder found.");
            return userRPortable;
        }else{
            Log.logger.severe("No r-portable found");
            return null;
        }
    }
    
    public static File getJarFolder(){
        File jarFolder = null;
        ProtectionDomain pDomain = Places.class.getProtectionDomain();
        CodeSource cSource = pDomain.getCodeSource();
        URL url = cSource.getLocation();
        URI uri = null;
        try{
            uri = url.toURI();
            jarFolder = new File(uri.getPath());
            if(jarFolder.isFile()){
                File parent = jarFolder.getParentFile();
                jarFolder = parent;
            }
            Log.logger.info("Jar folder: " + jarFolder.getAbsolutePath());
            return jarFolder;
        }catch(Exception ex){
            ex.printStackTrace();
            Log.logger.info("Could not load jar location");
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
        
    }
    
    public static File getRExec(){
        boolean is64Bits = false;
        if(SystemUtils.OS_ARCH.contains("64")){
            is64Bits = true;
        }
        String path = "";
        if(Places.rPortableDir != null){
            path = rPortableDir.getAbsolutePath();
            path += File.separator + "bin";
            if(SystemUtils.IS_OS_WINDOWS){
                if(is64Bits == true){
                    path += File.separator +"x64";
                }else{
                    path += File.separator + "i386";
                }
                path += File.separator + "R.exe";
            }else if(SystemUtils.IS_OS_LINUX){
               path += File.separator + "R";
            }
        }
        
        File exec = new File(path);
        if(exec.exists()){
            if(exec.isFile()){
                Log.logger.fine("R is " + exec.getAbsolutePath());
                if(SystemUtils.IS_OS_LINUX){
                    //setLinuxEnvironmentVars();
                }
                return exec;
            }
        }
        Log.logger.warning("R portable not found, using system PATH instead.");
        return null;
    }
    
    public static String[] getLinuxEnvForRPortable(){
        String[] strings = {"R_HOME='" + Places.rPortableDir.getAbsolutePath() + "'"};
        return strings;
    }
}
