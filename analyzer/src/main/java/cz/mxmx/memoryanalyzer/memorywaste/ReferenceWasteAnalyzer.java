package cz.mxmx.memoryanalyzer.memorywaste;

import cz.mxmx.memoryanalyzer.model.ClassDump;
import cz.mxmx.memoryanalyzer.model.InstanceDump;
import cz.mxmx.memoryanalyzer.model.InstanceFieldDump;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import cz.mxmx.memoryanalyzer.model.memorywaste.ReferencesWaste;
import cz.mxmx.memoryanalyzer.model.memorywaste.Waste;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Type;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Value;

import java.util.*;

public class ReferenceWasteAnalyzer implements WasteAnalyzer {

    @Override
    public List<Waste> findMemoryWaste(MemoryDump memoryDump) {
        Map<Long, InstanceDump> userInstances = memoryDump.getUserInstances();
        long referencesCount = 0;
        long nullReferencesCount = 0;

        for (InstanceDump instance : userInstances.values()) {
            ReferenceCounter references = countReferences(instance);
            referencesCount += references.getReferencesCount();
            nullReferencesCount += references.getNullReferencesCount();
        }

        return Collections.singletonList(new ReferencesWaste(this, referencesCount, nullReferencesCount));
    }

    private ReferenceCounter countReferences(InstanceDump instance) {
        ClassDump classDump = instance.getClassDump();
        long referencesCount = 0;
        long nullReferencesCount = 0;

        for (InstanceFieldDump field : classDump.getInstanceFields()) {
            Object value = instance.getInstanceFieldValues().get(field);

            if (value instanceof InstanceDump || value instanceof String) {
                referencesCount++;
            }

            if (value instanceof Value && ((Value<?>) value).type.equals(Type.OBJ) && ((Value<?>) value).value instanceof Long) {
                Long reference = (Long) ((Value<?>) value).value;
                referencesCount++;
                if (0 == reference) {
                    nullReferencesCount++;
                }
            }
        }

        return new ReferenceCounter(referencesCount, nullReferencesCount, instance.getInstanceId());
    }

    private static class ReferenceCounter {
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
}
