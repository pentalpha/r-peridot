package peridot;

import peridot.Archiver.Manager;
import peridot.Archiver.Places;
import peridot.Archiver.Spreadsheet;
import peridot.Archiver.Spreadsheet.Info;
import peridot.CLI.AnalysisFileParser;
import peridot.CLI.AnalysisFileParser.ParseException;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
/**
 * Manages the count reads file and all the meta-information about it.
 * @author pentalpha
 */
public class AnalysisData {
    public Map<String, Integer> samples;
    public Spreadsheet.Info info;
    public File conditionsFile, expressionFile;
    public SortedMap<IndexedString, String> conditions;
    protected int countReadsThreshold;
    protected Global.RoundingMode roundMode;
    protected boolean hasReplicates = false;
    protected boolean moreThanTwoConditions = false;
    protected File finalCountReadsFile;
    protected File finalConditionsFile;
    public AnalysisData(File expressionFile, File conditionsFile, Info info, String roundMode,
                        int countReadsThreshold) throws IOException, AnalysisFileParser.ParseException
    {
        defaultBuilderOperations(expressionFile, conditionsFile, info, roundMode, countReadsThreshold);
    }
    
    public AnalysisData(File expressionFile, SortedMap<IndexedString, String> conditions, Info info, String roundMode,
                        int countReadsThreshold) throws IOException, AnalysisFileParser.ParseException
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
                                          int countReadsThreshold)  throws IOException, AnalysisFileParser.ParseException
    {
        this.finalCountReadsFile = Places.countReadsInputFile;
        this.finalConditionsFile = Places.conditionInputFile;
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

        if(this.countReadsThreshold < 1){
            this.countReadsThreshold = 1;
        }

        if(Manager.fileExists(expressionFile.getAbsolutePath())){
            setExpressionFile(expressionFile, info);
        }else{
            throw new IOException("Expression file does not exists.");
        }
        
        if(Manager.fileExists(conditionsFile.getAbsolutePath())){
            this.conditionsFile = conditionsFile;
            SortedMap<IndexedString, String> cond = loadConditionsFromFile(conditionsFile, this.samples);
            if(cond == null){
                throw new AnalysisFileParser.ParseException(
                    "Failed to load conditions from " + conditionsFile.getAbsolutePath()
                );
            }
            setConditions(cond);
        }else{
            throw new IOException("Conditions file does not exists.");
        }
    }

    public void setCountReadsFile(File file){
        finalCountReadsFile = file;
    }

    public void setConditionsFile(File file){
        finalConditionsFile = file;
    }

    public void setConditionsInfo(){
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

    public void setConditions(SortedMap<IndexedString, String> newCond) {
        this.conditions = newCond;
        setConditionsInfo();
    }

    public void setExpressionFile(File file, Info info) throws AnalysisFileParser.ParseException{
        this.expressionFile = file;
        this.samples = getIndexedSamplesFromFile(file, info);
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
        int count = 0;
        for(Entry<IndexedString, String> entry : conditions.entrySet()){
            if(!entry.getValue().equals("not-use")){
                count++;
            }
        }
        return count;
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
            if(Spreadsheet.rowIsNames(Spreadsheet.getFirstRowFromFile(conditionsFile, "\t"))){
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

    private static String[] splitConditionsLine(String line){
        line = line.replace(" ", "\t").replace(",", "\t")
                .replace(";", "\t");
        String[] parts = line.split("\t");
        String[] first_and_last = {parts[0], parts[parts.length-1]};
        return first_and_last;
    }

    public static SortedMap<IndexedString, String> loadConditionsFromFile(
        File file, Map<String, Integer> samples)
    {
        SortedMap<IndexedString, String> map = new TreeMap<>();
        HashMap<String, String> map2 = new HashMap<>();

        /*for(String str : samples.keySet()){
            Log.logger.info(str + " is " + samples.get(str));
        }*/

        Set<String> addedSamples = new TreeSet<String>();
        try{
            FileReader fileReader = new FileReader(file);
            BufferedReader buffReader = new BufferedReader(fileReader);  
            String line = null;
            map.clear();
            while ((line = buffReader.readLine()) != null)  
            {  
                if (!line.equals(conditionsHeader)){
                    String[] nameAndCondition = splitConditionsLine(line);
                    if(nameAndCondition.length == 2){
                        String name = nameAndCondition[0];
                        String condition = nameAndCondition[1];
                        if(map2.containsKey(name)){
                            Log.logger.severe("Ambiguous sample condition: "+
                            "\n"+name+"\t"+map2.get(name)+
                            "\n"+name+"\t"+condition + "\nignoring it.");
                            //return null;
                        }
                        System.out.println(name + "\t" + condition);
                        Integer index = samples.get(name);
                        IndexedString key = new IndexedString(index, name);
                        map.put(key, condition);
                        map2.put(name, condition);
                        addedSamples.add(name);
                    }else{
                        Log.logger.severe("Line with more tham two collumns in conditions file:\n"+line);
                        //do something if the line is wrong
                    }
                }
            } 
            buffReader.close();
            fileReader.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }

        if(addedSamples.size() < samples.keySet().size()){
            Log.logger.info("Some samples do not have a defined condition. They will be marked as 'not-use':");
            for (String sample : samples.keySet()){
                if(!addedSamples.contains(sample)){
                    map.put(new IndexedString(samples.get(sample),sample), "not-use");
                    Log.logger.info(sample + "\tnot-use");
                }
            }
        }
        
        return map;
    }

    private static String[] getSampleNamesFromFile(File file, Info info){
        String[] firstRow = Spreadsheet.getFirstRowFromFile(file, info.separator);
        if(info.getHeaderOnFirstLine()){
            Vector<String> rowVector = new Vector<String>();
            for (int i = 0; i < firstRow.length;i++){
                rowVector.add(firstRow[i]);
            }

            if(info.getFirstCellPresent()){
                rowVector.remove(0);
                //Log.logger.info("First cell present");
            }

            String[] names = new String[1];
            names = rowVector.toArray(names);
            //Global.printArray(names);
            return names; 
        }else{
            int nConditions = firstRow.length;
            if(info.getLabelsOnFirstCol()){
                nConditions -= 1;
            }
            return Spreadsheet.getDefaultColumnNames(nConditions);
        }
    }

    public static Map<String, Integer> getIndexedSamplesFromFile(File file, Info info) throws AnalysisFileParser.ParseException{
        String[] names = getSampleNamesFromFile(file, info);

        Map<String, Integer> samples = new HashMap<>();
        for(int i = 0; i < names.length; i++){
            if(samples.containsKey(names[i])){
                throw new AnalysisFileParser.ParseException("Duplicated sample names in count reads table: " + "\n"+names[i]+ " at " + samples.get(names[i]) + " and " + i);
            }else{
                samples.put(names[i], Integer.valueOf(i));
            }
        }
        return samples;
    }
    
    public static SortedMap<IndexedString, String> getConditionsFromExpressionFile(
        File file, 
        Info info)
    {
        SortedMap<IndexedString, String> map;
        map = new TreeMap<>();
        
        String[] names = getSampleNamesFromFile(file, info);
        
        map.clear();
        for (int i = 0; i < names.length; i++) {
            map.put(new IndexedString(i, names[i]), "not-use");
        }
        
        return map;
    }
    
    protected void writeFinalConditions(){
        AnalysisData.createConditionsFile(this.finalConditionsFile, conditions, true, false);
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
                text += conditionsHeader + System.lineSeparator();
            }

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
    
    public void writeExpression() throws NumberFormatException{
        writeExpression(false);
    }

    public void writeExpression(boolean optimize) throws NumberFormatException{
        try{
            this.writeCountReadsWithoutConditions(optimize);
            this.writeFinalConditions();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    private void writeCountReadsWithoutConditions(boolean optimize) throws IOException, NumberFormatException{
        File newRNASeq = finalCountReadsFile;
        if(newRNASeq.exists()){
            newRNASeq.delete();
        }
        newRNASeq.createNewFile();
        
        FileReader inputReader = new FileReader(expressionFile);
        FileWriter outputWriter = new FileWriter(newRNASeq);
        BufferedReader buffInput = new BufferedReader(inputReader);
        BufferedWriter buffOutput = new BufferedWriter(outputWriter);

        //IndexedString[] sampleNames = new IndexedString[conditions.size()];
        IndexedString[] sampleNames = new IndexedString[samples.size()];
        String[] conditionNames = new String[samples.size()];
        String[] sampleNameWithCondition = new String[samples.size()];
        for (Map.Entry<String, Integer> entry : samples.entrySet()){
            String key = entry.getKey();
            int val = entry.getValue().intValue();
            if(val >= sampleNames.length){
                Log.logger.severe(val + " ("+key+") is out of bounds ");
            }
            sampleNames[val] = new IndexedString(val, key);
            conditionNames[val] = conditions.get(sampleNames[val]);
            sampleNameWithCondition[val] = conditionNames[val] + "-" + key;
        }
        
        //Log.logger.info("Unsorted conditions: ");
        //Global.printArray(sampleNameWithCondition);
        
        //Global.printArray(sampleNameWithCondition);
        Arrays.sort(sampleNameWithCondition);
        //Log.logger.info("Sorted conditions: " + sampleNameWithCondition[57]);
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
            String[] lineSplited = Global.split(line, info.separator);
            
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
                for(int i = 0; i < values.length; i++){
                    //sortedValues[i] = values[sampleNameNewIndex[i]];
                    sortedValues[sampleNameNewIndex[i]] = values[i];
                    if(sampleNameNewIndex[i] != i){
                        //Log.logger.info("column " + i +" new index is " + sampleNameNewIndex[i]);
                    }
                }
            }catch (java.lang.ArrayIndexOutOfBoundsException ex){
                ex.printStackTrace();
                Log.logger.severe("Less samples tham values!");
            }

            int[] intSortedValues = roundValuesAndEraseNotUse(sortedValues, sortedSampleName);
            if(intSortedValues == null){
                Log.logger.severe("Null value read in :" + line);
                Global.printArray(lineSplited);
                Global.printArray(values);
                Global.printArray(sortedValues);
            }
            boolean eraseLine = filterValues(intSortedValues, optimize);
            if(eraseLine){
                removeCounter++;
                Log.logger.fine(label + " line dropped by threshold.");
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

        if (optimize){
            Log.logger.info( ((float)removeCounter/(float)counter)*100 + "% of the lines dropped by threshold, with a threshold of " + countReadsThreshold);
        }else{
            Log.logger.info(((float)removeCounter/(float)counter)*100 + "% of the lines dropped by threshold, because only contained zeros");
        }
        
        this.setConditions(newConditions);
    }

    protected int[] roundValuesAndEraseNotUse(String[] values, String[] names) throws NumberFormatException{
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
        float x;
        for (int i = 0; i < values.length; i++){
            if(isUsable[i]){
                try{
                    x = Float.parseFloat(values[i]);
                }catch(Exception ex){
                    ex.printStackTrace();
                    String str = "Cannot parse number: " + values[i];
                    Log.logger.severe(str);
                    Global.printArray(values);
                    throw new NumberFormatException(str);
                }

                decimals[decimalsIndex] = Global.roundFloat(x, roundMode);
                if(decimals[decimalsIndex] < countReadsThreshold){
                    decimals[decimalsIndex] = 0;
                }
                decimalsIndex++;
            }
        }
        return decimals;
    }

    protected boolean filterValues(int[] values, boolean optimize){
        for (int i = 0; i < values.length; i++){
            if((optimize && values[i] >= countReadsThreshold)
                || (!optimize && values[i] > 0)){
                return false;
            }
        }
        return true;
    }

    public static String conditionsHeader = "sample\tcondition";
}
