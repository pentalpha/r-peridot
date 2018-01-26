package peridot.script.r;

import org.apache.commons.lang3.tuple.Pair;
import peridot.Archiver.PeridotConfig;
import peridot.Archiver.Places;
import peridot.script.RModule;

import java.io.File;
import java.util.*;
import static java.util.Collections.reverseOrder;
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

    public static Interpreter loadDefaultInterpreter(){
        if(PeridotConfig.get().defaultInterpreter != null){
            for(Interpreter interpreter : interpreters){
                if(interpreter.exe.equals(PeridotConfig.get().defaultInterpreter)){
                    return interpreter;
                }
            }
        }
        return null;
    }

    public static List<Interpreter> getAvailableInterpreters(){
        Set<String> execs = PeridotConfig.get().availableInterpreters;
        List<Interpreter> interpreters = new ArrayList<>();
        for(String exec : execs){
            Interpreter interpreter = new Interpreter(exec);
            if(interpreter.validInterpreter){
                interpreters.add(interpreter);
            }
        }

        Comparator<Interpreter> comparator = reverseOrder(comparing(Interpreter::getRVersion));
        Collections.sort(interpreters, comparator);
        return interpreters;
    }

    public static boolean addInterpreter(String exec){
        Interpreter toRemove = null;
        for(Interpreter interp : interpreters){
            if(interp.exe.equals(exec)){
                toRemove = interp;
            }
        }

        Interpreter interpreter = new Interpreter(exec);
        if(interpreter.validInterpreter){
            if(toRemove != null){
                interpreters.remove(toRemove);
            }
            interpreters.add(interpreter);
        }

        return interpreter.validInterpreter;
    }

    public static boolean removeInterpreter(int i){
        if(i >= 0 && i < interpreters.size()){
            return interpreters.remove(i) != null;
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

    //Non-static values
    public Set<Package> availablePackages;

    public String exe;

    public boolean validInterpreter = true;

    private float value = 0.0f;

    private VersionNumber rVersion;

    public Interpreter(String exe){
        this.exe = exe;

        String testOutput = null;

        /*if(!this.exe.exists()){
            validInterpreter = false;
            return;
        }

        if(!this.exe.isFile()){
            validInterpreter = false;
            return;
        }*/

        try{
            testOutput = readPackagesAvailable();
        }catch (Exception exp){
            validInterpreter = false;
        }
        if(validInterpreter){
            //if(testOutput.contains("> Packages\n")){
            validInterpreter = readPackagesFromOutput(testOutput);
        }
    }

    String readPackagesAvailable() throws Exception{
        Script readPackagesScript = new Script(Places.readPackagesScript);
        readPackagesScript.run(this);
        return readPackagesScript.waitForOutput(5);
    }

    boolean readPackagesFromOutput(String output){
        RInfoParser parser = new RInfoParser(output);
        if(parser.validOutput()){
            this.rVersion = parser.rVersion;
            this.availablePackages = parser.packages;
            this.evaluate();
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
        Set<Package> installed = new TreeSet<>();
        for(Package pack : this.availablePackages){
            if(required.keySet().contains(pack.name)){
                installed.add(required.get(pack.name));
            }
        }
        Set<Package> notInstalledPacks = new TreeSet<>();
        notInstalledPacks.addAll(RModule.requiredPackages());
        notInstalledPacks.removeAll(installed);
        return notInstalledPacks;
    }

    @Override
    public String toString(){

        String str = this.exe + ":\n" +
                        "\tVersion: " + this.rVersion.toString() +
                        "\tScore: " + String.format("%.2f", value*10) +
                        "\n";
        Set<Package> toInstall = getPackagesToInstall();
        for(Package pack : toInstall){
            str += "\t\t"+pack.name+"\t"+pack.version.toString()+"\n";
        }

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

        //compare with the minimal and preferred R versions
        if(this.rVersion.compareTo(PeridotConfig.minimalRVersion) < 0){
            //R is too old
            value = value * 0.25f;
        }else{
            int comparison = this.rVersion.compareTo(PeridotConfig.preferredRVersion);
            if(comparison > 0){
                //newer than expected, not bad, but can come with unexpected behaviour
                value = value * 0.9f;
            }else if(comparison < 0){
                //R is old, but may still work
                value = value * 0.75f;
            }
        }
        this.value = value;
        return value;
    }

    public boolean update(){
        return false;
    }
}
