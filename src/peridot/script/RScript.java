/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import peridot.AnalysisParameters;
import peridot.Archiver.Manager;
import peridot.Archiver.Places;
import peridot.GeneIdType;
import peridot.Log;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

/**
 *
 * @author pentalpha
 */
public class RScript implements Serializable{
    public static HashMap<String, RScript> availableScripts;
    //Nome dos parametros necessarios e suas respectivas classes
    //O script não irá executar se não forem todos passados antes
    public Map<String, Class> requiredParameters = null;
    public Map<String, Object> parameters = null;
    //O caminho dos arquivos externos necessarios para a execução interna do script
    //O script não irá executar se não forem todos passados antes
    public Set<String> requiredExternalFiles = null;
    public Set<String> requiredScripts = null;
    //Nomes dos arquivos de resultados que o script irá gerar e colocar na sua
    //respectiva pasta /temp.
    public Set<String> results = null;
    public Set<String> mandatoryResults = null;
    protected String scriptName = null;
    public File workingDirectory = null;
    protected File configFile = null;
    public File resultsFolder = null;
    public File scriptFile = null;
    public String name = null;
    public boolean environmentCreated;
    public boolean nonDefaultScript;
    public boolean max2Conditions;
    public boolean mandatoryFailed;
    public String info = null;
    public static Map<String, Class> availableParamTypes = defineAvailableParamTypes();
    public StringBuilder scriptContent = null;
    /**
     *  The extension of the RScript binary file
     */
    public static final String binExtension = "PeridotModule";
    
    public RScript(String name, String scriptFile, boolean externalScript,
            Map<String, Class> requiredParameters, 
            Set<String> requiredExternalFiles,
            Set<String> results,
            Set<String> requiredScripts)
    {
        this.max2Conditions = false;
        this.nonDefaultScript = externalScript;
        this.name = name;
        this.scriptName = scriptFile;
        this.requiredParameters = requiredParameters;
        this.requiredExternalFiles = requiredExternalFiles;
        this.results = results;
        this.mandatoryResults = new TreeSet<>();
        this.requiredScripts = requiredScripts;
        this.parameters = new HashMap<String, Object>();
        this.info = "";
        this.scriptFile = getScriptFile();
        this.workingDirectory = getWorkingDirectory();
        this.configFile = getConfigFile();
        this.environmentCreated = false;
        this.mandatoryFailed = false;
        loadScriptContent();
    }
    
    private void loadScriptContent(){
        if(this.getScriptFile().exists()){
            this.scriptContent = peridot.Archiver.Manager.fileToString(getScriptFile());
        }
    }
    
    public RScript(File dir) throws Exception
    {
        this.environmentCreated = true;
        this.parameters = new TreeMap<String, Object>();
        this.results = new TreeSet<>();
        this.mandatoryResults = new TreeSet<>();
        this.requiredScripts = new TreeSet<String>();
        this.requiredParameters = new TreeMap<String, Class>();
        this.requiredExternalFiles = new TreeSet<String>();
        this.info = "";
        
        File descriptionFile = new File(dir.getAbsolutePath() + File.separator
                                + "description");
        if(descriptionFile.exists() == false){
            throw new Exception("script description file not found in " 
                    + descriptionFile.getAbsolutePath());
        }

        FileReader fileReader = new FileReader(descriptionFile);
        BufferedReader reader = new BufferedReader(fileReader);

        String line = reader.readLine();
        while(line != null){
            String[] words = line.split("\t");
            String category;
            String value;
            String value2;
            if (words.length > 1){
                category = words[0];
                value = words[1];
                if(words.length == 2)
                {
                    if(category.equals("[NAME]"))
                    {
                        this.name = value;
                    }else if(category.equals("[EXTERNAL-SCRIPT]")){
                        this.nonDefaultScript = Boolean.parseBoolean(value);
                    }
                    else if(category.equals("[SCRIPT-NAME]"))
                    {
                        this.scriptName = value;
                    }
                    else if(category.equals("[RESULT]"))
                    {
                        this.results.add(value);
                    }
                    else if(category.equals("[MANDATORY-RESULT]"))
                    {
                        this.results.add(value);
                        this.mandatoryResults.add(value);
                    }
                    else if(category.equals("[REQUIRED-INPUT-FILE]"))
                    {
                        this.requiredExternalFiles.add(value);
                    }
                    else if(category.equals("[REQUIRED-SCRIPT]"))
                    {
                        this.requiredScripts.add(value);
                    }
                    else if(category.equals("[MAX-2-CONDITIONS]"))
                    {
                        this.max2Conditions = Boolean.parseBoolean(value);
                    }
                    else if(category.equals("[INFO]")){
                        this.info += value + "\n";
                    }
                    else
                    {
                        throw new Exception("Unknown category: " + category);
                    }

                }
                else if(words.length == 3)
                {
                    value2 = words[2];

                    if(category.equals("[REQUIRED-PARAMETER]"))
                    {
                        if(AnalysisParameters.availableParamTypes.keySet().contains(value2)){
                            this.requiredParameters.put(value, AnalysisParameters.availableParamTypes.get(value2));
                        }/*
                    if(value2.equals("Integer")){
                        this.requiredParameters.put(value, Integer.class);
                    }
                    else if(value2.equals("Float")){
                        this.requiredParameters.put(value, Float.class);
                    }
                    else if(value2.equals("String")){
                        this.requiredParameters.put(value, String.class);
                    }
                    else if(value2.equals("Boolean")){
                        this.requiredParameters.put(value, Boolean.class);
                    }
                    else if(value2.equals("GeneIdType")){
                        this.requiredParameters.put(value, GeneIdType.class);
                    }
                    */else
                        {
                            throw new Exception("Unknown parameter type: " + value2);
                        }
                    }
                    else
                    {
                        throw new Exception("Unknown category: " + category);
                    }
                }
                else
                {
                    throw new Exception("Number of words in line different from 2 or 3: " + words.length
                            + ". In:\n" + line + "\nModule: " + dir.getAbsolutePath());
                }
            }else{
                throw new Exception("Number of words in line different from 2 or 3: " + words.length
                        + ". In:\n" + line + "\nModule: " + dir.getAbsolutePath());
            }

            line = reader.readLine();
        }
        
        reader.close();
        fileReader.close();
        
        workingDirectory = getWorkingDirectory();
        configFile = getConfigFile();
        scriptFile = getScriptFile();
        this.resultsFolder = new File(workingDirectory.getAbsolutePath()
                + File.separator + "results");
        if(resultsFolder.exists() == false){
            FileUtils.forceMkdir(resultsFolder);
        }
        loadScriptContent();
        this.mandatoryFailed = false;
    }

    public File getDescriptionFile(){
        return new File(getWorkingDirectoryPath() + File.separator + "description");
    }
    
    public void createClassDescription() throws IOException{
        File description = new File(getWorkingDirectoryPath() + File.separator + "description");
        description.createNewFile();
        
        FileWriter fileWriter = new FileWriter(description);
        BufferedWriter writer = new BufferedWriter(fileWriter);
        
        
        writer.write("[NAME]\t"+ this.name + System.lineSeparator());
        writer.write("[EXTERNAL-SCRIPT]\t" + this.nonDefaultScript + System.lineSeparator());
        writer.write("[SCRIPT-NAME]\t" + this.scriptName + System.lineSeparator());
        writer.write("[MAX-2-CONDITIONS]\t" + this.max2Conditions + System.lineSeparator());
        for(String result : this.results){
            if(mandatoryResults.contains(result)){
                writer.write("[MANDATORY-RESULT]\t"+ result + System.lineSeparator());
            }else{
                writer.write("[RESULT]\t"+ result + System.lineSeparator());
            }
            
        }
        for(String file : this.requiredExternalFiles){
            writer.write("[REQUIRED-INPUT-FILE]\t"+ file + System.lineSeparator());
        }
        for(String script : this.requiredScripts){
            writer.write("[REQUIRED-SCRIPT]\t"+ script + System.lineSeparator());
        }
        for(Map.Entry<String, Class> pair : this.requiredParameters.entrySet()){
            //Log.logger.info("pair.getKey() == " + pair.getKey());
            //System.out.println(pair.getKey());
            String className = pair.getValue().getSimpleName();
            writer.write("[REQUIRED-PARAMETER]\t"+ pair.getKey() + "\t" + className + System.lineSeparator());
        }
        writer.write("[INFO]\t" + this.info + System.lineSeparator());
        
        writer.close();
        fileWriter.close();
    }
    
    /**
     * Cria o diretorio temporario (workingdirectory) onde serão guardados o script, os resultados e etc.
     * @param newScriptFilePath External script to be copied to environment. Can be null if its a internalScript.
     */
    public void createEnvironment(String newScriptFilePath){
        try{
            Log.logger.info("trying to create " + getWorkingDirectoryPath());
            FileUtils.deleteDirectory(getWorkingDirectory());
            this.getWorkingDirectory().mkdirs();
            this.getWorkingDirectory().mkdir();
            if(!getWorkingDirectory().exists()){
                Log.logger.info("could not create " + getWorkingDirectory().getName());
            }else{
                Log.logger.info("created " + getWorkingDirectory().getName());
            }
            if(newScriptFilePath != null){
                if(!copyExternalScriptToWorkingDir(newScriptFilePath, scriptName)){
                    throw new IOException("Could not copy external script " + newScriptFilePath);
                }
            }else if(scriptContent != null){
                exportScriptToDir();
            }
            loadScriptContent();
            resultsFolder = new File(this.getWorkingDirectoryPath() + File.separator + "results");
            FileUtils.forceMkdir(resultsFolder);
            if(!resultsFolder.exists()){
                Log.logger.info("could not create " + resultsFolder.getAbsolutePath());
            }
            this.environmentCreated = true;
            createClassDescription();
        }catch(Exception ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    public void exportScriptToDir(){
        if(scriptContent != null){
            if(scriptContent.toString().length() > 1){
                if(getScriptFile().exists()){
                    getScriptFile().delete();
                    FileUtils.deleteQuietly(getScriptFile());
                }
                try{
                    getScriptFile().createNewFile();
                    PrintWriter out = new PrintWriter(getScriptFile());
                    out.println(scriptContent.toString());
                    out.close();
                }catch(Exception ex){
                    Log.logger.severe("Could not export script from RScript to script dir.");
                    Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
    }
    
    public void setInfo(String info){
        this.info = info;
    }
    
    public void setResultAsMandatory(String res){
        if(results.contains(res)){
            mandatoryResults.add(res);
        }
    }
    
    private static Map<String, Class> defineAvailableParamTypes(){
        Map<String, Class> params = new TreeMap<>();
        params.put(Integer.class.getSimpleName(), Integer.class);
        params.put(Float.class.getSimpleName(), Float.class);
        params.put(GeneIdType.class.getSimpleName(), GeneIdType.class);
        return params;
    }
    
    public Vector<String> getParamsDescription(){
        Vector<String> list = new Vector<>();
        for(Map.Entry<String, Class> pair : this.requiredParameters.entrySet()){
            String entry = "";
            entry += pair.getKey() + ": ";
            entry += pair.getValue().getSimpleName();
            list.add(entry);
        }
        return list;
    }
    
    public Object[] getInputFiles(){
        return this.requiredExternalFiles.toArray();
    }
    
    public String getScriptPath(){
        if(scriptFile != null){
            return scriptFile.getAbsolutePath();
        }else{
            return getWorkingDirectoryPath() + File.separator + this.scriptName;
        }
    }
    
    public File getScriptFile(){
        if(scriptFile != null){
            return scriptFile;
        }else{
            return new File(getScriptPath());
        }
    }
    
    public String getWorkingDirectoryPath(){
        if(workingDirectory != null){
            return workingDirectory.getAbsolutePath();
        }
        return Places.scriptsDir + File.separator + name 
                                + "." + getScriptType();
    }
    public File getWorkingDirectory(){
        if(workingDirectory != null){
            return workingDirectory;
        }
        return new File(getWorkingDirectoryPath());
    }
    
    public String getConfigPath(){
        if(configFile != null){
            return configFile.getAbsolutePath();
        }else{
            return getWorkingDirectory() + File.separator + "config.txt";
        }
    }
    
    public File getConfigFile(){
        if(configFile != null){
            return configFile;
        }else{
            return new File(getConfigPath());
        }
    }
    
    public void passParameter(String parameterName, Object parameter){
        parameters.put(parameterName, parameter);
    }
    
    public void passParameters(AnalysisParameters params){
        for(Map.Entry<String, Class> pair : requiredParameters.entrySet()){
            if(params.parameters.containsKey(pair.getKey())){
                passParameter(pair.getKey(), params.parameters.get(pair.getKey()));
            }
        }
    }
    
    public void clearParameters(){
        parameters.clear();
    }
    
    //verifica se os requerimentos para executar o script foram supridos
    public boolean requirementsSufficed(){
        return parametersSufficed() && filesSufficed();
    }
    
    public boolean parametersSufficed(){
        for(Map.Entry<String, Class> pair : this.requiredParameters.entrySet()){
            Object parameter = parameters.get(pair.getKey());
            if(parameter == null){
                Log.logger.info(pair.getKey() + " not received");
                return false;
            }else{
                if(parameter.getClass() != pair.getValue()){
                    Log.logger.info("expected a " + pair.getValue().toString()
                            + " but received a " + parameters.getClass().toString()
                            + ", in " + pair.getKey());
                    return false;
                }
            }
        }
        return true;
    }
    
    public boolean filesSufficed(){
        for(String filePath : this.requiredExternalFiles){
            if(Manager.fileExists(Places.finalResultsDir.getAbsolutePath() + File.separator + filePath) == false){
                //Log.logger.info(filePath + " does not exists");
                return false;
            }
        }
        return true;
    }
    
    public Set<String> getNotExistantResults(){
        TreeSet<String> notExist = new TreeSet<String>();
        for(String filePath : this.requiredExternalFiles){
            if(Manager.fileExists(Places.finalResultsDir.getAbsolutePath() + File.separator + filePath) == false){
                notExist.add(filePath);
            }
        }
        return notExist;
    }
    
    public Set<String> filesAreInRequirements(AbstractCollection<String> toVerify){
        TreeSet<String> present = new TreeSet<String>();
        for(String filePath : this.requiredExternalFiles){
            for(String otherPath : toVerify){
                if(otherPath.equals(filePath)){
                    present.add(otherPath);
                }
            }
        }
        return present;
    }
    
    public boolean createConfig(){
        if(parametersSufficed() && this.environmentCreated){
            this.createConfigFile();
            return true;
        }
        return false;
    }
    
    //copia os resultados salvos na pasta (getWorkingDirectory())/temp em uma pasta externa, de forma permanente
    private void saveResultsAt(String newFolder){
        for(String result : this.results){
            try{
                File resultFile = new File(this.resultsFolder.getAbsolutePath() + File.separator + result);
                File destination = new File(newFolder + File.separator + result);
                if(resultFile.exists()){
                    new File(newFolder).mkdir();
                    FileUtils.copyFile(resultFile, destination);
                }
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }
    
    public void saveResults(){
        saveResultsAt(Places.finalResultsDir.getAbsolutePath() + File.separator
                        + this.name + "." + this.getClass().getSimpleName());
    }
    
    //utiliza requiredParameters para criar o ConfigFile com os parametros
    protected void createConfigFile(){
        if(parametersSufficed() && workingDirectory.exists()){
            try{
                this.getConfigFile().createNewFile();
                FileWriter fileWriter = new FileWriter(this.getConfigFile());
                PrintWriter printWriter = new PrintWriter(fileWriter);
                LinkedList<String> names = new LinkedList<String>();
                LinkedList<Object> values = new LinkedList<Object>();
                for(Map.Entry<String, Object> pair : parameters.entrySet()){
                    names.add(pair.getKey());
                    values.add(pair.getValue());
                }
                printWriter.print("id|");
                for(String name : names){
                    printWriter.print(name);
                    if(name != names.getLast()){
                        printWriter.print("|");
                    }
                }
                printWriter.print("\n");
                printWriter.print("1|");
                for(Object value : values){
                    printWriter.print(value.toString());
                    if(value != values.getLast()){
                        printWriter.print("|");
                    }
                }
                printWriter.print("\n");
                printWriter.close();
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
        
    }
    
    
    
    
    private boolean copyExternalScriptToWorkingDir(String path, String name){
        try{
            File external = new File(path);
            File inner = new File(getWorkingDirectoryPath() + File.separator + name);
            FileUtils.copyFile(external, inner);
            scriptFile = inner;
            return true;
        }catch(IOException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            return false;
        }
    }
    
    //exclui pasta com o script, o arquivo de configurações e os resultados gerados
    public void clearResults(){
        Log.logger.info("cleaning " + this.name + " previous results");
        try{
            FileUtils.deleteDirectory(resultsFolder);
        }catch(Exception ex){
            Log.logger.info("error cleaning results");
            ex.printStackTrace();
        }
    }
    
    //exclui pasta com o script, o arquivo de configurações e os resultados gerados
    public void clearEnvironment(){
        Log.logger.info("cleaning " + this.name);
        try{
            FileUtils.deleteDirectory(getWorkingDirectory());
            this.environmentCreated = false;
        }catch(Exception ex){
            Log.logger.info("error cleaning");
            ex.printStackTrace();
        }
    }
    
    public boolean verifyResults(){
        int nSuccessful = 0;
        for(String resultName : results){
            if(new File(this.resultsFolder + File.separator + resultName)
                    .exists() == false){
                Log.logger.warning("The expected result does not exists: " 
                        + this.resultsFolder + File.separator + resultName);
                
                if(mandatoryResults.contains(resultName)){
                    Log.logger.severe(resultName + " was a mandatory result "
                            + " and it failed!");
                    mandatoryFailed = true;
                    return false;
                }
            }else{
                nSuccessful++;
            }
        }
        mandatoryFailed = false;
        return (nSuccessful > 0);
    }
    
    public void cleanLocalResults(){
        Iterator<File> iterator = FileUtils.iterateFiles(this.resultsFolder, null, false);
        while(iterator.hasNext()){
            File file = iterator.next();
            try{
                file.delete();
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }
    
    public static void removeScriptResults(){
        File dir = Places.finalResultsDir;
        if(dir.exists()){
            for(File file : FileUtils.listFilesAndDirs(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)){
                if(file.isDirectory() && (file.getAbsolutePath().equals(dir.getAbsolutePath()) == false)){
                    try{
                        FileUtils.deleteDirectory(file);
                        //file.delete();
                    }catch(IOException ex){
                        ex.printStackTrace();
                        Log.logger.info("Cant delete " + file.getName() + " results.");
                    }
                }
            }
        }
    }
    
    public void cleanTempFiles(){
        Iterator<File> iterator = FileUtils.iterateFiles(getWorkingDirectory(), null, false);
        while(iterator.hasNext()){
            File file = iterator.next();
            if(file.getAbsolutePath().equals(resultsFolder.getAbsolutePath())
                    || file.getName().equals("description")
                    || file.getAbsolutePath().equals(getWorkingDirectoryPath())
                    || file.getAbsolutePath().equals(scriptFile.getAbsolutePath()))
            {
                
            }
            else
            {
                try{
                    file.delete();
                }catch(Exception ex){
                    Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
                
            }
        }
    }
    
    public static Vector<String> getAvailablePackages(){
        Vector<String> scripts = new Vector<>();
        for(Map.Entry<String, RScript> pair : RScript.availableScripts.entrySet()){
            if(pair.getValue().getWorkingDirectory().getName().contains(".AnalysisScript")){
                scripts.add(pair.getKey());
                //Log.logger.info("fount result: " + pair.getKey());
            }
        }
        return scripts;
    }
    
    public static Vector<String> getAvailablePostAnalysisScripts(){
        Vector<String> scripts = new Vector<>();
        for(Map.Entry<String, RScript> pair : RScript.availableScripts.entrySet()){
            if(pair.getValue().getWorkingDirectory().getName().contains(".PostAnalysisScript")){
                scripts.add(pair.getKey());
                //Log.logger.info("fount result: " + pair.getKey());
            }
        }
        return scripts;
    }
    
    public static Vector<String> getAvailableScripts(){
        Vector<String> scripts = new Vector<>();
        for(Map.Entry<String, RScript> pair : RScript.availableScripts.entrySet()){
            if(pair.getValue() instanceof PostAnalysisScript){
                scripts.add(pair.getKey());
            }
        }
        return scripts;
    }
    
    public static void updateUserScripts(){
        loadUserScripts();
    }
    
    public static boolean deleteScript(String script){
        try{
            //new File(Places.sgsDir + File.separator + "log.txt").delete();
            FileUtils.deleteDirectory(RScript.availableScripts.get(script).getWorkingDirectory());
            return true;
        }catch(IOException ex){
            Log.logger.severe("Could not delete " 
                    + RScript.availableScripts.get(script).workingDirectory.getName());
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            return false;
        }
    }
    
    //load all the scripts in the temp folder and sort them
    public static void loadUserScripts(){
        HashMap<String, RScript> loadedScripts = new HashMap<String, RScript>();
        Set<File> subFiles;
        Set<File> subDirs = new TreeSet<File>();
        //Log.logger.info("trying to get subfiles");
        subFiles = new TreeSet<File>(FileUtils.listFilesAndDirs(Places.scriptsDir,
                                     TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        //Log.logger.info("iterating subfiles subfiles");
        for(File file : subFiles){
            String filePath = file.getAbsolutePath();
            if((filePath.equals(Places.scriptsDir.getAbsolutePath()) == false)
                && (file.getAbsolutePath().contains("results") == false)
                && (file.getAbsolutePath().contains("results"+File.separator) == false)
                && (file.isDirectory() == true))
            {
                subDirs.add(file);
            }
        }
        
        
        for(File file : subDirs){
            //Log.logger.info("iterate subdir " + file.getAbsolutePath());
            try{
                if(file.getName().contains(".PostAnalysisScript")){
                    PostAnalysisScript script = new PostAnalysisScript(file);
                    loadedScripts.put(script.name, script);
                }else if(file.getName().contains(".AnalysisScript")){
                    //Log.logger.info("creating " + file.getName());
                    AnalysisScript script = new AnalysisScript(file);
                    //Log.logger.info("created");
                    loadedScripts.put(script.name, script);
                }
            }catch(Exception ex){
                Log.logger.info("ERROR\nCould not import " + file.getName() + ", because: ");
                Log.logger.info(ex.getMessage());
                ex.printStackTrace();
            }
        }
        
        availableScripts = loadedScripts;
        if(loadedScripts.size() <= 0){
            Log.logger.severe("Could not load any script from user folder.");
        }
        //sortAvailableScripts();
        //return loadedScripts;
    }

    public static Map<String, Class> getRequiredParametersFromModules(){
        Map<String, Class> params = new HashMap<>();
        for(RScript script : availableScripts.values()){
            for(Map.Entry<String, Class> param : script.requiredParameters.entrySet()){
                params.put(param.getKey(), param.getValue());
            }
        }
        return params;
    }
    
    public static boolean makeDefaultScriptsFolders(){
        try{
            LinkedList<RScript> scripts = new LinkedList<>();
            //Package deSeq = new RNASeqPackage("DESeq", "scriptDESeq.R", false);
            //scripts.add(deSeq);
            AnalysisScript ebSeq = new RNASeqPackage("EBSeq", "scriptEBSeq.R", false, true);
            ebSeq.setInfo("From bioconductor.org:[LINE-BREAK]"
                    + "EBSeq: An R package for gene and isoform differential "
                    + "expression analysis of RNA-seq data.[LINE-BREAK]Differential "
                    + "Expression analysis at both gene and isoform level using "
                    + "RNA-seq data.[LINE-BREAK]Author: Ning Leng, Christina Kendziorski."
                    + "[LINE-BREAK]Maintainer: Ning Leng <lengning1 at gmail.com>.");
            scripts.add(ebSeq);
            AnalysisScript edgeR = new RNASeqPackage("edgeR", "scriptEdgeR.R", false, false);
            edgeR.setInfo("From bioconductor.org:[LINE-BREAK]"
                    + "edgeR: Empirical AnalysisData of Digital Gene Expression "
                    + "Data in R.[LINE-BREAK]Differential expression analysis of "
                    + "RNA-seq expression profiles with biological replication. "
                    + "Implements a range of statistical methodology based on the "
                    + "negative binomial distributions, including empirical Bayes "
                    + "estimation, exact tests, generalized linear models and quasi-likelihood "
                    + "tests. As well as RNA-seq, it be applied to differential "
                    + "signal analysis of other types of genomic data that produce "
                    + "counts, including ChIP-seq, SAGE and CAGE.[LINE-BREAK]Author: "
                    + "Yunshun Chen <yuchen at wehi.edu.au>, Aaron Lun <alun at "
                    + "wehi.edu.au>, Davis McCarthy <dmccarthy at wehi.edu.au>, "
                    + "Xiaobei Zhou <xiaobei.zhou at uzh.ch>, Mark Robinson <mark.robinson "
                    + "at imls.uzh.ch>, Gordon Smyth <smyth at wehi.edu.au>[LINE-BREAK]"
                    + "Maintainer: Yunshun Chen <yuchen at wehi.edu.au>, Aaron Lun "
                    + "<alun at wehi.edu.au>, Mark Robinson <mark.robinson at imls.uzh.ch>, "
                    + "Davis McCarthy <dmccarthy at wehi.edu.au>, Gordon Smyth <smyth at wehi.edu.au>");
            scripts.add(edgeR);
            AnalysisScript sseq = new RNASeqPackage("sSeq", "scriptSeq.R", false, false);
            sseq.setInfo("From bioconductor.org:[LINE-BREAK]"
                    + "sSeq: Shrinkage estimation of dispersion in Negative "
                    + "Binomial models for RNA-seq experiments with small sample "
                    + "size.[LINE-BREAK]The purpose of this package is to discover "
                    + "the genes that are differentially expressed between two "
                    + "conditions in RNA-seq experiments. Gene expression is "
                    + "measured in counts of transcripts and modeled with the "
                    + "Negative Binomial (NB) distribution using a shrinkage "
                    + "approach for dispersion estimation. The method of moment "
                    + "(MM) estimates for dispersion are shrunk towards an "
                    + "estimated target, which minimizes the average squared "
                    + "difference between the shrinkage estimates and the initial "
                    + "estimates. The exact per-gene probability under the NB "
                    + "model is calculated, and used to test the hypothesis that "
                    + "the expected expression of a gene in two conditions "
                    + "identically follow a NB distribution.[LINE-BREAK]Author: "
                    + "Danni Yu <dyu at purdue.edu>, Wolfgang Huber <whuber at "
                    + "embl.de> and Olga Vitek <ovitek at purdue.edu>"
                    + "[LINE-BREAK]Maintainer: Danni Yu <dyu at purdue.edu>");
            scripts.add(sseq);
            AnalysisScript deSeq = new RNASeqPackage("DESeq", "scriptDESeq.R", false, true);
            deSeq.setInfo("From bioconductor.org:[LINE-BREAK]"
                    + "DESeq: Differential gene expression analysis based on "
                    + "the negative binomial distribution.[LINE-BREAK]Estimate "
                    + "variance-mean dependence in count data from high-throughput "
                    + "sequencing assays and test for differential expression "
                    + "based on a model using the negative binomial distribution"
                    + "[LINE-BREAK]Author: Simon Anders, EMBL Heidelberg <sanders at fs.tum.de>."
                    + "[LINE-BREAK]Maintainer: Simon Anders <sanders at fs.tum.de>.");
            scripts.add(deSeq);
            AnalysisScript deSeq2 = new RNASeqPackage("DESeq2", "scriptDESeq2.R", false, false);
            deSeq2.setInfo("From bioconductor.org:[LINE-BREAK]"
                    + "DESeq2: Differential gene expression analysis based "
                    + "on the negative binomial distribution.[LINE-BREAK]Estimate "
                    + "variance-mean dependence in count data from high-throughput "
                    + "sequencing assays and test for differential expression "
                    + "based on a model using the negative binomial distribution."
                    + "[LINE-BREAK]Author: Michael Love, Simon Anders, Wolfgang "
                    + "Huber.[LINE-BREAK]Maintainer: Michael Love "
                    + "<michaelisaiahlove at gmail.com>.");
            scripts.add(deSeq2);
            
            Set<String> requiredScripts;
            Set<String> requiredExternalFiles;
            Set<String> results;
            
            requiredScripts = new TreeSet<String>();
            requiredExternalFiles = new TreeSet<String>();
            //requiredExternalFiles.add(Places.countReadsInputFileName);
            results = new TreeSet<String>();
            results.add("Intersect.tsv");
            results.add("plots.pdf");
            results.add("vennDiagramPlot.png");
            PostAnalysisScript vennDiagram = new PostAnalysisScript("VennDiagram", "vennDiagram.R", false, 
                    new HashMap<String, Class>(), requiredExternalFiles,
                    results, requiredScripts);
            vennDiagram.setResultAsMandatory("Intersect.tsv");
            vennDiagram.setInfo("VennDiagram: Reads the results from all AnalysisData "
                    + "Modules and creates Intersect.tsv (a spreadsheet file with "
                    + "the intersection of the selected reads) and a Venn Diagram "
                    + "plot these same results.");
            scripts.add(vennDiagram);
            
            requiredScripts = new TreeSet<String>();
            requiredExternalFiles = new TreeSet<String>();
            //requiredExternalFiles.add(Places.countReadsInputFileName);
            //requiredExternalFiles.add(Places.conditionInputFileName);
            results = new TreeSet<String>();
            results.add("BoxPlot.png");
            PostAnalysisScript boxPlot = new PostAnalysisScript("BoxPlot", "scriptBoxPlot.R", false, 
                    new HashMap<String, Class>(), requiredExternalFiles,
                    results, requiredScripts);
            boxPlot.setInfo("BoxPlot: Produces box-and-whisker plot of each sample reads.");
            scripts.add(boxPlot);
            
            requiredScripts = new TreeSet<String>();
            requiredScripts.add(vennDiagram.name);
            requiredExternalFiles = new TreeSet<String>();
            requiredExternalFiles.add("VennDiagram.PostAnalysisScript"
                    + File.separator + "Intersect.tsv");
            results = new TreeSet<String>();
            results.add("PCA.png");
            results.add("NormalizedCounts.tsv");
            results.add("Dendrogram.png");
            results.add("HeatMap.png");
            results.add("HeatMap.pdf");
            results.add("aux1.pdf");
            results.add("aux2.pdf");
            PostAnalysisScript heatMap = new PostAnalysisScript("HeatMap", "Heatmap.R", false, 
                    new HashMap<String, Class>(), requiredExternalFiles,
                    results, requiredScripts);
            heatMap.setResultAsMandatory("HeatMap.png");
            heatMap.setInfo("HeatMap: Creates a normalized version of VennDiagram's "
                    + "\"Intersect.tsv\", a Dendogram plot and a HeatMap plot.");
            scripts.add(heatMap);
            
            PostAnalysisScript postOntology = new PostAnalysisOntology(
                    "EnrichmentClusterProfiler", 
                    "clusterProfiler.R", false);
            postOntology.setInfo("From bioconductor.org:[LINE-BREAK]"
                    + "ClusterProfiler: Statistical analysis and "
                    + "visualization of functional profiles for genes and gene "
                    + "clusters.[LINE-BREAK]This package implements methods to "
                    + "analyze and visualize functional profiles (GO and KEGG) "
                    + "of gene and gene clusters.[LINE-BREAK]Author: Guangchuang"
                    + " Yu [aut, cre], Li-Gen Wang [ctb], Giovanni Dall'Olio [ctb] "
                    + "(formula interface of compareCluster)[LINE-BREAK]"
                    + "Maintainer: Guangchuang Yu <guangchuangyu at gmail.com>");
            scripts.add(postOntology);
            
            for(RScript x : scripts){
                x.createEnvironment(null);
            }
            return true;
        }catch(Exception ex){
            Log.logger.info("erro");
            ex.printStackTrace();
            return false;
        }
    }
    
    public void toBin(File file) throws IOException{
        peridot.Archiver.Persistence.saveObjectAsBin(file, this);
    }
    
    public String getScriptType(){
        if(this instanceof AnalysisScript){
            return "AnalysisScript";
        }else if(this instanceof PostAnalysisScript){
            return "PostAnalysisScript";
        }else{
            return "RScript";
        }
    }
}
