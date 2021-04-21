package cz.mxmx.memoryanalyzer.app.shell;

public enum Commands {
    IDS("ids");

    private final String command;

    Commands(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
