package cz.mxmx.memoryanalyzer.app.shell;

public abstract class GenericCommand {
    public abstract void accept(CommandsVisitor visitor);
}
