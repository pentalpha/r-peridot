/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.Archiver;

import peridot.Global;
import peridot.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *  Collection of utilities to handle spreadsheet-like files, .csv or .tsv.
 *  @author pentalpha
 */
public class Spreadsheet {
    /**
     * Types of values that can be found on the data: Integer or Rational
     */
    //public enum DataType{
      //  Int, Float
    //}

    /**
     * @param line  line from a .csv or .tsv file.
     * @param sep   Cell separator.
     * @return      If the line is made mostly of words, not numbers.
     */
    public static boolean lineIsHeader(String line, String sep){
        String[] row = Global.split(line, sep);
        String firstWord = row[0];
        if(Global.lineIsWords(row)){
            Log.logger.fine("The first line is made of words");
            return true;
        }
        
        return false;
    }

    private static void testParsing(String[] row, boolean rowNames) throws NumberFormatException{
        float x;
        for(int i = 0; i < row.length; i++){
            try{
                x = Float.parseFloat(row[i]);
            }catch(NumberFormatException ex){
                if(rowNames){
                    if(i != 0){
                        throw new NumberFormatException("Cannot parse value to float: " + row[i]);
                    }
                }else{
                    throw new NumberFormatException("Cannot parse value to float: " + row[i]);
                }
            }
        }
    }

    private static List<String[]> getRowsFromTable(File tableFile, int max, String separator,
                                                   boolean header, boolean rowNames, boolean testParsing)
            throws NumberFormatException
    {
        List<String[]> allRows = new LinkedList<>();
        int nLines = 0;
        try{
            FileReader inputReader = new FileReader(tableFile);
            BufferedReader buffInput = new BufferedReader(inputReader);
            String line = buffInput.readLine();
            while(line != null){
                String[] cells = Global.split(line, separator);
                try {
                    if(!header && nLines == 0){
                        if(testParsing){
                            testParsing(cells, rowNames);
                        }
                    }
                }catch (Exception ex){
                    if(nLines == 0){
                        Log.logger.info("Detect header in file not supposed to have a header: "
                                + tableFile.getAbsolutePath());
                    }else{
                        throw ex;
                    }
                }
                allRows.add(cells);
                nLines++;
                line = buffInput.readLine();
                if(max >= 0 && nLines > max){
                    break;
                }
            }
        }catch (Exception ex){
            Log.logger.severe("Could not read input from file " + tableFile.getAbsolutePath());
            ex.printStackTrace();
        }
        return allRows;
    }

    /**
     * Assuming that the table has no first cell (line 0, column 0), corrects the first line.
     * @param allRows   Reference to a List of String[] with all the cells from a spreadsheet file.
     *                  It will be altered by the method.
     */
    public static void correctNoFirstCell(List<String[]> allRows){
        if(allRows.size() <= 1){
            return;
        }
        String[] line1 = allRows.get(0);
        String[] line2 = allRows.get(1);
        if(line1.length < line2.length){
            int diff = line2.length - line1.length;
            String[] newLine1 = new String[line2.length];
            for(int i = 0; i < diff; i++){
                newLine1[i] = "";
            }
            for(int i = diff; i < newLine1.length; i++){
                newLine1[i] = line1[i-diff];
            }
            allRows.set(0, newLine1);
        }
    }

    /**
     * @param x Number of elements on the new header
     * @return  A header in the "ColumnX" form.
     */
    public static String[] getDefaultHeader(int x){
        String[] names = new String[x];
        for(int i = 0; i < x; i++){
            names[i] = "Column" + i;
        }
        return names;
    }

    /**
     *
     * @param file  Spreadsheet file.
     * @param sep   Cell separator.
     * @return      Only the first row of the spreadsheet.
     */
    public static String[] getFirstRowFromFile(File file, String sep){
        String[] line = null;
        try{
            FileReader reader = new FileReader(file);
            BufferedReader buffReader = new BufferedReader(reader);
            String firstLine = buffReader.readLine();
            line = Global.split(firstLine,sep);
            buffReader.close();
            reader.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return line;
    }

    /**
     * @param line  String of text, not spliced.
     * @param sep   Cell separator.
     * @return      True if line contains the names of the samples.
     */
    public static boolean lineIsSampleNames(String line, String sep){
        String[] row  = Global.split(line, sep);
        return lineIsSampleNames(row);
    }

    /**
     * @param row  Words in a line of text.
     * @return     True if row contains the names of the samples.
     *              Tries to determine this by looking for
     *              "gene-id"-like identifiers on the first name,
     *              or by parsing the values to Double.
     */
    public static boolean lineIsSampleNames(String[] row){
        if(row[0].equals("gene-id")
            || row[0].equals("gene_id")
            || row[0].equals("gene")
            || row[0].equals("gene id")
            || row[0].equals("id"))
        {
            return true;
        }else{
            return rowIsNames(row);
        }
    }

    /**
     * @param row   Words in a line of text.
     * @return      True if row contains the names of the samples.
     *              Tries to determine this by parsing the values to Double.
     */
    public static boolean rowIsNames(String[] row){
        for(int i = 1; i < row.length; i++){
            try{
                double value = Double.parseDouble(row[i]);
            }catch(Exception ex){
                return true;
            }
        }
        return false;
    }

    /**
     * @param n Number of elements on the new header
     * @return  A header in the "Column X" form.
     */
    public static String[] getDefaultColumnNames(int n){
        String[] defaultNames = new String[n];
        for(int i = 0; i < defaultNames.length; i++){
            defaultNames[i] = "Colunn " + (i+1);
        }
        return defaultNames;
    }

    /////////////////////////////////////////////////////////////////
            ///////// INSTANCE FIELDS & METHODS //////////
    /////////////////////////////////////////////////////////////////

    private Info info;
    private List<String[]> rows;
    public File tableFile;
    private boolean reloadFlag = true;
    private int lastMax = -2;
    private boolean testParsing;

    public Spreadsheet(File tableFile, boolean testParsing) throws IOException{
        Log.logger.info("Reading spreadsheet for " + tableFile.getName());
        this.info = new Info(tableFile);
        this.tableFile = tableFile;
        Log.logger.info("Read spreadsheet for " + tableFile.getName());
        this.testParsing = testParsing;
    }

    public Spreadsheet(File tableFile, Info info, boolean testParsing){
        this.info = info;
        this.tableFile = tableFile;
        this.testParsing = testParsing;

    }

    public void setSeparator(String sep){
        boolean reload = !sep.equals(info.separator);
        info.separator = sep;
        reloadFlag = true;
    }

    public String getSeparator(){
        return info.separator;
    }

    private void reloadRows(int max) throws NumberFormatException{
        this.rows = Spreadsheet.getRowsFromTable(tableFile, max, info.separator,
                info.headerOnFirstLine, info.labelsOnFirstCol, testParsing);
    }

    public List<String[]> getRows() throws NumberFormatException{
        return getRows(-1);
    }

    public List<String[]> getRows(int max) throws NumberFormatException{
        if(reloadFlag || lastMax == -2 || lastMax != max){
            reloadRows(max);
        }
        reloadFlag = false;
        lastMax = max;
        return rows;
    }

    public void setInfo(Info info){
        boolean reload = false;
        if(!info.separator.equals(this.info.separator)
                || info.getHeaderOnFirstLine() != this.info.getHeaderOnFirstLine()
                || info.getFirstCellPresent() != this.info.getFirstCellPresent()
                || info.getLabelsOnFirstCol() != this.info.getLabelsOnFirstCol()){
            reload = true;
        }

        this.info = info;
        reloadFlag = reload;
    }


    public Info getInfo(){
        return info;
    }

    /**
     * Meta-information about a spreadsheet file.
     */
    public static class Info{

        public static String guessLineSeparator(File tableFile){
            ArrayList<String> lines = Global.getFirstLinesFromFile(tableFile, 20);

            if(lines == null){
                Log.logger.warning("Could not precisely determine the line separator of '" + tableFile.getAbsolutePath()
                + "' because R-Peridot failed to read it.");
                return ",";
            }

            String[] separators = {"\t", "," , " " , ";"};
            float[] avgCellsPerLine = {-1.0f, -1.0f, -1.0f, -1.0f};
            for(int i = 0; i < separators.length; i++){
                for(String line : lines){
                    String[] split = line.split(separators[i]);
                    if(split.length > 0){
                        split = Global.joinArgsBetweenQuotes(split);
                        if(avgCellsPerLine[i] < 0.0f){
                            avgCellsPerLine[i] = (float)split.length;
                        }else{
                            avgCellsPerLine[i] = avgCellsPerLine[i] * (float)split.length;
                        }
                    }
                }
            }

            int max = 0;
            for(int i = 1; i < separators.length; i++){
                if(avgCellsPerLine[i] >= avgCellsPerLine[max]){
                    max = i;
                }
            }

            return separators[max];
        }

        /**
         * Cell separator char
         */
        public String separator;
        /**
         * If there are labels on the first column
         */
        private Boolean labelsOnFirstCol;
        /**
         * If there are headers on the first line
         */
        private Boolean headerOnFirstLine;
        /**
         * If the first cell (line 0, column 0) has
         * a header, label or value on it.
         */
        private Boolean firstCellPresent;

        /**
         *  R-Peridot tries to guess the Spreadsheet.Info of a spreadsheet.
         * @param tableFile The .csv or .tsv file to be analyzed.
         * @throws IOException If failed to read the table file.
         */
        public Info(File tableFile) throws IOException{
            Log.logger.info("Reading info of " + tableFile.getName());
            FileReader inputReader = new FileReader(tableFile);
            BufferedReader tableInput = new BufferedReader(inputReader);
            String line = tableInput.readLine();
            String line2 = tableInput.readLine();
            tableInput.close();
            inputReader.close();
            separator = guessLineSeparator(tableFile);
            if(line != null){
                if(line2 != null){
                    setFirstCellPresent(!(Global.split(line, separator).length < Global.split(line2, separator).length));
                }else{
                    setFirstCellPresent(true);
                }
            }else{
                setFirstCellPresent(false);
            }
            if(getFirstCellPresent()){
                setHeaderOnFirstLine(lineIsHeader(line, separator));
            }else{
                setHeaderOnFirstLine(true);
            }

            setLabelsOnFirstCol(true);
            Log.logger.info("Read info of " + tableFile.getName());
        }

        /**
         *
         * @param labelsOnFirstCol  If there are labels on the first column
         * @param headerOnFirstLine If there are headers on the first line
         * @param firstCellPresent  If the first cell (line 0, column 0) has
         *                          a header, label or value on it.
         * @param sep   Cell separator.
         */
        public Info(boolean labelsOnFirstCol,
                    boolean headerOnFirstLine,
                    boolean firstCellPresent,
                    String  sep){
            //this.dataType = dataType;
            this.labelsOnFirstCol = Boolean.valueOf(labelsOnFirstCol);
            this.headerOnFirstLine = Boolean.valueOf(headerOnFirstLine);
            this.firstCellPresent = Boolean.valueOf(firstCellPresent);
            this.separator = sep;
        }

        /**
         * Constructor that does nothing
         */
        public Info(){

        }

        public boolean allInfoSet(){
            return labelsOnFirstCol != null
                    && headerOnFirstLine != null
                    && separator != null;
        }

        public boolean getFirstCellPresent(){
            return this.firstCellPresent.booleanValue();
        }

        public static boolean getFirstCellPresent(File tableFile, String separator){
            try{
                FileReader inputReader = new FileReader(tableFile);
                BufferedReader tableInput = new BufferedReader(inputReader);
                String line = tableInput.readLine();
                String line2 = tableInput.readLine();
                tableInput.close();
                inputReader.close();
                //char separator = guessLineSeparator(tableFile);
                if(line != null){
                    if(line2 != null){
                        return (!(Global.split(line, separator).length < Global.split(line2, separator).length));
                    }else{
                        return (true);
                    }
                }else{
                    return (false);
                }
            }catch(IOException ex){
                ex.printStackTrace();
                return false;
            }
        }

        public boolean getLabelsOnFirstCol(){
            return this.labelsOnFirstCol.booleanValue();
        }

        public boolean getHeaderOnFirstLine(){
            return this.headerOnFirstLine.booleanValue();
        }

        public void setFirstCellPresent(boolean newValue){
            if(firstCellPresent == null){
                firstCellPresent = Boolean.valueOf(newValue);
            }else{
                firstCellPresent = Boolean.valueOf(newValue);
            }
        }

        public void setLabelsOnFirstCol(boolean newValue){
            if(labelsOnFirstCol == null){
                labelsOnFirstCol = Boolean.valueOf(newValue);
            }else{
                labelsOnFirstCol = Boolean.valueOf(newValue);
            }
        }

        public void setHeaderOnFirstLine(boolean newValue){
            if(headerOnFirstLine == null){
                headerOnFirstLine = Boolean.valueOf(newValue);
            }else{
                headerOnFirstLine = Boolean.valueOf(newValue);
            }
        }
    }
}
