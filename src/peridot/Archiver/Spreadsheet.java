/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.Archiver;
import peridot.Log;
import peridot.Global;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author pentalpha
 */
public class Spreadsheet {
    public enum DataType{
        Int, Float
    }
    
    public static class Info{
        public DataType dataType;
        public boolean labelsOnFirstCol;
        public boolean headerOnFirstLine;
        public boolean firstCellPresent;
        
        public Info(DataType dataType, boolean labelsOnFirstCol, 
                boolean headerOnFirstLine, boolean firstCellPresent){
            this.dataType = dataType;
            this.labelsOnFirstCol = labelsOnFirstCol;
            this.headerOnFirstLine = headerOnFirstLine;
            this.firstCellPresent = firstCellPresent;
        }
        public Info(){
            
        }
    }
    
    public static boolean lineIsHeader(String line){
        String[] row = Global.splitWithTabOrCur(line);
        String firstWord = row[0];
        if(Global.lineIsWords(row)){
            Log.logger.info("The first line is made of words");
            return true;
        }
        
        return false;
    }
    
    
    
    public static Spreadsheet.Info getInfo(File tableFile, boolean promptUserToo) throws IOException{
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
    
    
    
    public static List<String[]> getRowsFromCSV(File tableFile){
        List<String[]> allRows;
        CsvParserSettings settings = new CsvParserSettings();
        settings.getFormat().setLineSeparator("\n");
        CsvParser parser = new CsvParser(settings);
        allRows = parser.parseAll(tableFile);
        correctNoFirstCell(allRows);
        
        return allRows;
    }

    public static List<String[]> getRowsFromTSV(File tableFile){
        List<String[]> allRows;
        TsvParserSettings settings = new TsvParserSettings();
        settings.getFormat().setLineSeparator("\n");
        TsvParser parser = new TsvParser(settings);
        allRows = parser.parseAll(tableFile);
        correctNoFirstCell(allRows);

        return allRows;
    }
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

    public static boolean isTableFile(String fileName){
        return fileName.contains(".csv") 
               || fileName.contains(".tsv");
    }

    

    public static String[] getDefaultHeader(int x){
        String[] names = new String[x];
        for(int i = 0; i < x; i++){
            names[i] = "Column" + i;
        }
        return names;
    }

    

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

    public static boolean lineIsSampleNames(String line){
        String[] row  = peridot.Global.splitWithTabOrCur(line);
        return lineIsSampleNames(row);
    }
    
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
