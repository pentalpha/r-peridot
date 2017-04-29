package peridot.CLI;


import peridot.script.RScript;

public abstract class Command {
    protected String commandStr;
    protected String detail;
    protected String[] args;

    public Command(String[] args){
        this.args = args;
        defineCmdNameAndDetails();
        assert(commandStr != null);
        assert(detail != null);
        try {
            evaluateArgs();
            run();
        } catch (CmdParseException ex){
            System.out.println("Error: " + ex.getMessage());
            System.out.println("For better use, read these instructions: ");
            this.printDetails();
        }
    }

    protected abstract void defineCmdNameAndDetails();

    protected abstract void evaluateArgs() throws CmdParseException;

    protected abstract void run();

    public static boolean isAModule(String name){
        return RScript.availableScripts.containsKey(name);
    }

    public static boolean isHelpArg(String arg){
        if(arg == null){
            return false;
        }
        return arg.equals("-h") || arg.equals("--help");
    }

    protected static void fail(String failedMessage) throws CmdParseException{
        throw new CmdParseException(failedMessage);
    }

    public void printDetails(){
        System.out.println(detail);
    }

    public static class CmdParseException extends Exception{
        public CmdParseException(String message){
            super(message);
        }
    }
}
