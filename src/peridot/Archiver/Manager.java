/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.Archiver;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import peridot.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * Archiver.Manager is a collection of methods to manage files/folders and retrieve info about them.
 * @author pentalpha
 */
public final class Manager {
    private Manager(){
        throw new AssertionError();
    }

    /**
     * Build a String from the contents of a file
     * @param   file    Text file to be read
     * @return          The text read on the file
     * @throws  IOException
     */
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

    /**
     * Convert text file to String, without throwing exceptions.
     * @param   file    File to be read
     * @return          The contents of the file. Returns null if could not read anything.
     */
    public static StringBuilder fileToString(File file){
        try{
            return readFile(file);
        }catch(IOException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Checks for the existence of a directory.
     * @param path  Path of the directory
     * @return  True if exists and is a directory
     */
    public static boolean dirExists(String path){
        File f = new File(path);
        return (f.exists() && f.isDirectory());
    }

    /**
     * Checks for the existence of a file
     * @param path  Path to the file
     * @return  True if exists and is a file
     */
    public static boolean fileExists(String path){
        File f = new File(path);
        return (f.exists() && !f.isDirectory());
    }

    /***
     * Creates a new file, given a path
     * If did not existed, creates it alongside the parent directory
     * If existed previously, overwrites it
     * 
     * @param path Absolute path to the file
     * @return If a file with the same path already existed
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

    /**
     * Retrieves the subdirectories of a directory
     * @param dir The directory to be searched
     *
     * @return  Set of files that are subdirectories of dir
     */
    public static Set<File> getSubDirs(File dir){
        Set<File> subFiles;
        Set<File> subDirs = new TreeSet<File>();
        subFiles = new TreeSet<File>(FileUtils.listFilesAndDirs(Places.peridotDir,
                                     TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        for(File file : subFiles){
            String filePath = file.getAbsolutePath();
            if((filePath.equals(Places.peridotDir.getAbsolutePath()) == false)
                && (file.getAbsolutePath().contains("results") == false)
                && (file.getAbsolutePath().contains("results"+File.separator) == false)
                && (file.isDirectory() == true))
            {
                subDirs.add(file);
            }
        }
        return subDirs;
    }

    /**
     * Determines if a file is an .jpg, .png or .img image file
     * @param fileName  the full path to the file
     * @return  If it's an image file
     */
    public static boolean isImageFile(String fileName){
        return fileName.contains(".jpg") 
               || fileName.contains(".jpeg")
               || fileName.contains(".png")
               || fileName.contains(".img");
    }

    /**
     * Counts the number of lines in a text file
     * @param filename  The full path to the file
     * @return  The number of lines
     * @throws IOException
     */
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
