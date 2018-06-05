/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.Archiver;

import org.apache.commons.io.FileUtils;
import peridot.Log;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

//import org.apache.commons.lang3.SystemUtils;

/**
 * Archiver.Places holds file and directories paths.
 * It also search possible locations for R environments.
 * @author pentalpha
 */
public final class Places {
    /**
     * The master directory where all the user stuff is stored.
     * But its not the place where r-peridot is, that's the jarFolder.
     */
    public static File peridotDir = new File(getUserHomePath() + File.separator
            + ".r-peridot-files");
    public static File peridotConfigFile = new File(getUserHomePath() + File.separator
            + ".r-peridot-files"
            + File.separator + "peridot.config");
    /**
     * The place where all the results generated by the modules are saved after analysis.
     */
    public static File finalResultsDir = new File(getUserHomePath() + File.separator 
            + ".r-peridot-files" 
            + File.separator + "results");

    /**
     * The place where all the modules available to the user are stored, each on
     * it's own directory.
     */
    public static File modulesDir = new File(getUserHomePath() + File.separator
            + ".r-peridot-files" 
            + File.separator + "scripts");
    public static File readPackagesScript = new File(getUserHomePath() + File.separator
            + ".r-peridot-files"
            + File.separator + "scripts"
            + File.separator + "installedPackages.R");
    public static File installPackageScript = new File(getUserHomePath() + File.separator
            + ".r-peridot-files"
            + File.separator + "scripts"
            + File.separator + "installPackage.R");
    /**
     * The directory of r-portable, if there is a detectable r-portable on this system.
     */
    //public static File rPortableDir = getRPortableFolder();

    /**
     * The name of the file that stores the count reads for the modules to use.
     */
    public static String countReadsInputFileName = "rna-seq-input.tsv";

    /**
     * The file that stores the count reads for the modules to use.
     */
    public static File countReadsInputFile = new File(getUserHomePath() + File.separator
            + ".r-peridot-files" 
            + File.separator + "results" 
            + File.separator + countReadsInputFileName);

    /**
     * The name of the file that describes what is the condition of each sample.
     */
    public static String conditionInputFileName = "condition-input.tsv";

    /**
     * The file that describes what is the condition of each sample.
     */
    public static File conditionInputFile = new File(getUserHomePath() + File.separator 
            + ".r-peridot-files" 
            + File.separator + "results" 
            + File.separator + conditionInputFileName);

    /**
     * The directory in which r-peridot is located.
     */
    public static File jarFolder = getJarFolder();

    /**
     * The directory where the defaults module, that can be loaded to modulesDir, are stored.
     * If [jarFolder]/r-peridot-scripts is not present, its null.
     */
    public static File defaultModulesDir = getDefaultModulesDir();

    private Places(){
        throw new AssertionError();
    }

    /**
     *
     * @return The current user's home directory.
     */
    public static String getUserHomePath(){
        return System.getProperty("user.home");
    }

    /**
     * Creates peridotDir and finalResultsDir, if they do not yet.
     */
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

    /**
     * @return The directory where the default modules are stored. If does not exists, returns null.
     */
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

    /**
     * Update each of the modules in modulesDir, based on the modules in defaultModulesDir.
     * If modulesDir does not exist, creates it.
     * If failed to update anything, Logs the exception.
     * @param overwrite True to force the update of modules
     *                  if they already exist in modulesDir.
     */
    public static void updateModulesDir(boolean overwrite){
        try{
            if(modulesDir.exists() == false){
                modulesDir.mkdir();
                overwrite = true;
            }
            updateModulesFolder(overwrite);
        }
        catch(Exception ex){
            Log.logger.severe("Error: could not update modules folder.");
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * 1. Gets the subdirectories of defaultModulesDir;
     * 2. For each subdirectory (that is not the .git folder),
     * Verifies if the equivalent subdirectory already existed in modulesDir
     * and, if it did not, copies it to modulesDir, only overwriting the
     * module already on scripts dir if overwrite = true.
     *
     * @param overwrite If already loaded modules should be overwritten.
     */
    private static void updateModulesFolder(boolean overwrite) throws Exception{
        File[] subs = defaultModulesDir.listFiles();

        for(int i = 0; i < subs.length; i++){
            if(subs[i].getParentFile().getAbsolutePath().equals(defaultModulesDir.getAbsolutePath())
            && subs[i].getName().contains(".git") == false)
            {
                if(subs[i].isDirectory()){
                    File targetDir = new File(modulesDir.getAbsolutePath()
                            + File.separator + subs[i].getName());
                    if(targetDir.exists()){
                        if(overwrite){
                            FileUtils.deleteDirectory(targetDir);
                            FileUtils.copyDirectory(subs[i], targetDir);
                            Log.logger.info("Updated " + subs[i].getName() + ".");
                        }
                    }else{
                        FileUtils.copyDirectory(subs[i], targetDir);
                        Log.logger.fine("Updated " + subs[i].getName() + ".");
                    }
                }else if(subs[i].getName().contains(".R")){
                    File destFile = new File(modulesDir.getAbsolutePath()
                            + File.separator + subs[i].getName());
                    if(destFile.exists()){
                        if(overwrite){
                            FileUtils.forceDelete(destFile);
                            FileUtils.copyFile(subs[i], destFile, true);
                        }
                    }else{
                        FileUtils.copyFile(subs[i], destFile, true);
                    }


                }
            }
        }

        File gitDir = new File(modulesDir.getAbsolutePath() + File.separator + ".git");
        if(gitDir.exists()){
            FileUtils.deleteDirectory(gitDir);
        }
    }

    public static String[] getWin64And32ExecsFrom(String rWindowsDir){
        String[] exes = new String[2];
        String binDir = rWindowsDir + File.separator + "bin";
        exes[0] = binDir + File.separator +"x64" + File.separator + "R.exe";
        exes[1] = binDir + File.separator +"i386" + File.separator + "R.exe";

        return exes;
    }

    public static String[] defaultRportableDirs(){
        File userRPortable = new File(getUserHomePath() + File.separator
                + "r-portable");
        File jarFolder = getJarFolder();
        File localRPortable = new File(jarFolder.getAbsolutePath() + File.separator
                + "r-portable");
        String[] dirs = new String[2];
        dirs[0] = userRPortable.getAbsolutePath();
        dirs[1] = localRPortable.getAbsolutePath();
        return dirs;
    }

    public static Set<String> windowsRexes(){
        TreeSet<String> exes = new TreeSet<>();
        String[] defaultRportableDir = defaultRportableDirs();
        for(int i = 0; i < defaultRportableDir.length; i++){
            if(defaultRportableDir[i] != null){
                String[] win64and32execs = getWin64And32ExecsFrom(defaultRportableDir[i]);
                for(int j = 0; j < win64and32execs.length; j++){
                    if(win64and32execs[j] != null){
                        exes.add(win64and32execs[j]);
                    }
                }
            }
        }

        File programFilesRDir = new File("C:" + File.separator + "Program Files"
                                        + File.separator + "R");
        if(programFilesRDir.exists()){
            File[] subs = programFilesRDir.listFiles();
            for(int i = 0; i < subs.length; i++){
                if(subs[i].isDirectory()
                        && subs[i].getParentFile().getAbsolutePath().equals(programFilesRDir.getAbsolutePath())
                        && subs[i].getName().contains("R-3"))
                {
                    String[] win64and32execs = getWin64And32ExecsFrom(subs[i].getAbsolutePath());
                    for(int j = 0; j < win64and32execs.length; j++){
                        if(win64and32execs[i] != null){
                            exes.add(win64and32execs[j]);
                        }
                    }
                }
            }
        }

        return exes;
    }

    public static Set<String> linuxRexes(){
        TreeSet<String> exes = new TreeSet<>();
        //exes.add("/opt/r-peridot/r-portable/bin/R"); //Included on the default dirs
        String[] defaultRportableDir = defaultRportableDirs();
        for(int i = 0; i < defaultRportableDir.length; i++){
            if(defaultRportableDir[i] != null){
                exes.add(defaultRportableDir[i] + File.separator + "bin" + File.separator + "R");
            }
        }
        return exes;
    }

    public static Set<String> getDefaultRexecs(){
        HashSet<String> exes = new HashSet<>();
        if(org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS){
            exes.addAll(windowsRexes());
        }else if(org.apache.commons.lang3.SystemUtils.IS_OS_LINUX){
            exes.addAll(linuxRexes());
        }
        exes.add("R");
        return exes;
    }

    /**
     * @return  The place where the current .jar executable is located.
     *          If the .jar is embedded in an .exe, returns the parent directory.
     */
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
            return jarFolder;
        }catch(Exception ex){
            ex.printStackTrace();
            Log.logger.severe("Error: Could not load jar location");
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
        
    }

    /**
     * Determines where r-portable may be installed
     * Looks for a ./r-portable/ folder or for a ~/r-portable folder.
     * @return  The directory in which r-peridot is located.
     *          null if could not find it.
     */
    /*public static File getRPortableFolder(){
        File userRPortable = new File(getUserHomePath() + File.separator
                + "r-portable");
        File jarFolder = getJarFolder();
        File localRPortable = new File(jarFolder.getAbsolutePath() + File.separator
                + "r-portable");

        if(org.apache.commons.lang3.SystemUtils.IS_OS_LINUX){
            File file = new File("/opt/r-peridot/r-portable");
            if (file.exists()){
                return file;
            }
        }

        if(localRPortable.exists()){
            //Log.logger.info("Local r-portable folder found.");
            return localRPortable;
        }else if (userRPortable.exists()){
            //Log.logger.warning("Warning: Local r-portable folder not found.");
            //Log.logger.info("User r-portable folder found.");
            return userRPortable;
        }else{
            //Log.logger.severe("Error: No r-portable found");

            return null;
        }
    }*/

    /**
     * Looks for the R executable file in [rPortableDir]/bin,
     * choosing the right one based on the current processor architecture and OS.
     *
     * @return  The R executable file.
     *          null if could not find it.
     */
    /*public static File getRExec(){
        boolean is64Bits = false;
        if(org.apache.commons.lang3.SystemUtils.OS_ARCH.contains("64")){
            is64Bits = true;
        }
        String path1 = "";
        String path2 = "";
        if(Places.rPortableDir != null){
            path1 = rPortableDir.getAbsolutePath() + File.separator + "bin";
            if(org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS){
                path2 = rPortableDir.getAbsolutePath() + File.separator + "bin";
                if(is64Bits == true){
                    path1 += File.separator +"x64";
                    path2 += File.separator +"i386";
                }else{
                    path1 += File.separator + "i386";
                }
                path1 += File.separator + "R.exe";
                path2 += File.separator + "R.exe";
            }else if(org.apache.commons.lang3.SystemUtils.IS_OS_LINUX){
                path2 = rPortableDir.getAbsolutePath();
                path1 += File.separator + "R";
                path2 += File.separator + "R";
            }
        }
        
        File exec = new File(path1);
        File exec2 = new File(path2);
        if(exec.exists() == false || exec.isDirectory()){
            exec = exec2;
        }
        if(exec.exists()){
            if(exec.isFile()){
                //Log.logger.info("R is " + exec.getAbsolutePath());
                if(org.apache.commons.lang3.SystemUtils.IS_OS_LINUX){
                    //setLinuxEnvironmentVars();
                }
                return exec;
            }
        }
        Log.logger.warning("R portable not found, using system PATH instead.");
        return null;
    }*/

    /**
     * @return Array of Linux Environment Variables for R-Portable.
     */
    /*public static String[] getLinuxEnvForRPortable(){
        String[] strings = {"R_HOME='" + Places.rPortableDir.getAbsolutePath() + "'"};
        return strings;
    }*/
}
