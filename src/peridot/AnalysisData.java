package peridot;

import peridot.Archiver.Manager;
import peridot.Archiver.Places;
import peridot.Archiver.Spreadsheet;
import peridot.Archiver.Spreadsheet.Info;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
/**
 *
 * @author pentalpha
 */
public class Analysis {
    public Spreadsheet.Info info;
    public File conditionsFile, expressionFile;
    public SortedMap<IndexedString, String> conditions;
    public Analysis(File expressionFile, File conditionsFile, Info info) throws IOException{
        defaultBuilderOperations(expressionFile, conditionsFile, info);
    }
    
    public Analysis(File expressionFile, SortedMap<IndexedString, String> conditions, Info info) throws IOException{
        File newConditionsFile = new File(expressionFile.getAbsolutePath() + ".conditions");
        this.conditions = conditions;
        createConditionsFile(newConditionsFile, conditions, false);
        defaultBuilderOperations(expressionFile, newConditionsFile, info);
    }
    
    private void defaultBuilderOperations(File expressionFile, File conditionsFile, Info info)  throws IOException{
        this.info = info;
        if(Manager.fileExists(expressionFile.getAbsolutePath())){
            this.expressionFile = expressionFile;
        }else{
            throw new IOException("Expression file does not exists.");
        }
        
        if(Manager.fileExists(conditionsFile.getAbsolutePath())){
            this.conditionsFile = conditionsFile;
            this.conditions = loadConditionsFromFile(conditionsFile);
        }else{
            throw new IOException("Conditions file does not exists.");
        }
    }
    
    
    
    public Set<Entry<IndexedString, String>> getNamesAndConditions(){
        return this.conditions.entrySet();
    }
    
    public int numberOfColunns(){
        return conditions.size();
    }
    
    public int getNumberOfSamples(){
        return conditions.keySet().size();
    }
    
    public void setCondition(String name, String condition){
       for(Entry<IndexedString, String> sample: conditions.entrySet()){
           if(sample.getKey().getText().equals(name)){
               conditions.put(sample.getKey(), condition);
               return;
           }
       }
    }
    
    public String getCondition(String name){
       for(Entry<IndexedString, String> sample: conditions.entrySet()){
           if(sample.getKey().getText().equals(name)){
               return sample.getValue();
           }
       }
       return null;
    }
    
    public int getNumberOfGenes(){
        try{
            if(Spreadsheet.rowIsNames(Spreadsheet.getFirstRowFromFile(conditionsFile))){
                return Manager.countLines(expressionFile.getAbsolutePath()) - 1;
            }else{
                return Manager.countLines(expressionFile.getAbsolutePath());
            }
        }catch(Exception ex){
            ex.printStackTrace();
            return -1;
        }
    }
    
    public int getNumberOfConditions(){
        List<String> valueList = new LinkedList<String>(conditions.values());
        Set<String> conditionsSet = new HashSet<String>();
        for(String string : valueList){
            conditionsSet.add("" + string);
        }
        boolean hasNotUse = false;
        for(String string : conditionsSet){
            if(string.equals("not-use")){
                hasNotUse = true;
                break;
            }
        }
        if(hasNotUse){
            return conditionsSet.size() - 1;
        }else{
            return conditionsSet.size();
        }
    }
    
    public static SortedMap<IndexedString, String> loadConditionsFromFile(File file){
        SortedMap<IndexedString, String> map = new TreeMap<>();
        try{
            FileReader fileReader = new FileReader(file);
            BufferedReader buffReader = new BufferedReader(fileReader);  
            String line = null;
            map.clear();
            int i = 0;
            while ((line = buffReader.readLine()) != null)  
            {  
               String[] nameAndCondition = line.split("\t");
               
               if(nameAndCondition.length == 2){
                   String name = nameAndCondition[0];
                   String condition = nameAndCondition[1];
                   map.put(new IndexedString(i, name), condition);
                   i++;
               }else{
                   //do something if the line is wrong
               }
               
            } 
            buffReader.close();
            fileReader.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        
        return map;
    }
    
    public static SortedMap<IndexedString, String> 
        getConditionsFromExpressionFile(File file, Info info){
        SortedMap<IndexedString, String> map;
        map = new TreeMap<>();
        
        String[] firstRow = Spreadsheet.getFirstRowFromFile(file);
        int nConditions;
        if(info.getFirstCellPresent() == false || info.getLabelsOnFirstCol() == false){
            nConditions = firstRow.length;
        }else{
            nConditions = firstRow.length-1;
        }
        String[] names;
        
        if(info.getHeaderOnFirstLine()){
            names = new String[nConditions];
            for(int i = 0; i < names.length; i++){
                if(info.getFirstCellPresent() && info.getLabelsOnFirstCol()){
                    names[i] = firstRow[i+1];
                }else{
                    names[i] = firstRow[i];
                }
            }
        }else{
            names = Spreadsheet.getDefaultColunnNames(nConditions);
        }
        
        map.clear();
        for (int i = 0; i < names.length; i++) {
            map.put(new IndexedString(i, names[i]), "not-use");
        }
        
        return map;
    }
    
    protected void writeFinalConditions(){
        Analysis.createConditionsFile(Places.conditionInputFile, conditions, true);
    }
    
    public static void createConditionsFile(File file,
                                               SortedMap<IndexedString, String> conditions, boolean makeHeader)
    {
        try{
            if(file.exists() == true){
                file.delete();
            }
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter buffWriter = new BufferedWriter(fileWriter);

            String text = "";
            if(makeHeader){
                text += "sample\tcondition" + System.lineSeparator();
            }
            /*String nameWithCondition, name, condition;
            for(int i = 0; i < sampleNameWithCondition.length; i++){
                nameWithCondition = sampleNameWithCondition[i];
                int sepIndex = nameWithCondition.indexOf("-");
                name = nameWithCondition.substring(sepIndex+1);
                condition = nameWithCondition.substring(0, sepIndex-1);
                text += name + "\t" + condition + System.lineSeparator();
            }*/
            IndexedString[] sampleNames = conditions.keySet().toArray(new IndexedString[conditions.size()]);
            //Arrays.sort(sampleNames);
            for(int i = 0; i < sampleNames.length; i++){
                String sampleName = Global.noSpaces(sampleNames[i].getText());
                //Log.logger.info("With spaces: " + sampleNames[i].getText() + ", without: " + sampleName);
                text += sampleName + "\t" + conditions.get(sampleNames[i]) + System.lineSeparator();
            }

            //Log.logger.info("printing:\n" + text);
            buffWriter.write(text);
            buffWriter.close();
            fileWriter.close();
        }catch(Exception ex){
            Log.logger.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    public void writeExpression(){
        try{
            this.writeRNASeqWithoutConditions();
            this.writeFinalConditions();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    private void writeRNASeqWithoutConditions() throws IOException{
        File newRNASeq = Places.countReadsInputFile;
        if(newRNASeq.exists()){
            newRNASeq.delete();
        }
        newRNASeq.createNewFile();
        
        FileReader inputReader = new FileReader(expressionFile);
        FileWriter outputWriter = new FileWriter(newRNASeq);
        BufferedReader buffInput = new BufferedReader(inputReader);
        BufferedWriter buffOutput = new BufferedWriter(outputWriter);

        IndexedString[] sampleNames = new IndexedString[conditions.size()];
        String[] conditionNames = new String[conditions.size()];
        String[] sampleNameWithCondition = new String[conditions.size()];
        for(int i = 0; i < sampleNames.length; i++){
            for(Map.Entry<IndexedString, String> pair : conditions.entrySet()){
                if(pair.getKey().getNumber() == i){
                        sampleNames[i] = pair.getKey();
                        conditionNames[i] = pair.getValue();
                        sampleNameWithCondition[i] = pair.getValue() + "-" + pair.getKey().getText();
                    break;
                }
            }
        }
        //Log.logger.info("Unsorted conditions: ");
        //Global.printArray(sampleNameWithCondition);
        
        Arrays.sort(sampleNameWithCondition);
        //Log.logger.info("Sorted conditions: ");
        //Global.printArray(sampleNameWithCondition);
        
        int[] sampleNameNewIndex = new int[sampleNames.length];
        for(int i = 0; i < sampleNames.length; i++)
        {
            for(int j = 0; j < sampleNameWithCondition.length; j++){
                //System.out.println("-" + sampleNames[i].getText() + " in " + sampleNameWithCondition[j] + "?");
                if(sampleNameWithCondition[j].contains("-" + sampleNames[i].getText())){
                    sampleNameNewIndex[i] = j;
                    break;
                }
            }
        }

        for(int j = 0; j < sampleNameWithCondition.length; j++){
            for(int i = 0; i < sampleNames.length; i++) {
                if (sampleNameWithCondition[j].contains("-" + sampleNames[i].getText())) {
                    //Log.logger.info("Changing " + sampleNames[i] + " from " + sampleNames[i].getNumber()
                    //+ " to " + j);
                    sampleNames[i].setIndex(j);
                    break;
                }
            }
        }

        SortedMap<IndexedString, String> newConditions = new TreeMap<>();
        for(int i = 0; i < sampleNames.length; i++)
        {
            newConditions.put(sampleNames[i], conditionNames[i]);
        }
        //Arrays.sort(sampleNames);
        //Log.logger.info("New positions: ");
        //Global.printArray(sampleNameNewIndex);
        
        String[] sortedSampleName = new String[sampleNameWithCondition.length];
        for(int i = 0; i < sortedSampleName.length; i++)
        {
            if(sampleNameWithCondition[i].contains("not-use")){
                sortedSampleName[i] = "not-use";
            }else{
                String nameWithCondition = sampleNameWithCondition[i];
                int sepIndex = nameWithCondition.indexOf("-");
                sortedSampleName[i] = nameWithCondition.substring(sepIndex+1);
            }
        }
        
        String firstLine = "gene-id\t";
        for(int i = 0; i < sortedSampleName.length; i++){
           firstLine += sortedSampleName[i];
           if(i != sortedSampleName.length-1){
               firstLine += "\t";
           }
        }
        buffOutput.write(firstLine);
        buffOutput.newLine();
        String line = buffInput.readLine();
        if(info.getHeaderOnFirstLine()){
            line = buffInput.readLine();
        }
        
        int counter = 1;
        while(line != null){
            String[] lineSplitted = Global.splitWithTabOrCur(line);
            String[] values;
            String label;
            if(info.getLabelsOnFirstCol()){
                label = lineSplitted[0];
                values = new String[lineSplitted.length-1];
                for(int i = 0; i < values.length; i++){
                    values[i] = lineSplitted[i+1];
                }
            }else{
                label = "gene" + counter;
                values = new String[lineSplitted.length];
                for(int i = 0; i < values.length; i++){
                    values[i] = lineSplitted[i];
                }
            }
            
            String[] sortedValues = new String[values.length];
            for(int i = 0; i < sortedValues.length; i++){
                sortedValues[sampleNameNewIndex[i]] = values[i];
            }
            
            String lineToWrite = label;
            for(int i = 0; i < sortedValues.length; i++){
                lineToWrite += "\t" + sortedValues[i];
            }
            buffOutput.write(lineToWrite);
            buffOutput.newLine();
            
            line = buffInput.readLine();
            counter++;
        }
        
        buffInput.close();
        inputReader.close();
        buffOutput.close();
        outputWriter.close();

        conditions = newConditions;
    }
}
