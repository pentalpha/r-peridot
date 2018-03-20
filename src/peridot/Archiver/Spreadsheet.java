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

    private static List<String[]> getRowsFromTable(File tableFile, String separator){
        List<String[]> allRows = new LinkedList<>();
        try{
            FileReader inputReader = new FileReader(tableFile);
            BufferedReader buffInput = new BufferedReader(inputReader);
            String line = buffInput.readLine();
            while(line != null){
                String[] cells = Global.split(line, separator);
                allRows.add(cells);
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

    public Spreadsheet(File tableFile) throws IOException{
        this.info = new Info(tableFile);
        this.tableFile = tableFile;
    }

    public Spreadsheet(File tableFile, Info info){
        this.info = info;
        this.tableFile = tableFile;
    }

    private void setSeparator(String sep){
        info.separator = sep;
        if(rows != null){
            this.reloadRows();
        }
    }

    public String getSeparator(){
        return info.separator;
    }

    private void reloadRows(){
        this.rows = Spreadsheet.getRowsFromTable(tableFile, info.separator);
    }

    public List<String[]> getRows(){
        if(rows == null){
            reloadRows();
        }
        return rows;
    }

    public void setInfo(Info info){
        boolean reload = true;
        if(info.separator.equals(this.info.separator)){
            reload = false;
        }

        this.info = info;
        if(reload){
            boolean alreadyLoaded = rows != null;
            rows = null;
            if(alreadyLoaded){
                reloadRows();
            }
        }
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
                    String[] split = Global.joinArgsBetweenQuotes(line.split(separators[i]));
                    if(avgCellsPerLine[i] < 0.0f){
                        avgCellsPerLine[i] = (float)split.length;
                    }else{
                        avgCellsPerLine[i] = avgCellsPerLine[i] * (float)split.length;
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
            FileReader inputReader = new FileReader(tableFile);
            BufferedReader tableInput = new BufferedReader(inputReader);
            String line = tableInput.readLine();
            String line2 = tableInput.readLine();
            tableInput.close();
            inputReader.close();
            separator = guessLineSeparator(tableFile);
            setFirstCellPresent(!(Global.split(line, separator).length < Global.split(line2, separator).length));
            if(getFirstCellPresent()){
                setHeaderOnFirstLine(lineIsHeader(line, separator));
            }else{
                setHeaderOnFirstLine(true);
            }

            setLabelsOnFirstCol(true);
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
            this.labelsOnFirstCol = new Boolean(labelsOnFirstCol);
            this.headerOnFirstLine = new Boolean(headerOnFirstLine);
            this.firstCellPresent = new Boolean(firstCellPresent);
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

        public boolean getLabelsOnFirstCol(){
            return this.labelsOnFirstCol.booleanValue();
        }

        public boolean getHeaderOnFirstLine(){
            return this.headerOnFirstLine.booleanValue();
        }

        public void setFirstCellPresent(boolean newValue){
            if(firstCellPresent == null){
                firstCellPresent = new Boolean(newValue);
            }else{
                firstCellPresent = new Boolean(newValue);
            }
        }

        public void setLabelsOnFirstCol(boolean newValue){
            if(labelsOnFirstCol == null){
                labelsOnFirstCol = new Boolean(newValue);
            }else{
                labelsOnFirstCol = new Boolean(newValue);
            }
        }

        public void setHeaderOnFirstLine(boolean newValue){
            if(headerOnFirstLine == null){
                headerOnFirstLine = new Boolean(newValue);
            }else{
                headerOnFirstLine = new Boolean(newValue);
            }
        }
    }
}
