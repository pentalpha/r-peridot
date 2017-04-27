/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.Archiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import peridot.Log;

/**
 *
 * @author pentalpha
 */
public final class Manager {
    private Manager(){
        throw new AssertionError();
    }
    
    private static StringBuilder readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader (file));
        String         line = null;
        StringBuilder  stringBuilder = new StringBuilder();
        String         ls = System.getProperty("line.separator");

        try {
            while((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            return stringBuilder;
        } finally {
            reader.close();
        }
    }
    
    public static StringBuilder fileToString(File file){
        try{
            return readFile(file);
        }catch(IOException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
    }
    
    public static boolean dirExists(String path){
        File f = new File(path);
        return (f.exists() && f.isDirectory());
    }

    public static boolean fileExists(String path){
        File f = new File(path);
        return (f.exists() && !f.isDirectory());
    }

    /***
     * Creates a new file, giver a name
     * 
     * @param path Absolute path to the file
     * @return If a file with the same name already existed
     */
    public static boolean makeNewFile(String path) throws IOException{
        //Log.info("trying to create " + path);
        File f = new File(path);
        boolean alreadyExisted = f.exists();
        if(alreadyExisted == false){
            f.getParentFile().mkdirs();
            f.createNewFile();
        }else{
            f.delete();
        }
        return alreadyExisted;
    }

    public static Set<File> getSubDirs(File dir){
        Set<File> subFiles;
        Set<File> subDirs = new TreeSet<File>();
        subFiles = new TreeSet<File>(FileUtils.listFilesAndDirs(Places.sgsDir,
                                     TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        for(File file : subFiles){
            String filePath = file.getAbsolutePath();
            if((filePath.equals(Places.sgsDir.getAbsolutePath()) == false)
                && (file.getAbsolutePath().contains("results") == false)
                && (file.getAbsolutePath().contains("results"+File.separator) == false)
                && (file.isDirectory() == true))
            {
                subDirs.add(file);
            }
        }
        return subDirs;
    }

    public static boolean isImageFile(String fileName){
        return fileName.contains(".jpg") 
               || fileName.contains(".jpeg")
               || fileName.contains(".png")
               || fileName.contains(".img");
    }
    
    public static int countLines(String filename) throws IOException {
        //Log.info("counting lines of " + filename);
        FileReader reader = new FileReader(filename);
        BufferedReader buffReader = new BufferedReader(reader);
        int count = 0;
        while(buffReader.readLine() != null){
            count++;
        }
        reader.close();
        buffReader.close();
        return count;
    }
}
