package cz.mxmx.memoryanalyzer.app.shell;

public interface CommandsVisitor {
    void visitIdsCommand(IdsCommand idsCommand);
}
