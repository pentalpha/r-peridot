/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import peridot.AnalysisParameters;
import peridot.ConsensusThreshold;
import peridot.Archiver.Manager;
import peridot.Archiver.Places;
import peridot.GeneIdType;
import peridot.Log;
import peridot.Organism;
import peridot.script.r.Interpreter;
import peridot.script.r.Package;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

/**
 *
 * @author pentalpha
 */
public class RModule implements Serializable{
    ///////////////////////////////////////////////
    //static fields and methods ///////////////////
    ///////////////////////////////////////////////
    public static HashMap<String, RModule> availableModules;
    public static Map<String, Class> availableParamTypes = defineAvailableParamTypes();
    /**
     *  The extension of the RModule binary file
     */
    public static final String binExtension = "PeridotModule";

    private static Map<String, Class> defineAvailableParamTypes(){
        Map<String, Class> params = new TreeMap<>();
        params.put(Integer.class.getSimpleName(), Integer.class);
        params.put(Float.class.getSimpleName(), Float.class);
        params.put(GeneIdType.class.getSimpleName(), GeneIdType.class);
        params.put(Organism.class.getSimpleName(), Organism.class);
        params.put(ConsensusThreshold.class.getSimpleName(), ConsensusThreshold.class);
        return params;
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
                }else if(file.isFile() && file.getAbsolutePath().contains(".output")){
                    FileUtils.deleteQuietly(file);
                }
            }
        }
    }

    public static Vector<String> getAvailableAnalysisModules(){
        Vector<String> scripts = new Vector<>();
        for(Map.Entry<String, RModule> pair : RModule.availableModules.entrySet()){
            if(pair.getValue().getWorkingDirectory().getName().contains(".AnalysisModule")){
                scripts.add(pair.getKey());
                //Log.logger.info("fount result: " + pair.getKey());
            }
        }
        return scripts;
    }

    public static Vector<String> getAvailablePostAnalysisModules(){
        Vector<String> scripts = new Vector<>();
        for(Map.Entry<String, RModule> pair : RModule.availableModules.entrySet()){
            if(pair.getValue().getWorkingDirectory().getName().contains(".PostAnalysisModule")){
                scripts.add(pair.getKey());
                //Log.logger.info("fount result: " + pair.getKey());
            }
        }
        return scripts;
    }

    public static Vector<String> getAvailableModules(){
        Vector<String> scripts = new Vector<>();
        for(Map.Entry<String, RModule> pair : RModule.availableModules.entrySet()){
            //if(pair.getValue() instanceof PostAnalysisModule){
            //    scripts.add(pair.getKey());
            //}
            scripts.add(pair.getKey());
        }
        return scripts;
    }

    public static Set<String> modulesWithUnmetDependencies(){
        if(Interpreter.isDefaultInterpreterDefined()){
            return modulesWithUnmetDependencies(Interpreter.defaultInterpreter);
        }else{
            HashSet<String> set = new HashSet<String>();
            set.addAll(getAvailableModules());
            return set;
        }
    }

    public static Set<String> modulesWithUnmetDependencies(Interpreter interpreter){
        Set<String> cannotRun = new HashSet<>();
        for(String module : RModule.getAvailableModules()){
            if(!RModule.availableModules.get(module).requiredPackagesInstalled(interpreter)){
                cannotRun.add(module);
            }
        }
        return cannotRun;
    }

    public static void updateUserScripts(){
        loadUserScripts();
    }

    public static boolean deleteScript(String script){
        try{
            //new File(Places.sgsDir + File.separator + "log.txt").delete();
            FileUtils.deleteDirectory(RModule.availableModules.get(script).getWorkingDirectory());
            Log.logger.info("Deleting " + RModule.availableModules.get(script).getWorkingDirectory().getAbsolutePath());
        }catch(IOException ex){
            Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
            return false;
        }

        boolean reallyDeleted = RModule.availableModules.get(script).getWorkingDirectory().exists() == false;
        if(!reallyDeleted){
            Log.logger.severe("Could not delete "
                    + RModule.availableModules.get(script).workingDirectory.getName());
        }

        return reallyDeleted;
    }

    //load all the scripts in the temp folder and sort them
    public static void loadUserScripts(){
        HashMap<String, RModule> loadedScripts = new HashMap<String, RModule>();
        Set<File> subFiles;
        Set<File> subDirs = new TreeSet<File>();
        //Log.logger.info("trying to get subfiles");
        subFiles = new TreeSet<File>(FileUtils.listFilesAndDirs(Places.modulesDir,
                TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        //Log.logger.info("iterating subfiles subfiles");
        for(File file : subFiles){
            String filePath = file.getAbsolutePath();
            if((filePath.equals(Places.modulesDir.getAbsolutePath()) == false)
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
                if(file.getName().contains(".PostAnalysisModule")){
                    PostAnalysisModule script = new PostAnalysisModule(file);
                    loadedScripts.put(script.name, script);
                }else if(file.getName().contains(".AnalysisModule")){
                    //Log.logger.info("creating " + file.getName());
                    AnalysisModule script = new AnalysisModule(file);
                    //Log.logger.info("created");
                    loadedScripts.put(script.name, script);
                }
            }catch(Exception ex){
                Log.logger.info("ERROR\nCould not import " + file.getName() + ", because: ");
                Log.logger.info(ex.getMessage());
                ex.printStackTrace();
            }
        }

        availableModules = loadedScripts;
        if(loadedScripts.size() <= 0){
            Log.logger.severe("Could not load any script from user folder.");
        }
        //sortAvailableScripts();
        //return loadedScripts;
    }

    public static Map<String, Class> getRequiredParametersFromModules(){
        Map<String, Class> params = new HashMap<>();
        for(RModule script : availableModules.values()){
            for(Map.Entry<String, Class> param : script.requiredParameters.entrySet()){
                params.put(param.getKey(), param.getValue());
            }
        }
        return params;
    }

    public static Set<Package> requiredPackages(){
        Set<Package> req = new HashSet<>();
        for(RModule module : RModule.availableModules.values()){
            req.addAll(module.requiredPackages);
        }
        return req;
    }

    ///////////////////////////////////////////
    // instance fields and methods ////////////
    ///////////////////////////////////////////

    //Nome dos parametros necessarios e suas respectivas classes
    //O script não irá executar se não forem todos passados antes
    public Map<String, Class> requiredParameters = null;
    public Set<Package> requiredPackages = null;

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
    public boolean max2Conditions;
    public boolean needsReplicates;
    public boolean mandatoryFailed;
    public boolean needsAllDependencies;
    public String info = null;
    public StringBuilder scriptContent = null;

    
    public RModule(String name, String scriptFile,
                   Map<String, Class> requiredParameters,
                   Set<String> requiredExternalFiles,
                   Set<String> results,
                   Set<String> requiredScripts)
    {
        this.max2Conditions = false;
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
    
    public RModule(File dir) throws Exception
    {
        this.environmentCreated = true;
        this.needsAllDependencies = true;
        this.parameters = new TreeMap<String, Object>();
        this.results = new TreeSet<>();
        this.mandatoryResults = new TreeSet<>();
        this.requiredScripts = new TreeSet<String>();
        this.requiredParameters = new TreeMap<String, Class>();
        this.requiredExternalFiles = new TreeSet<String>();
        this.requiredPackages = new HashSet<>();
        this.info = "";

        this.needsReplicates = false;
        this.max2Conditions = false;
        
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
                    else if(category.equals("[NEEDS-REPLICATES]"))
                    {
                        this.needsReplicates = Boolean.parseBoolean(value);
                    }
                    else if(category.equals("[REQUIRES-ALL-DEPENDENCIES]"))
                    {
                        this.needsAllDependencies = Boolean.parseBoolean(value);
                    }
                    else if(category.equals("[INFO]")){
                        this.info += value + "\n";
                    }
                    else
                    {
                        throw new Exception("Unknown category: " + category + " " + value);
                    }

                }
                else if(words.length == 3)
                {
                    value2 = words[2];

                    if(category.equals("[REQUIRED-PARAMETER]")) {
                        if(AnalysisParameters.availableParamTypes.keySet().contains(value2)){
                            this.requiredParameters.put(value, AnalysisParameters.availableParamTypes.get(value2));
                        }
                        else{
                            throw new Exception("Unknown parameter type: " + value2);
                        }
                    }else if(category.equals("[PACKAGE]")) {
                        Package pack = new Package(value, value2);
                        this.requiredPackages.add(pack);
                    }else
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
        writer.write("[SCRIPT-NAME]\t" + this.scriptName + System.lineSeparator());
        writer.write("[MAX-2-CONDITIONS]\t" + this.max2Conditions + System.lineSeparator());
        writer.write("[NEEDS-REPLICATES]\t" + this.needsReplicates + System.lineSeparator());
        writer.write("[NEEDS-REPLICATES]\t" + this.needsAllDependencies + System.lineSeparator());
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
        for(Package pack : this.requiredPackages){
            writer.write("[PACKAGE]\t" + pack.name + "\t" + pack.version.toString() + System.lineSeparator());
        }
        String infoStr = "[INFO]\t";
        String[] linesRaw = this.info.split("\n");
        ArrayList<String> lines = new ArrayList<>();
        for(int i = 0; i < linesRaw.length; i++){
            String line = linesRaw[i];
            if(line.length() > 0){
                if(!(line.length() == 1 && line.equals("\t"))){
                    lines.add(line.replace(infoStr, ""));
                }
            }
        }

        for(String line : lines){
            writer.write(infoStr + line + System.lineSeparator());
        }
        
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
                    Log.logger.severe("Could not export script from RModule to script dir.");
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
        return Places.modulesDir + File.separator + name
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
        String scriptTypeExtension = ".PostAnalysisModule";
        if(RModule.getAvailableAnalysisModules().contains(name)){
            scriptTypeExtension = ".AnalysisModule";
        }
        for(String filePath : this.results){
            if(Manager.fileExists(Places.finalResultsDir.getAbsolutePath() + File.separator
                    + this.name + scriptTypeExtension + File.separator + filePath) == false){
                notExist.add(filePath);
            }
        }
        return notExist;
    }
    
    public Set<String> filesAreInRequirements(AbstractCollection<String> toVerify){
        TreeSet<String> present = new TreeSet<String>();
        for(String filePath : this.requiredExternalFiles){
            for(String otherPath : toVerify){
                if(otherPath.contains(filePath)){
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
        Collection<File> subs = FileUtils.listFiles(resultsFolder,null,true);
        if(subs != null){
            new File(newFolder).mkdir();
            for(File sub : subs){
                if(sub.isFile()) {
                    String subLocation = sub.getAbsolutePath();
                    String destLocation = subLocation.replace(resultsFolder.getAbsolutePath(), newFolder);
                    File destination = new File(destLocation);
                    try {
                        FileUtils.copyFile(sub, destination);
                    } catch (Exception ex) {
                        Log.logger.severe("Failed copying " + sub.getAbsolutePath() + " to " + destination.getAbsolutePath());
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
    
    public void saveResults(){
        saveResultsAt(Places.finalResultsDir.getAbsolutePath() + File.separator
                        + this.name + "." + this.getClass().getSimpleName());
    }
    
    //uses requiredParameters to create the ConfigFile with the parameters
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
        if(this.getWorkingDirectory().exists() == false){
            return;
        }
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
        if(results.size() == 0){
            return true;
        }else{
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
    }
    
    public void cleanLocalResults(){
        if(this.resultsFolder.exists() == false){
            return;
        }
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
    
    public void cleanTempFiles(){
        if(this.getWorkingDirectory().exists() == false){
            return;
        }
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
    

    
    public void toBin(File file) throws IOException{
        peridot.Archiver.Persistence.saveObjectAsBin(file, this);
    }
    
    public String getScriptType(){
        if(this instanceof AnalysisModule){
            return "AnalysisModule";
        }else if(this instanceof PostAnalysisModule){
            return "PostAnalysisModule";
        }else{
            return "RModule";
        }
    }

    public Set<Package> requiredPackagesNotInstalled(){
        if(Interpreter.isDefaultInterpreterDefined()) {
            return requiredPackagesNotInstalled(Interpreter.defaultInterpreter);
        }else{
            return requiredPackages;
        }
    }

    public Set<Package> requiredPackagesNotInstalled(Interpreter interpreter){
        if(interpreter != null) {
            Set<Package> installed = interpreter.availablePackages;
            Set<String> installedNames = new TreeSet<>();
            Set<Package> notInstalledPacks = new HashSet<>();
            for(Package pack : installed){
                installedNames.add(pack.name);
            }
            for(Package pack : requiredPackages){
                if(installedNames.contains(pack.name) == false){
                    notInstalledPacks.add(pack);
                }
            }
            return notInstalledPacks;
        }else{
            return requiredPackages;
        }
    }

    public boolean requiredPackagesInstalled(){
        if(requiredPackages.size() == 0){
            return true;
        }else{
            return requiredPackagesNotInstalled().size() == 0;
        }
    }

    public boolean requiredPackagesInstalled(Interpreter interpreter){
        if(requiredPackages.size() == 0){
            return true;
        }else{
            int n = requiredPackagesNotInstalled(interpreter).size();
            if(this.needsAllDependencies){
                return n == 0;
            }else{
                return n < requiredPackages.size();
            }
        }
        
    }
}
