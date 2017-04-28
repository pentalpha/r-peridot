/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.Archiver;

import peridot.Log;
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
    public static File peridotDir = new File(getUserHomePath() + File.separator
            + ".r-peridot-files");
    public static File finalResultsDir = new File(getUserHomePath() + File.separator 
            + ".r-peridot-files" 
            + File.separator + "results");
    public static File scriptsDir = new File(getUserHomePath() + File.separator 
            + ".r-peridot-files" 
            + File.separator + "scripts");
    public static File rPortableDir = getRPortableFolder();
    public static String rnaSeqInputFileName = "rna-seq-input.tsv";
    public static File rnaSeqInputFile = new File(getUserHomePath() + File.separator 
            + ".r-peridot-files" 
            + File.separator + "results" 
            + File.separator + rnaSeqInputFileName);
    public static String conditionInputFileName = "condition-input.tsv";
    public static File conditionInputFile = new File(getUserHomePath() + File.separator 
            + ".r-peridot-files" 
            + File.separator + "results" 
            + File.separator + conditionInputFileName);
    public static String geneListInputFileName = "gene-list-input.txt";
    public static File geneListInputFile = new File(getUserHomePath() + File.separator 
            + ".r-peridot-files" 
            + File.separator + "results" 
            + File.separator + geneListInputFileName);

    public static File rExec = getRExec();
    public static File jarFolder = getJarFolder();
    public static File defaultModulesDir = getDefaultModulesDir();

    private Places(){
        throw new AssertionError();
    }
    
    public static String getUserHomePath(){
        return System.getProperty("user.home");
    }
    
    public static void createPeridotDir(){
        if(peridotDir.exists() == false){
            Log.logger.info("~/r-peridot-files not found, creating it.");
            peridotDir.mkdirs();
            peridotDir.mkdir();
            if(!peridotDir.exists()){
                Log.logger.severe("Error: Could not create " + peridotDir.getAbsolutePath());
            }
        }
        if(finalResultsDir.exists() == false){
            Log.logger.info("Results directory not found, creating it.");
            finalResultsDir.mkdirs();
            finalResultsDir.mkdir();
            if(!finalResultsDir.exists()){
                Log.logger.severe("Error: Could not create directory for final results it");
            }
        }
    }

    public static File getDefaultModulesDir(){
        File file = new File(Places.jarFolder.getAbsolutePath() +
                            File.separator + "r-peridot-scripts");
        if(file.exists()){
            return file;
        }else{
            Log.logger.warning("Warning: No DefaultModulesDir found, without it you can't " +
                    "reload the modules.");
            return null;
        }
    }
    
    public static void updateModulesDir(boolean overwrite){
        try{
            if(scriptsDir.exists() == false){
                scriptsDir.mkdir();
            }
            updateModulesFolder(overwrite);
        }
        catch(Exception ex){
            Log.logger.severe("Error: could not update modules folder.");
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private static void updateModulesFolder(boolean overwrite) throws Exception{
        File[] modules = defaultModulesDir.listFiles();

        for(int i = 0; i < modules.length; i++){
            if(modules[i].isDirectory()
                    && modules[i].getParentFile().getAbsolutePath().equals(
                            defaultModulesDir.getAbsolutePath()
            ))
            {
                File targetDir = new File(scriptsDir.getAbsolutePath()
                                    + File.separator + modules[i].getName());

                if(targetDir.exists()){
                    if(overwrite){
                        FileUtils.deleteDirectory(targetDir);
                        FileUtils.copyDirectory(modules[i], targetDir);
                        Log.logger.info("Updated " + modules[i].getName() + ".");
                    }
                }else{
                    FileUtils.copyDirectory(modules[i], targetDir);
                    Log.logger.info("Updated " + modules[i].getName() + ".");
                }
            }
        }

        File gitDir = new File(scriptsDir.getAbsolutePath() + File.separator + ".git");
        if(gitDir.exists()){
            FileUtils.deleteDirectory(gitDir);
        }
    }
    
    public static File getRPortableFolder(){
        File userRPortable = new File(getUserHomePath() + File.separator 
            + "r-portable");
        File jarFolder = getJarFolder();
        File localRPortable = new File(jarFolder.getAbsolutePath() + File.separator
                + "r-portable");
        
        if(localRPortable.exists()){
            Log.logger.info("Local r-portable folder found.");
            return localRPortable;
        }else if (userRPortable.exists()){
            Log.logger.warning("Warning: Local r-portable folder not found.");
            Log.logger.info("User r-portable folder found.");
            return userRPortable;
        }else{
            Log.logger.severe("Error: No r-portable found");
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
            Log.logger.severe("Error: Could not load jar location");
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
