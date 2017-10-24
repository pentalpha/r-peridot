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
public class AnalysisData {
    public Spreadsheet.Info info;
    public File conditionsFile, expressionFile;
    public SortedMap<IndexedString, String> conditions;
    protected int countReadsThreshold;
    protected Global.RoundingMode roundMode;
    protected boolean hasReplicates = false;
    protected boolean moreThanTwoConditions = false;
    public AnalysisData(File expressionFile, File conditionsFile, Info info, String roundMode,
                        int countReadsThreshold) throws IOException
    {
        defaultBuilderOperations(expressionFile, conditionsFile, info, roundMode, countReadsThreshold);
    }
    
    public AnalysisData(File expressionFile, SortedMap<IndexedString, String> conditions, Info info, String roundMode,
                        int countReadsThreshold) throws IOException
    {
        File newConditionsFile = new File(expressionFile.getAbsolutePath() + ".conditions");
        this.setConditions(applyConditionsToExprFileSamples(expressionFile, info, conditions));
        createConditionsFile(newConditionsFile, conditions, false, true);
        defaultBuilderOperations(expressionFile, newConditionsFile, info, roundMode, countReadsThreshold);
    }

    public static SortedMap<IndexedString, String> applyConditionsToExprFileSamples(File exprFile, Info info,
                                                                                    SortedMap<IndexedString, String> cond){
        SortedMap<IndexedString, String> conditions = AnalysisData.getConditionsFromExpressionFile(exprFile, info);
        for(Map.Entry<IndexedString, String> entry : cond.entrySet()){
            conditions.put(entry.getKey(), entry.getValue());
        }
        return conditions;
    }
    
    private void defaultBuilderOperations(File expressionFile, File conditionsFile, Info info, String roundMode,
                                          int countReadsThreshold)  throws IOException
    {
        this.info = info;
        if(roundMode.equals("HALF_UP")){
            this.roundMode = Global.RoundingMode.HALF_UP;
        }else if(roundMode.equals("HALF_DOWN")){
            this.roundMode = Global.RoundingMode.HALF_DOWN;
        }else if(roundMode.equals("UP")){
            this.roundMode = Global.RoundingMode.UP;
        }else{
            this.roundMode = Global.RoundingMode.DOWN;
        }
        this.countReadsThreshold = countReadsThreshold;

        if(Manager.fileExists(expressionFile.getAbsolutePath())){
            this.expressionFile = expressionFile;
        }else{
            throw new IOException("Expression file does not exists.");
        }
        
        if(Manager.fileExists(conditionsFile.getAbsolutePath())){
            this.conditionsFile = conditionsFile;
            setConditions(loadConditionsFromFile(conditionsFile));
        }else{
            throw new IOException("Conditions file does not exists.");
        }
    }

    public void setConditions(SortedMap<IndexedString, String> newCond) {
        this.conditions = newCond;
        HashMap<String, Integer> sampleCountInCondition = new HashMap<>();
        for (Entry<IndexedString, String> entry : getNamesAndConditions()){
            if(sampleCountInCondition.containsKey(entry.getValue())){
                sampleCountInCondition.put(entry.getValue(),sampleCountInCondition.get(entry.getValue()) + 1);
            }else{
                sampleCountInCondition.put(entry.getValue(),1);
            }
        }

        hasReplicates = false;
        int nConditions = 0;
        for(Entry<String, Integer> entry : sampleCountInCondition.entrySet()){
            if(entry.getKey().equals("not-use") == false){
                nConditions++;
                if(entry.getValue() > 1) {
                    hasReplicates = true;
                }
            }
        }

        moreThanTwoConditions = (nConditions > 2);
    }

    public boolean hasReplicatesInSamples(){
        return hasReplicates;
    }

    public boolean hasMoreThanTwoConditions(){
        return moreThanTwoConditions;
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
    
    public void setConditionOf(String name, String condition){
       for(Entry<IndexedString, String> sample: conditions.entrySet()){
           if(sample.getKey().getText().equals(name)){
               conditions.put(sample.getKey(), condition);
               return;
           }
       }
    }
    
    public String getConditionOf(String name){
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
        AnalysisData.createConditionsFile(Places.conditionInputFile, conditions, true, false);
    }
    
    public static void createConditionsFile(File file,
                                            SortedMap<IndexedString, String> conditions,
                                            boolean makeHeader, boolean writeNotUses)
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
                String conditionName = conditions.get(sampleNames[i]);
                //Log.logger.info("With spaces: " + sampleNames[i].getText() + ", without: " + sampleName);
                if(!conditionName.equals("not-use") || writeNotUses){
                    text += sampleName + "\t" + conditionName + System.lineSeparator();
                }
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
            this.writeCountReadsWithoutConditions();
            this.writeFinalConditions();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
    
    private void writeCountReadsWithoutConditions() throws IOException{
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
            if(!sortedSampleName[i].equals("not-use")){
                firstLine += sortedSampleName[i];
                if(i != sortedSampleName.length-1){
                    firstLine += "\t";
                }
            }
        }
        buffOutput.write(firstLine);
        buffOutput.newLine();
        String line = buffInput.readLine();
        if(info.getHeaderOnFirstLine()){
            line = buffInput.readLine();
        }
        
        int counter = 1;
        int removeCounter = 0;
        while(line != null){
            String[] lineSplited = Global.splitWithTabOrCur(line);
            String[] values;
            String label;
            if(info.getLabelsOnFirstCol()){
                label = lineSplited[0];
                values = new String[lineSplited.length-1];
                for(int i = 0; i < values.length; i++){
                    values[i] = lineSplited[i+1];
                }
            }else{
                label = "gene" + counter;
                values = new String[lineSplited.length];
                for(int i = 0; i < values.length; i++){
                    values[i] = lineSplited[i];
                }
            }
            
            String[] sortedValues = new String[values.length];
            try{
                for(int i = 0; i < sortedValues.length; i++){
                    sortedValues[sampleNameNewIndex[i]] = values[i];
                }
            }catch (java.lang.ArrayIndexOutOfBoundsException ex){
                ex.printStackTrace();
                Log.logger.severe("Less samples tham values!");
            }

            int[] intSortedValues = roundValuesAndEraseNotUse(sortedValues, sortedSampleName);
            boolean eraseLine = filterValues(intSortedValues);
            if(eraseLine){
                removeCounter++;
                Log.logger.info(label + " line dropped by threshold.");
            }else{
                String lineToWrite = label;
                for(int i = 0; i < intSortedValues.length; i++){
                    lineToWrite += "\t" + intSortedValues[i];
                }
                buffOutput.write(lineToWrite);
                buffOutput.newLine();
            }

            
            line = buffInput.readLine();
            counter++;
        }
        
        buffInput.close();
        inputReader.close();
        buffOutput.close();
        outputWriter.close();

        Log.logger.info( ((float)removeCounter/(float)counter)*100 + "% of the lines dropped by threshold");
        this.setConditions(newConditions);
    }

    protected int[] roundValuesAndEraseNotUse(String[] values, String[] names){
        boolean[] isUsable = new boolean[values.length];
        int usableCount = 0;
        for (int i = 0; i < values.length; i++){
            isUsable[i] = !(names[i].equals("not-use"));
            if(isUsable[i]){
                usableCount++;
            }
        }
        int[] decimals = new int[usableCount];
        int decimalsIndex = 0;
        for (int i = 0; i < values.length; i++){
            if(isUsable[i]){
                decimals[decimalsIndex] = Global.roundFloat(Float.parseFloat(values[i]), roundMode);
                if(decimals[decimalsIndex] < countReadsThreshold){
                    decimals[decimalsIndex] = 0;
                }
                decimalsIndex++;
            }
        }
        return decimals;
    }

    protected boolean filterValues(int[] values){
        for (int i = 0; i < values.length; i++){
            if(values[i] >= countReadsThreshold){
                return false;
            }
        }
        return true;
    }
}
