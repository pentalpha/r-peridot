/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import peridot.AnalysisParameters;
import peridot.Archiver.Manager;
import peridot.Archiver.Places;
import peridot.CLI.AnalysisFileParser;
import peridot.Log;
import peridot.script.r.Interpreter;
import peridot.script.r.Package;
import peridot.script.AbstractModule;
import java.io.*;
import java.util.*;
import java.util.logging.Level;

/**
 *
 * @author pentalpha
 */
public class RModule extends AbstractModule implements Serializable{

    //@Override
    public boolean equals(RModule a, RModule b){
        return a.name.equals(b.name);
    }
    public static String extension = "GenericModule";
    //Nome dos parametros necessarios e suas respectivas classes
    //O script não irá executar se não forem todos passados antes
    public Map<String, Class> requiredParameters = null;
    public Set<Package> requiredPackages = null;
    public Set<RModule> children = null;

    public Map<String, Object> parameters = null;
    //O caminho dos arquivos externos necessarios para a execução interna do script
    //O script não irá executar se não forem todos passados antes
    public Set<String> requiredExternalFiles = null;
    public Set<String> requiredScripts = null;
    //Nomes dos arquivos de resultados que o script irá gerar e colocar na sua
    //respectiva pasta /temp.
    public Set<String> results = null;
    public Set<String> mandatoryResults = null;
    public String scriptName = null;
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
        
        parseJson(dir);
        
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

        //this.createJson();
    }

    private void parseJson(File dir) throws Exception{
        File descriptionFile = new File(dir.getAbsolutePath() + File.separator
                + AbstractModule.moduleFileName);
        if(descriptionFile.exists() == false){
            throw new Exception("Module description file not found in "
                    + descriptionFile.getAbsolutePath());
        }

        String jsonContent = Manager.fileToString(descriptionFile).toString();
        JSONObject json = new JSONObject(jsonContent);

        this.name = json.getString("NAME");
        this.scriptName = json.getString("SCRIPT-NAME");
        String scriptFileName = dir.getAbsolutePath() + File.separator + this.scriptName;
        if(!(new File(scriptFileName).exists())){
            throw new Exception("The following script file does not exist:\n" + scriptFileName);
        }
        this.max2Conditions = json.getBoolean("MAX-2-CONDITIONS");
        this.needsReplicates = json.getBoolean("NEEDS-REPLICATES");

        if(json.keySet().contains("REQUIRES-ALL-DEPENDENCIES")){
            this.needsAllDependencies = json.getBoolean("REQUIRES-ALL-DEPENDENCIES");
        }
        if(json.keySet().contains("INFO")){
            this.info = json.getString("INFO");
        }
        if(json.keySet().contains("RESULTS")){
            List<String> resultsList = Arrays.asList(json.getJSONArray("RESULTS").toList().toArray(new String[0]));
            results.addAll(resultsList);
        }
        if(json.keySet().contains("MANDATORY-RESULTS")){
            List<String> resultsList = Arrays.asList(json.getJSONArray("MANDATORY-RESULTS").toList().toArray(new String[0]));
            results.addAll(resultsList);
            mandatoryResults.addAll(resultsList);
        }
        if(json.keySet().contains("REQUIRED-INPUT-FILES")){
            requiredExternalFiles.addAll(Arrays.asList(json.getJSONArray("REQUIRED-INPUT-FILES").toList().toArray(new String[0])));
        }
        if(json.keySet().contains("REQUIRED-SCRIPTS")){
            requiredScripts.addAll(Arrays.asList(json.getJSONArray("REQUIRED-SCRIPTS").toList().toArray(new String[0])));
        }

        if(json.keySet().contains("PACKAGES")) {
            JSONObject packagesJson = json.getJSONObject("PACKAGES");
            for (String packageName : packagesJson.keySet()) {
                Package pack = new Package(packageName, packagesJson.getString(packageName));
                this.requiredPackages.add(pack);
            }
        }
        if(json.keySet().contains("REQUIRED-PARAMETERS")) {
            JSONObject paramsJson = json.getJSONObject("REQUIRED-PARAMETERS");
            for (String paramName : paramsJson.keySet()) {
                String paramType = paramsJson.getString(paramName);
                if(AnalysisParameters.availableParamTypes.keySet().contains(paramType)){
                    this.requiredParameters.put(paramName, AnalysisParameters.availableParamTypes.get(paramType));
                }
                else{
                    throw new Exception("Unknown parameter type: " + paramType);
                }
            }
        }
    }



    public File getDescriptionFile(){
        return new File(getWorkingDirectoryPath() + File.separator + AbstractModule.moduleFileName);
    }

    public void createJson(){
        String path = getWorkingDirectoryPath() + File.separator + AbstractModule.moduleFileName;
        JSONObject json = new JSONObject();

        json.put("NAME", this.name);
        json.put("SCRIPT-NAME", this.scriptName);
        json.put("MAX-2-CONDITIONS", this.max2Conditions);
        json.put("NEEDS-REPLICATES", this.needsReplicates);
        json.put("REQUIRES-ALL-DEPENDENCIES", this.needsAllDependencies);
        json.put("INFO", this.info);
        JSONArray resultsArray = new JSONArray();
        for(String result : this.results){
            if(!mandatoryResults.contains(result)){
                resultsArray.put(result);
            }
        }
        json.put("MANDATORY-RESULTS", mandatoryResults);
        json.put("RESULTS", resultsArray);
        JSONArray requiredExternalFilesArray = new JSONArray(requiredExternalFiles);
        json.put("REQUIRED-INPUT-FILES", requiredExternalFilesArray);
        JSONArray requiredScriptsArray = new JSONArray(requiredScripts);
        json.put("REQUIRED-SCRIPTS", requiredScriptsArray);
        JSONObject requiredParametersJson = new JSONObject();
        for(Map.Entry<String, Class> pair : this.requiredParameters.entrySet()){
            String className = pair.getValue().getSimpleName();
            requiredParametersJson.put(pair.getKey(),className);
        }
        json.put("REQUIRED-PARAMETERS", requiredParametersJson);
        JSONObject requiredPackagesJson = new JSONObject();
        for(Package pack : this.requiredPackages){
            requiredPackagesJson.put(pack.name, pack.version.toString());
        }
        json.put("PACKAGES", requiredPackagesJson);

        Manager.stringToFile(path,json.toString(4));
    }
    
    /**
     * Cria o diretorio (workingdirectory) onde serão guardados o script, os resultados e etc.
     * @param newScriptFilePath External script to be copied to environment. Can be null if its a internalScript.
     */
    public void createEnvironment(String newScriptFilePath) throws IOException{
        Log.logger.info("trying to create " + getWorkingDirectoryPath());
        FileUtils.deleteDirectory(getWorkingDirectory());
        this.getWorkingDirectory().mkdirs();
        this.getWorkingDirectory().mkdir();
        if(!getWorkingDirectory().exists()){
            throw new IOException("could not create " + getWorkingDirectory().getName());
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
        createJson();
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
    
    public void setChildren(){
        Log.logger.finest("Looking for children of " + this.name);
        this.children = new HashSet<>();
        for (String module_name : RModule.availableModules.keySet()){
            RModule possible_children = RModule.availableModules.get(module_name);
            if(possible_children.requiredScripts.contains(this.name)){
                children.add(possible_children);
                Log.logger.finest(possible_children.name + " is a children of " + this.name);
            }
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
        return verifyResults(this.resultsFolder);
    }
    
    public boolean verifyResults(File resultsDir){
        if(results.size() == 0){
            return true;
        }else{
            int nSuccessful = 0;
            for(String resultName : results){
                if(new File(resultsDir + File.separator + resultName)
                        .exists() == false){
                    Log.logger.warning("The expected result does not exists: " 
                            + resultsDir + File.separator + resultName);
                    
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
                    || file.getName().equals(AbstractModule.moduleFileName)
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

    public boolean runnableWith(Set<String> modules_set){
        int n = requiredScripts.size();
        for (String scriptName : requiredScripts){
            if (modules_set.contains(scriptName)){
                if(needsAllDependencies){
                    n -= 1;
                }else{
                    return true;
                }
            }
        }
        return n == 0;
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
