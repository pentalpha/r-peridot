package peridot.script.r;

import peridot.Global;

import java.util.*;

/**
 * Created by pentalpha on 26/01/2018.
 */
public class RInfoParser {
    public VersionNumber rVersion;
    public Set<Package> packages;
    private HashMap<String, List<String>> commands;

    public RInfoParser(String info){
        this.rVersion = null;
        this.packages = null;

        splitInfo(info);
        readLines();
    }

    public boolean validOutput(){
        return rVersion != null && this.packages != null;
    }

    private void splitInfo(String info){
        String[] infoLines = info.split(System.lineSeparator());

        String currentCommand = null;
        List<String> commandOutput = null;
        commands = new HashMap<>();
        for(int i = 0; i < infoLines.length; i++){
            if(infoLines[i].length() > 0){
                //String[] splice = Global.spliceBySpacesAndTabs(infoLines[i]);
                if(infoLines[i].charAt(0) == '>'){
                    if(currentCommand != null){
                        commands.put(currentCommand, commandOutput);
                    }
                    currentCommand = infoLines[i];
                    commandOutput = new ArrayList<>();
                }else{
                    commandOutput.add(infoLines[i]);
                }
            }

        }
    }

    private void readLines(){
        List<String> versionOutput = commands.get("> R.Version()");
        readVersion(versionOutput);
        List<String> packsTable = commands.get("> Packages");
        readPackages(packsTable);
    }

    private void readVersion(List<String> versionOutput){
        String versionLine = null;
        for(int i = 0; i < versionOutput.size(); i++){
            if(versionOutput.get(i).contains("$version.string")){
                versionLine = versionOutput.get(i+1);
            }
        }
        if(versionLine != null){
            String[] splice = Global.spliceBySpacesAndTabs(versionLine);
            String number = splice[3];
            VersionNumber version = new VersionNumber(number);
            this.rVersion = version;
        }
    }

    private void readPackages(List<String> packsTable){
        Set<Package> packs = new HashSet<>();
        for(int i = 1; i < packsTable.size(); i++){
            String[] splice = Global.spliceBySpacesAndTabs(packsTable.get(i));
            if(splice.length == 3){
                Package pack = new Package(splice[1], splice[2]);
                packs.add(pack);
            }
        }
        if(packs.size() > 0){
            this.packages = packs;
        }else{
            this.packages = null;
        }
    }
}
