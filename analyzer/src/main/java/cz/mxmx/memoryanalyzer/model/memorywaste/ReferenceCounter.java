package cz.mxmx.memoryanalyzer.model.memorywaste;

public class ReferenceCounter {
    private final long referencesCount;
    private final long nullReferencesCount;
    private final long instanceId;

    public ReferenceCounter(long referencesCount, long nullReferencesCount, long instanceId) {
        this.referencesCount = referencesCount;
        this.nullReferencesCount = nullReferencesCount;
        this.instanceId = instanceId;
    }

    public long getReferencesCount() {
        return referencesCount;
    }

    public long getNullReferencesCount() {
        return nullReferencesCount;
    }

    public long getInstanceId() {
        return instanceId;
    }
}
