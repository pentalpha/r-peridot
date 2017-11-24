/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import peridot.Archiver.Places;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * Global general purpose functions.
 * @author pentalpha
 */
public final class Global {
    public enum RoundingMode {
        HALF_UP,
        HALF_DOWN,
        UP,
        DOWN
    }
    private Global(){
        throw new AssertionError();
    }

    /**
     * @param s String of text.
     * @return  True if there are only letters and digits on s.
     */
    public static boolean stringIsLettersAndDigits(String s){
        String text = s;
        for(int i = 0; i < text.length(); i++){
            if(Character.isLetterOrDigit(text.charAt(i)) == false){
                return false;
            }
        }
        return true;
    }

    /**
     * Open a file with it's default viewer on the actual system.
     * @param file The file to be opened.
     */
    public static void openFileWithSysApp(File file){
        String cmd = "";
        if(SystemUtils.IS_OS_WINDOWS){
            cmd = "CMD /C START \"\" \"" + file.getAbsolutePath() + "\"";
        }else if(SystemUtils.IS_OS_LINUX){
            cmd = "xdg-open " + file.getAbsolutePath();
        }
        try{
            Log.logger.info("Executing " + cmd);
            Runtime.getRuntime().exec(cmd);
        }catch (Exception ex){
            Log.logger.severe("Could not open " + file.getName() + " with external program.");
            Log.logger.severe("Failed to: " + cmd);
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Delete the whole r-peridot-files folder.
     * @return  True if the deletion was successful.
     */
    public static boolean deletePeridotFolder(){
        try{
            FileHandler f = null;
            for(Handler h : Log.logger.getHandlers()){
                if(h instanceof FileHandler){
                    f = (FileHandler)h;
                }
                Log.logger.removeHandler(h);
            }
            f.close();
            new File(Places.peridotDir + File.separator + "log.txt").delete();
            FileUtils.deleteDirectory(Places.peridotDir);
            return true;
        }catch(IOException ex){
            Log.logger.severe("Could not delete ~/r-peridot-files");
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Run's a Runnable after a certain time delay.
     * @param runnable  Runnable to be executed.
     * @param delay     Delay, in milliseconds.
     */
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

    /**
     * Replaces all spaces ' ' with '_' on a string
     * @param string    Text to be edited.
     * @return          The edited text.
     */
    public static String noSpaces(String string)
    {
        string = string.replaceAll(" ", "_");
        return string;
    }

    /**
     * Splits a line with tabulation or ',', the one who splits more.
     * @param line  Line to be Spliced.
     * @return      Spliced line.
     */
    public static String[] splitWithTabOrCur(String line){
        String[] row = line.split("\t");
        String[] row2 = line.split(",");
        if(row2.length > row.length){
            row = row2;
        }
        return row;
    }

    /**
     * @param word  Word to be verified.
     * @return      True if word is some kind of 'gene id' string.
     */
    public static boolean wordIsGeneID(String word){
        return (word.equals("gene-id")
            || word.equals("gene_id")
            || word.equals("gene")
            || word.equals("gene id")
            || word.equals("id"));
    }

    /**
     * @param word  Word to be verified.
     * @return      True if word is some kind of 'microarray' string.
     */
    public static boolean wordIsMicroArrayID(String word){
        return (word.toLowerCase().equals("micro array")
            || word.toLowerCase().equals("micro_array")
            || word.toLowerCase().equals("micro")
            || word.toLowerCase().equals("micro-array")
            || word.toLowerCase().equals("array")
            || word.toLowerCase().equals("id"));
    }

    /**
     * @param row   Cells of text to be analyzed.
     * @return      True if most of the cells is made of text.
     */
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

    /**
     * @param row   Cells of text to be analyzed.
     * @return      True if most of the cells is made of rational values.
     */
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

    /**
     * Changes a String from programming notation to a more "natural" form.
     * @param string    Text to be verified.
     * @return          'Naturally Writen' text.
     */
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

    /**
     * Prints an array of integers.
     */
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

    /**
     * Prints an array of Strings.
     */
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


    /**
     * Turns a string into an array of two strings where the
     * first is the first word of the string and the second
     * one is the rest of the original string
     * @param text String to be separated in two
     * @return  return[0] = first word, return[1] = the rest
     */
    public static String[] spliceBySpacesAndTabs(String text){
        String[] spliced = text.split(" ");
        int total = 0;
        for(int i = 0; i < spliced.length; i++){
            total += spliced[i].split("\t").length;
        }
        String[] spliceTotal = new String[total];
        int lastJ = 0;
        for(int i = 0; i < spliced.length; i++){
            String[] spliceTemp = spliced[i].split("\t");
            for(int j = 0; j < spliceTemp.length; j++){
                spliceTotal[lastJ+j] = spliceTemp[j];
            }
            lastJ = lastJ + spliceTemp.length;
        }
        return spliceTotal;
    }

    public static String[] firstWordAndTheRest(String text){
        String[] array = new String[2];
        String[] spliced = spliceBySpacesAndTabs(text);
        array[0] = spliced[0];
        if(spliced.length == 1){
            array[1] = null;
        }else{
            array[1] = "";
            for(int i = 1; i < spliced.length; i++)
            {
                array[1] += " " + spliced[i];
            }
        }
        return array;
    }

    public static int roundFloat(float x, RoundingMode roundingMode){
        if(roundingMode == RoundingMode.DOWN){
            return (int) Math.floor(x);
        }else if (roundingMode == RoundingMode.UP){
            return (int) Math.ceil(x);
        }else {
            int naturalPart = new Double(Math.floor(x)).intValue();
            float fraction = x - naturalPart;
            if (fraction < 0.5) {
                return naturalPart;
            }else if(fraction > 0.5){
                return naturalPart+1;
            }else{
                if (roundingMode == RoundingMode.HALF_DOWN) {
                    return naturalPart;
                }else{
                    return naturalPart+1;
                }
            }
        }
    }

    public static void printRoundingTable(){
        String str = "x\tUP\tDOWN\tHALF_UP\tHALF_DOWN\n";
        float[] val = {0.1f, 0.25f, 0.5f, 0.75f, 1.0f, 3.1f, 3.25f, 3.5f, 3.75f, 4.0f};
        for(int i = 0; i < val.length; i++){
            str += val[i] + "\t" + roundFloat(val[i], RoundingMode.UP) +
                    "\t" + roundFloat(val[i], RoundingMode.DOWN) +
                    "\t" + roundFloat(val[i], RoundingMode.HALF_UP) +
                    "\t" + roundFloat(val[i], RoundingMode.HALF_DOWN) + "\n";
        }
        System.out.println(str);
    }

    public static String listOfWordsToLine(List<String> words){
        String s = "";
        for(String str : words){
            s+=str + " ";
        }
        return s;
    }

}
