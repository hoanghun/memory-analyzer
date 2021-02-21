package cz.mxmx.memoryanalyzer.process;

import cz.mxmx.memoryanalyzer.model.ClassDump;
import cz.mxmx.memoryanalyzer.model.InstanceDump;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import cz.mxmx.memoryanalyzer.model.ProcessedMemoryDump;
import cz.mxmx.memoryanalyzer.model.raw.RawMemoryDump;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filters the instances and classes via specified namespaces.
 */
public class FilteredMemoryDumpProcessor implements MemoryDumpProcessor {

    /**
     * Cache to stop memory dumps from re-processing.
     */
    private final Map<RawMemoryDump, MemoryDump> cache = new HashMap<>();

    /**
     * Generic memory dump to get raw data.
     */
    private final MemoryDumpProcessor genericMemoryDumpProcessor;

    /**
     * Namespaces to use as a filter.
     */
    private final Collection<String> namespaces;

    private final boolean excludeNamespace;

    /**
     * Creates user-specific instances and classes processor.
     *
     * @param genericMemoryDumpProcessor Generic processor which handles pre-processing.
     * @param namespaces                 Namespaces to use as a filter.
     */
    public FilteredMemoryDumpProcessor(MemoryDumpProcessor genericMemoryDumpProcessor, Collection<String> namespaces, boolean excludeNamespace) {
        this.genericMemoryDumpProcessor = genericMemoryDumpProcessor;
        this.namespaces = namespaces;
        this.excludeNamespace = excludeNamespace;
    }

    @Override
    public MemoryDump process(RawMemoryDump rawMemoryDump) {
        if (this.cache.containsKey(rawMemoryDump)) {
            return this.cache.get(rawMemoryDump);
        }

        MemoryDump preprocessed = this.genericMemoryDumpProcessor.process(rawMemoryDump);
        Map<Long, ClassDump> userClasses = this.getUserClasses(preprocessed.getClasses());
        Map<Long, InstanceDump> userInstances = this.getUserInstances(preprocessed.getInstances(), userClasses);

        ProcessedMemoryDump processedMemoryDump = new ProcessedMemoryDump()
                .setUserNamespaces(this.namespaces)
                .setDumpHeader(preprocessed.getDumpHeader())
                .setInstances(preprocessed.getInstances())
                .setClasses(preprocessed.getClasses())
                .setUserInstances(userInstances)
                .setUserClasses(userClasses)
                .setPrimitiveArrays(preprocessed.getPrimitiveArrays())
                .setInstanceArrays(preprocessed.getInstanceArrays());

        this.cache.put(rawMemoryDump, processedMemoryDump);
        return processedMemoryDump;
    }

    /**
     * Filters instances by the namespaces.
     *
     * @param instances   Instances to filter.
     * @param userClasses User classes.
     * @return Filtered instances.
     */
    protected Map<Long, InstanceDump> getUserInstances(Map<Long, InstanceDump> instances, Map<Long, ClassDump> userClasses) {
        return instances.entrySet().stream()
                .filter(it -> userClasses.containsKey(it.getValue().getClassDump().getClassId()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Filters classes by the namespaces.
     *
     * @param classes Classes to filter.
     * @return Filtered classes.
     */
    protected Map<Long, ClassDump> getUserClasses(Map<Long, ClassDump> classes) {
        return classes.entrySet().stream()
                .filter(it -> namespaces.stream()
                        .anyMatch(namespace -> excludeNamespace != it.getValue().getName().matches(namespace)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
