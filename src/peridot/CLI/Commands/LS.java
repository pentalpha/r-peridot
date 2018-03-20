package peridot.CLI.Commands;

import peridot.CLI.Command;
import peridot.CLI.PeridotCmd;

/**
 * Command to list all available modules, or the details of a specific modules.
 * Created by pentalpha on 4/28/17.
 */
public class LS extends Command {
    public LS(String[] args){
        super(args);
    }

    public void defineCmdNameAndDetails(){
        this.commandStr = "ls";
        this.detail = "R-Peridot's Command: ls\n" +
                "Options:\n" +
                "\tls\t\t\tList all of the R-Peridot modules currently available\n" +
                "\tls [module-name]\tDisplay all details about a module\n" +
                "\tls -h\t\t\tDisplays this help message\n" +
                "\tls --help\t\tDisplays this help message\n";
    }

    public void evaluateArgs() throws CmdParseException{
        if(args.length > 1){
            fail("'r-peridot ls' takes only zero or one arguments.");
        }else{
            if(args.length == 1) {
                if (isHelpArg(args[0]) == false && isAModule(args[0]) == false) {
                    fail("Not a module name or --help: " + args[0]);
                }
            }
        }
    }

    public void run(){
        if(args.length == 0){
            PeridotCmd.listModules();
        }else if(args.length == 1){
            if(isHelpArg(args[0])){
                this.printDetails();
            }else if(isAModule(args[0])){
                PeridotCmd.scriptDetails(args[0]);
            }
        }
    }
}
