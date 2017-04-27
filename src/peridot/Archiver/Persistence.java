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
import peridot.script.internalScript.ScriptStreamer;

/**
 *
 * @author pentalpha
 */
public final class Persistence{
    private Persistence(){
        throw new AssertionError();
    }
    
    public static String exportResource(String resourceName, String outputFolder) throws Exception {
        InputStream stream = null;
        FileOutputStream resStreamOut = null;
        String jarFolder;
        try {
            stream = ScriptStreamer.getScriptStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if(stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            jarFolder = new File(Places.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath().replace('\\', '/');
            //Log.info("Puting in : " + jarFolder);

            String fileName = new String(outputFolder + File.separator + resourceName);
            File file = new File(outputFolder + File.separator + resourceName);
            file.createNewFile();
            resStreamOut = new FileOutputStream(file);
            //Log.info("Puting in : " + fileName);
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            stream.close();
            resStreamOut.close();
        }

        return outputFolder + File.separator + resourceName;
    }

    public static void saveObjectAsBin(File file, 
                                   Object data)
            throws IOException
    {
        String absolutePath = file.getAbsolutePath();
        Manager.makeNewFile(absolutePath);
        FileOutputStream fOutput = new FileOutputStream(absolutePath);
        ObjectOutputStream oOutput = new ObjectOutputStream(fOutput);
        //Log.info("starting to write " + absolutePath);
        oOutput.writeObject(data);
        fOutput.close();
        oOutput.close();
    }

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
