package peridot.CLI.Commands;

import peridot.CLI.Command;
import peridot.Operations;
import peridot.script.RModule;

/**
 * Command to export, to a file, an R-Peridot Module.
 * Created by pentalpha on 4/28/17.
 */
public class OUT extends Command {
    public OUT(String[] args){
        super(args);
    }

    public void defineCmdNameAndDetails(){
        this.commandStr = "out";
        this.detail = "R-Peridot's Command: out\n" +
                "Options:\n" +
                "\tout [module-name] [directory]\tExport a module to [directory]/[module-name]." + RModule.binExtension + "\n" +
                "\tout -h\t\t\t\tDisplays this help message\n" +
                "\tout --help\t\t\tDisplays this help message\n";
    }

    public void evaluateArgs() throws Command.CmdParseException {
        if(args.length != 1 && args.length != 2){
            fail("Invalid, 'r-peridot out [args]' takes one or two arguments.");
        }else if(args.length == 1){
            if(isHelpArg(args[0]) == false){
                fail("Invalid argument: " + args[0]);
            }
        }else if(args.length == 2){
            if(isAModule(args[0]) == false){
                fail(args[0] + " is not an existent module.");
            }
        }
    }

    public void run(){
        if(isHelpArg(args[0])){
            this.printDetails();
        }else{
            Operations.exportModule(args[0], args[1]);
        }
    }
}
