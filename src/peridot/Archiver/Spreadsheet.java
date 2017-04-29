/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.Archiver;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import peridot.Global;
import peridot.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 *  Collection of utilities to handle spreadsheet-like files, .csv or .tsv.
 *  Uses Univocity's parsers.
 *  @author pentalpha
 */
public class Spreadsheet {
    /**
     * Types of values that can be found on the data: Integer or Rational
     */
    public enum DataType{
        Int, Float
    }

    /**
     * Meta-information about a spreadsheet file.
     */
    public static class Info{
        /**
         * Rationals or Integers
         */
        public DataType dataType;
        /**
         * If there are labels on the first column
         */
        public boolean labelsOnFirstCol;
        /**
         * If there are headers on the first line
         */
        public boolean headerOnFirstLine;
        /**
         * If the first cell (line 0, column 0) has
         * a header, label or value on it.
         */
        public boolean firstCellPresent;

        /**
         *
         * @param dataType          Rationals or Integers
         * @param labelsOnFirstCol  If there are labels on the first column
         * @param headerOnFirstLine If there are headers on the first line
         * @param firstCellPresent  If the first cell (line 0, column 0) has
         *                          a header, label or value on it.
         */
        public Info(DataType dataType, boolean labelsOnFirstCol, 
                boolean headerOnFirstLine, boolean firstCellPresent){
            this.dataType = dataType;
            this.labelsOnFirstCol = labelsOnFirstCol;
            this.headerOnFirstLine = headerOnFirstLine;
            this.firstCellPresent = firstCellPresent;
        }

        /**
         * Constructor that does nothing
         */
        public Info(){
            
        }
    }

    /**
     * @param line  line from a .csv or .tsv file.
     * @return      If the line is made mostly of words, not numbers.
     */
    public static boolean lineIsHeader(String line){
        String[] row = Global.splitWithTabOrCur(line);
        String firstWord = row[0];
        if(Global.lineIsWords(row)){
            Log.logger.info("The first line is made of words");
            return true;
        }
        
        return false;
    }

    /**
     *  R-Peridot tries to guess the Spreadsheet.Info of a spreadsheet.
     * @param tableFile     The .csv or .tsv file to be analyzed.
     * @return              Spreadsheet.Info instance with the info.
     * @throws IOException
     */
    public static Spreadsheet.Info getInfo(File tableFile) throws IOException{
        Spreadsheet.Info info = new Spreadsheet.Info();
        
        FileReader inputReader = new FileReader(tableFile);
        BufferedReader tableInput = new BufferedReader(inputReader);
        String line = tableInput.readLine();
        String line2 = tableInput.readLine();
        tableInput.close();
        inputReader.close();
        
        info.firstCellPresent = !(Global.splitWithTabOrCur(line).length < Global.splitWithTabOrCur(line2).length);
        if(info.firstCellPresent){
            info.headerOnFirstLine = lineIsHeader(line);
        }else{
            info.headerOnFirstLine = true;
        }
        if(Global.lineIsDoubles(Global.splitWithTabOrCur(line2))){
            info.dataType = DataType.Float;
        }else{
            info.dataType = DataType.Int;
        }
        info.labelsOnFirstCol = true;
        return info;
    }

    /**
     * @param tableFile .csv file, values separated by ','
     * @return          List of String[], with all the cells of the tableFile
     */
    public static List<String[]> getRowsFromCSV(File tableFile){
        List<String[]> allRows;
        CsvParserSettings settings = new CsvParserSettings();
        settings.getFormat().setLineSeparator("\n");
        CsvParser parser = new CsvParser(settings);
        allRows = parser.parseAll(tableFile);
        correctNoFirstCell(allRows);
        
        return allRows;
    }

    /**
     * @param tableFile .tsv file, values separated by tabulation
     * @return          List of String[], with all the cells of the tableFile
     */
    public static List<String[]> getRowsFromTSV(File tableFile){
        List<String[]> allRows;
        TsvParserSettings settings = new TsvParserSettings();
        settings.getFormat().setLineSeparator("\n");
        TsvParser parser = new TsvParser(settings);
        allRows = parser.parseAll(tableFile);
        correctNoFirstCell(allRows);

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
     * @param f Spreadsheet file
     * @return  True if f is a .csv or .tsv file
     */
    public static boolean fileIsCSVorTSV(File f){
       return fileIsCSV(f) || fileIsTSV(f);
    }

    public static boolean fileIsCSV(File f){
        return (f.getName().endsWith(".csv")) 
               || (f.getName().endsWith(".CSV"));
    }

    public static boolean fileIsTSV(File f){
        return (f.getName().endsWith(".tsv")) 
               || (f.getName().endsWith(".TSV"));
    }

    /**
     *
     * @param file  Spreadsheet file.
     * @return      Only the first row of the spreadsheet.
     */
    public static String[] getFirstRowFromFile(File file){
        String[] line = null;
        try{
            FileReader reader = new FileReader(file);
            BufferedReader buffReader = new BufferedReader(reader);
            String firstLine = buffReader.readLine();
            line = Global.splitWithTabOrCur(firstLine);
            buffReader.close();
            reader.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return line;
    }

    /**
     * @param line  String of text, not spliced.
     * @return      True if line contains the names of the samples.
     */
    public static boolean lineIsSampleNames(String line){
        String[] row  = peridot.Global.splitWithTabOrCur(line);
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
    public static String[] getDefaultColunnNames(int n){
        String[] defaultNames = new String[n];
        for(int i = 0; i < defaultNames.length; i++){
            defaultNames[i] = "Colunn " + (i+1);
        }
        return defaultNames;
    }

    /*public static String[][] getColunnNamesAndConditions(File file){
        String[][] colunnAndCondition = null;
        try{
            int nLines = Manager.countLines(file.getAbsolutePath());
            colunnAndCondition = new String[nLines][2];
            FileReader reader = new FileReader(file);
            BufferedReader buffReader = new BufferedReader(reader);
            String line;
            String[] splitedLine;
            for(int i = 0; i < colunnAndCondition.length; i++){
                line = buffReader.readLine();
                splitedLine = line.split("\t");
                colunnAndCondition[i][0] = splitedLine[0];
                colunnAndCondition[i][1] = splitedLine[1];
            }
            buffReader.close();
            reader.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return colunnAndCondition;
    }*/
    
    /*public static boolean tableFileHasHeader(File tableFile) throws IOException, LessWordsOnHeaderLineException{
        FileReader inputReader = new FileReader(tableFile);
        BufferedReader tableInput = new BufferedReader(inputReader);
        String line = tableInput.readLine();
        String line2 = tableInput.readLine();
        if(splitWithTabOrCur(line).length < splitWithTabOrCur(line2).length){
            Log.logger.info("Less words on header");
            throw new LessWordsOnHeaderLineException();
        }else if(lineIsHeader(line)){
            Log.logger.info("FirstLine is header");
            return true;
        }else{
            ConfirmNoHeaderDialog askTheUser = new ConfirmNoHeaderDialog(MainGUI._instance, tableFile);
            askTheUser.setVisible(true);
            Log.logger.info("Asking the user");
            return askTheUser.isHeaderOnFirstLine;
        }
        
    }*/
}
