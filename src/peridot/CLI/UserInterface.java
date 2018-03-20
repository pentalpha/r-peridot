package peridot.CLI;

import peridot.CLI.Commands.*;
import peridot.Global;
import peridot.script.RModule;

/**
 * The main class for the R-Peridot CLI.
 * Created by pentalpha on 4/27/17.
 */
public class UserInterface {

    public UserInterface(String[] args){
        args = Global.joinArgsBetweenQuotes(args);
        String command = args[0];
        String[] theRest = new String[args.length-1];
        Command cmd;
        for(int i = 1; i < args.length; i++){
            theRest[i-1] = args[i];
        }
        if(command.equals("-h") || command.equals("--help")){
            printMajorHelp();
        }else if(command.equals("ls")){
            cmd = new LS(theRest);
        }else if(command.equals("in")){
            cmd = new IN(theRest);
        }else if(command.equals("out")){
            cmd = new OUT(theRest);
        }else if(command.equals("run")){
            cmd = new RUN(theRest);
        }else if(command.equals("r")){
            cmd = new R(theRest);
        }else{
            printInvalidCommand(command);
        }
    }

    public static void printNoCommand(){
        System.out.println("No command given.\n");
        printMajorHelp();
    }

    public static void printInvalidCommand(String cmd){
        System.out.println("Invalid command: " + cmd + "\n");
        printMajorHelp();
    }

    public static void printMajorHelp(){
        System.out.println("R-Peridot Commands:\n" +
                "\tCommand:\t\tDescription:\n" +
                "\tin [args]\t\tImport a module from *." + RModule.binExtension + " file\n" +
                "\tout [args]\t\tExport a module to a *." + RModule.binExtension + " file\n" +
                "\tls [args]\t\tList modules or read module details\n" +
                "\trun [args]\t\tMake an analysis defined by an .af file.\n" +
                "\tr [args]\t\tList, add, remove or update R environments for R-Peridot.\n" +
                "\t\nAdd -h or --help to any command (r-peridot [command] -h) for more detailed information.\n");
    }
}

