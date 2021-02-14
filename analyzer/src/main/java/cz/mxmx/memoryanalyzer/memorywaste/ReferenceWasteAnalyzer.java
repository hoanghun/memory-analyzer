package cz.mxmx.memoryanalyzer.memorywaste;

import cz.mxmx.memoryanalyzer.model.ClassDump;
import cz.mxmx.memoryanalyzer.model.InstanceDump;
import cz.mxmx.memoryanalyzer.model.InstanceFieldDump;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import cz.mxmx.memoryanalyzer.model.memorywaste.ReferenceCounter;
import cz.mxmx.memoryanalyzer.model.memorywaste.ReferencesWaste;
import cz.mxmx.memoryanalyzer.model.memorywaste.Waste;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Type;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReferenceWasteAnalyzer implements WasteAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(ReferenceWasteAnalyzer.class);

    @Override
    public List<Waste> findMemoryWaste(MemoryDump memoryDump) {
        log.info("Starting reference waste analysis");

        Map<Long, InstanceDump> userInstances = memoryDump.getUserInstances();
        long referencesCount = 0;
        long nullReferencesCount = 0;

        for (InstanceDump instance : userInstances.values()) {
            ReferenceCounter references = countReferences(instance);
            referencesCount += references.getReferencesCount();
            nullReferencesCount += references.getNullReferencesCount();
        }

        log.info("Finished reference waste analysis");

        return Collections.singletonList(new ReferencesWaste(this, referencesCount, nullReferencesCount));
    }

    /**
     * Calculates total count of references and null references on single instance. Always NullReferencesCount <= ReferencesCount
     * @param instance instance dump to count fields for
     * @return dto with values for references counts
     */
    public ReferenceCounter countReferences(InstanceDump instance) {
        ClassDump classDump = instance.getClassDump();
        long referencesCount = 0;
        long nullReferencesCount = 0;

        for (InstanceFieldDump<?> field : classDump.getInstanceFields()) {
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

}
