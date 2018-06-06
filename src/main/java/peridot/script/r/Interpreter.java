package peridot.script.r;

import peridot.Archiver.PeridotConfig;
import peridot.Archiver.Places;
import peridot.Log;
import peridot.Output;
import peridot.script.RModule;

import java.io.File;
import java.util.*;

import static java.util.Comparator.comparing;

public class Interpreter {

    public static class InvalidExeException extends Exception{
        public InvalidExeException(){
            super();
        }
    }

    public static List<Interpreter> interpreters = null;

    public static Interpreter defaultInterpreter = null;

    public static boolean isDefaultInterpreterDefined(){
        return defaultInterpreter != null;
    }

    public static boolean interpretersAreLoaded(){
        return interpreters != null;
    }

    public static void loadDefaultInterpreter(){
        if(PeridotConfig.get().defaultInterpreter != null){
            for(Interpreter interpreter : interpreters){
                if(interpreter.exe.equals(PeridotConfig.get().defaultInterpreter)){
                    Interpreter.defaultInterpreter = interpreter;
                    //Log.logger.info(PeridotConfig.get().defaultInterpreter + "\nequals to\n"+interpreter.exe);
                    return;
                }else{
                    //Log.logger.info(PeridotConfig.get().defaultInterpreter + "\ndifferent from\n"+interpreter.exe);
                }
            }
        }
        Interpreter.defaultInterpreter = null;
    }

    public static void getAvailableInterpreters(){
        Set<String> execs = PeridotConfig.get().availableInterpreters;
        List<Interpreter> interpreters = new ArrayList<>();
        for(String exec : execs){
            Interpreter interpreter = new Interpreter(exec);
            if(interpreter.validInterpreter){
                interpreters.add(interpreter);
            }
        }

        Comparator<Interpreter> comparator = comparing(Interpreter::getRVersion).reversed();
        Collections.sort(interpreters, comparator);
        Interpreter.interpreters = interpreters;
    }

    public static boolean addInterpreter(String exec){
        Interpreter toRemove = null;
        if(interpreters != null){
            if(interpreters.size() > 0){
                for(Interpreter interp : interpreters){
                    if(interp.exe.equals(exec)){
                        toRemove = interp;
                    }
                }
            }
        }
        //System.out.println("Trying to add interpreter");
        Interpreter interpreter = new Interpreter(exec);
        if(interpreter.validInterpreter){
            if(toRemove != null){
                interpreters.remove(toRemove);
            }
            if(interpreters == null){
                interpreters = new ArrayList<>();
            }
            interpreters.add(interpreter);
        }

        return interpreter.validInterpreter;
    }

    public static boolean removeInterpreter(int i){
        if(i >= 0 && i < interpreters.size()){
            boolean removeDefault = false;
            {
                Interpreter toRm = interpreters.get(i);
                if(isDefaultInterpreterDefined()){
                    if(toRm.exe.equals(defaultInterpreter.exe)){
                        removeDefault = true;
                    }
                }
            }

            boolean removed = interpreters.remove(i) != null;

            if(removed){
                if(removeDefault){
                    defaultInterpreter = null;
                }
            }
            return removed;
        }else{
            return false;
        }
    }

    public static boolean setDefault(int i){
        if(i >= 0 && i < interpreters.size()){
            Interpreter.defaultInterpreter = interpreters.get(i);
            return true;
        }
        return false;
    }

    public static String getInterpretersStr(){
        String s = "";
        int i = 0;
        for(Interpreter interpreter : Interpreter.interpreters){
            i++;
            String str = "["+i+"] " + interpreter.toString() + "\n";
            if(Interpreter.isDefaultInterpreterDefined()){
                if(Interpreter.defaultInterpreter.exe.equals(interpreter.exe)){
                    str = "* " + str;
                }
            }
            s += str;
        }
        if(Interpreter.isDefaultInterpreterDefined()){
            s += "\n* = Default interpreter";
        }
        return s;
    }

    //Non-static values
    public Set<Package> availablePackages;

    public String exe;

    public boolean validInterpreter = true;

    private float value = 0.0f;

    private VersionNumber rVersion;

    public Interpreter(String exe){
        this.exe = exe;
        /*if(!this.exe.exists()){
            validInterpreter = false;
            return;
        }

        if(!this.exe.isFile()){
            validInterpreter = false;
            return;
        }*/
        analyseInterpreter();
    }

    public void analyseInterpreter(){
        Output testOutput = null;
        try{
            testOutput = readPackagesAvailable();
        }catch (Exception exp){
            //System.out.println("Exception thrown");
            //exp.printStackTrace();
            //System.out.println(testOutput.getText());
            validInterpreter = false;
        }
        if(validInterpreter){
            validInterpreter = readPackagesFromOutput(testOutput);
        }
    }

    private Output readPackagesAvailable() throws Exception{
        Script readPackagesScript = new Script(Places.readPackagesScript);
        readPackagesScript.run(this, true);
        return readPackagesScript.getOutputStream();
    }

    boolean readPackagesFromOutput(Output output){
        RInfoParser parser = new RInfoParser(output);
        if(parser.validOutput()){
            this.rVersion = parser.rVersion;
            this.availablePackages = parser.packages;
            this.evaluate();
        }else{
            System.out.println(output.getText());
        }
        return parser.validOutput();
    }

    public VersionNumber getRVersion(){
        return rVersion;
    }

    public String[] getLinuxEnvVars(){
        File exeFile = new File(exe);
        if(exeFile.exists()){
            String[] strings = {"R_HOME='" + exeFile.getParentFile().getAbsolutePath() + "'"};
            return strings;
        }else{
            String[] strings = {""};
            return strings;
        }
    }

    public Set<Package> getPackagesToInstall(){
        Map<String, Package> required = new TreeMap<>();
        for(Package pack : RModule.requiredPackages()){
            required.put(pack.name, pack);
        }
        //notInstalledPacks.addAll(RModule.requiredPackages());
        //Set<String> requiredNames //TODO
        Set<Package> installed = new HashSet<>();
        for(Package pack : this.availablePackages){
            if(required.keySet().contains(pack.name)){
                installed.add(required.get(pack.name));
            }
        }
        Set<Package> notInstalledPacks = new HashSet<>();
        notInstalledPacks.addAll(RModule.requiredPackages());
        notInstalledPacks.removeAll(installed);
        return notInstalledPacks;
    }

    public String titleString(){
        return this.exe + ":\n" +
                "\tVersion: " + this.rVersion.toString() +
                "\tScore: " + String.format("%.2f", value*10);
    }

    public String packagesToInstallString(){
        String str = "";
        Set<Package> toInstall = getPackagesToInstall();
        if(toInstall.size() > 0){
            str += "To Install:\n";
            for(Package pack : toInstall){
                str += "\t"+pack.name+"\t"+pack.version.toString()+"\n";
            }
        }

        Set<String> cannotRun = RModule.modulesWithUnmetDependencies(this);

        if(cannotRun.size() > 0){
            str += "Modules with unmet dependencies:\n\t";
            for(String module : cannotRun){
                str += module+"; ";
            }
        }

        return str;
    }

    @Override
    public String toString(){
        String str = titleString() + "\n";
        str += packagesToInstallString();

        return str;
    }

    private float evaluate(){
        Set<Package> requiredPackages = RModule.requiredPackages();
        Map<String, Package> packsMap = new HashMap<>();
        for(Package pack : requiredPackages){
            packsMap.put(pack.name, pack);
        }

        //compare the packages available in this interpreter with the ones required by the modules
        float nRequiredPackages = requiredPackages.size();
        float value = 0.0f;
        for(Package pack : this.availablePackages){
            if(packsMap.keySet().contains(pack.name)){
                int comparison = pack.version.compareTo(packsMap.get(pack.name).version);
                if(comparison >= 0){
                    if(comparison == 0){
                        value += 1.0;
                    }else{
                        value += 0.75;
                    }
                }else{
                    value += 0.5;
                }
            }
        }

        //normalize by the number of required packages
        value = value / nRequiredPackages;

        float rValue = 0.0f;
        //compare with the minimal and preferred R versions
        if(this.rVersion.compareTo(PeridotConfig.minimalRVersion) < 0){
            //R is too old
            rValue = 0;
        }else{
            int comparison = this.rVersion.compareTo(PeridotConfig.preferredRVersion);
            if(comparison > 0){
                //newer than expected, not bad, but can come with unexpected behaviour
                rValue = 0.9f;
            }else if(comparison < 0){
                //R is old, but may still work
                rValue = 0.5f;
            }else if(comparison == 0){
                rValue = 1.0f;
            }
        }
        this.value = value * 0.6f + rValue * 0.4f;
        return value;
    }

    public boolean update(){
        return false;
    }
}
