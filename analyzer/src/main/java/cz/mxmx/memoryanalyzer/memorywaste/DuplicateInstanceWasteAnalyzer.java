package cz.mxmx.memoryanalyzer.memorywaste;

import com.google.common.collect.Lists;
import cz.mxmx.memoryanalyzer.cache.DefaultInstanceComparisonCache;
import cz.mxmx.memoryanalyzer.cache.InstanceComparisonCache;
import cz.mxmx.memoryanalyzer.model.ClassDump;
import cz.mxmx.memoryanalyzer.model.InstanceDump;
import cz.mxmx.memoryanalyzer.model.InstanceFieldDump;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import cz.mxmx.memoryanalyzer.model.memorywaste.DuplicateInstanceWaste;
import cz.mxmx.memoryanalyzer.model.memorywaste.Waste;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Analyzer to find duplicate instances of objects.
 */
public class DuplicateInstanceWasteAnalyzer implements WasteAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(DuplicateInstanceWasteAnalyzer.class);
    private final Map<Long, Set<Long>> currentlyComparing = new HashMap<>();
    private final InstanceComparisonCache cache;
    private boolean cacheResult = true;

    public DuplicateInstanceWasteAnalyzer() {
        cache = new DefaultInstanceComparisonCache();
    }

    public DuplicateInstanceWasteAnalyzer(InstanceComparisonCache cache) {
        this.cache = cache;
    }

    @Override
    public List<Waste> findMemoryWaste(MemoryDump memoryDump) {
        log.info("Instance waste analysis.");
        List<Waste> wasteList = new ArrayList<>();

        List<InstanceDump> instances = new ArrayList<>(memoryDump.getUserInstances().values());
        int onePercent = instances.size() / 100;
        log.info("Starting duplicate instance waste analyzer. Using deep equals.");

        for (int i = 0; i < instances.size() - 1; i++) {
            InstanceDump instance = instances.get(i);
            if (i % onePercent == 0) {
                log.info("Done {}%.", (i / ((double) instances.size() - 2)) * 100);
            }
            for (int j = i + 1; j < instances.size(); j++) {
                InstanceDump instanceToCompareWith = instances.get(j);

                if (this.instancesAreSame(instance, instanceToCompareWith)) {
                    this.mergeWasteList(wasteList, instance, instanceToCompareWith);
                }
            }
        }

        log.info("Finished duplicate instance waste analyses.");

        log.info("Cache statistics - hit: {}, miss: {}.", cache.getCacheHitCount(), cache.getCacheMissCount());

        return wasteList;
    }

    /**
     * Checks if the given instances are the same
     *
     * @param instance  Instance 1
     * @param instance2 Instance 2
     * @return True if the instances are the same, otherwise false.
     */
    public boolean instancesAreSame(InstanceDump instance, InstanceDump instance2) {
        currentlyComparing.clear();
        return deepEquals(instance, instance2, currentlyComparing);
//        return shallowEquals(instance, instance2);
    }

    /**
     * @param a instance dump a
     * @param b instance dumb b
     * @return true if objects have same references or string values.
     * @deprecated Deprecated version to compare objects. Can compare only references as long values and Strings.
     */
    private boolean shallowEquals(InstanceDump a, InstanceDump b) {
        if (!this.instancesOfSameClass(a, b)
                || a.getInstanceFieldValues().size() != b.getInstanceFieldValues().size()) {
            return false;
        }

        ClassDump classDump = a.getClassDump();

        for (InstanceFieldDump field : classDump.getInstanceFields()) {
            Object value = a.getInstanceFieldValues().get(field);
            Object value2 = b.getInstanceFieldValues().get(field);

            if (!value.equals(value2)) {
                return false;
            }
        }
        return classDump.getInstanceFields().size() > 0;
    }

    private boolean deepEquals(InstanceDump a, InstanceDump b, Map<Long, Set<Long>> currentlyComparing) {
        if (a.getInstanceId().equals(b.getInstanceId())) {
            return true;
        }

        if (!this.instancesOfSameClass(a, b)
                || a.getInstanceFieldValues().size() != b.getInstanceFieldValues().size()) {
            return false;
        }

        Optional<Boolean> cacheResult = getResultFromCache(a, b);
        if (cacheResult.isPresent()) {
            return cacheResult.get();
        }

        if (currentlyComparing.getOrDefault(a.getInstanceId(), Collections.emptySet()).contains(b.getInstanceId())) {
            this.cacheResult = false; // we cannot cache result based on this, could be false positive
            return true;
        } else {
            currentlyComparing.computeIfAbsent(a.getInstanceId(), k -> new HashSet<>()).add(b.getInstanceId());
        }

        ClassDump classDump = a.getClassDump();

        boolean instanceComparisonResult = true;
        for (InstanceFieldDump field : classDump.getInstanceFields()) {
            Object value = a.getInstanceFieldValues().get(field);
            Object value2 = b.getInstanceFieldValues().get(field);

            if (value instanceof InstanceDump && value2 instanceof InstanceDump) {
                if (!deepEquals((InstanceDump) value, (InstanceDump) value2, currentlyComparing)) {
                    instanceComparisonResult = false;
                    break;
                }
            } else if (value instanceof Value && value2 instanceof Value) {
                if (!Objects.equals(((Value<?>) value).value, ((Value<?>) value2).value)) {
                    instanceComparisonResult = false;
                    break;
                }
            } else if (!value.equals(value2)) {
                instanceComparisonResult = false;
                break;
            }
        }

        instanceComparisonResult = instanceComparisonResult && classDump.getInstanceFields().size() > 0;
        if (this.cacheResult) {
            cacheResult(a, b, instanceComparisonResult);
        } else {
            this.cacheResult = true;
        }

        return instanceComparisonResult;
    }

    private void cacheResult(InstanceDump a, InstanceDump b, boolean result) {
        cache.cacheComparisonResult(a, b, result);
    }

    private Optional<Boolean> getResultFromCache(InstanceDump a, InstanceDump b) {
        return cache.instancesEqual(a, b);
    }

    /**
     * Checks if the instances are of the same class.
     *
     * @param instance  Instance 1
     * @param instance2 Instance 2
     * @return True if t he instances are of the same class, otherwise false
     */
    private boolean instancesOfSameClass(InstanceDump instance, InstanceDump instance2) {
        ClassDump parent = instance.getClassDump();

        do {
            if (parent.equals(instance2.getClassDump())) {
                return true;
            }

            parent = parent.getSuperClassDump();
        } while (parent != null && parent.getName() != null && !parent.getName().equals(Object.class.getName()));

        parent = instance2.getClassDump();

        do {
            if (parent.equals(instance.getClassDump())) {
                return true;
            }

            parent = parent.getSuperClassDump();
        } while (parent != null && parent.getName() != null && !parent.getName().equals(Object.class.getName()));

        return false;
    }

    /**
     * Merges the results into the given list, if similar exist, otherwise creates a new entry.
     *
     * @param wasteList List with current results
     * @param instance  New instance 1
     * @param instance2 New instance 2
     */
    private void mergeWasteList(List<Waste> wasteList, InstanceDump instance, InstanceDump instance2) {
        Optional<Waste> optWaste = wasteList
                .stream()
                .filter(waste -> this.instancesAreSame(waste.getAffectedInstances().get(0), instance)).findFirst();

        if (optWaste.isPresent()) {
            if (!optWaste.get().getAffectedInstances().contains(instance)) {
                optWaste.get().addAffectedInstance(instance);
            }

            if (!optWaste.get().getAffectedInstances().contains(instance2)) {
                optWaste.get().addAffectedInstance(instance2);
            }
        } else {
            wasteList.add(new DuplicateInstanceWaste(this, Lists.newArrayList(instance, instance2)));
        }
    }
}
