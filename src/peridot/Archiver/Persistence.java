/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.Archiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *  Utilities to manage importing and exporting of Objects
 * @author pentalpha
 */
public final class Persistence{
    private Persistence() {
        throw new AssertionError();
    }

    /**
     * Save an Object as a binary file
     * @param file  The file where the Object will be stored
     * @param data  The Java's Object to be stored in file
     * @throws IOException
     */
    public static void saveObjectAsBin(File file, 
                                   Object data)
            throws IOException
    {
        String absolutePath = file.getAbsolutePath();
        peridot.Archiver.Manager.makeNewFile(absolutePath);
        FileOutputStream fOutput = new FileOutputStream(absolutePath);
        ObjectOutputStream oOutput = new ObjectOutputStream(fOutput);
        //Log.info("starting to write " + absolutePath);
        oOutput.writeObject(data);
        fOutput.close();
        oOutput.close();
    }

    /**
     * Load an Object from a binary file
     * @param filePath  Full path to the file
     * @return  Object loaded from filePath
     */
    public static Object loadObjectFromBin(String filePath){
        Object data = null;
        try{
            FileInputStream fInput = new FileInputStream(filePath);
            ObjectInputStream oInput = new ObjectInputStream(fInput);
            data = oInput.readObject();
            fInput.close();
            oInput.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return data;
    }
}
