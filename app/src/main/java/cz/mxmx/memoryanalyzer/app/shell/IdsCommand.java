package cz.mxmx.memoryanalyzer.app.shell;

import java.util.List;

public class IdsCommand extends GenericCommand {

    private final List<Long> ids;

    public IdsCommand(List<Long> ids) {
        this.ids = ids;
    }

    @Override
    public void accept(CommandsVisitor visitor) {
        visitor.visitIdsCommand(this);
    }

    public List<Long> getIds() {
        return ids;
    }
}
