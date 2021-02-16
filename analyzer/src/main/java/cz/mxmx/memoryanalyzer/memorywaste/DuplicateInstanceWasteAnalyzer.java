package cz.mxmx.memoryanalyzer.memorywaste;

import cz.mxmx.memoryanalyzer.cache.DefaultInstanceCacheWithStatistics;
import cz.mxmx.memoryanalyzer.cache.DummyInstanceComparisonCache;
import cz.mxmx.memoryanalyzer.cache.InstanceComparisonCache;
import cz.mxmx.memoryanalyzer.cache.InstanceComparisonCacheWithStatistics;
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
    private final Set<InstancesIds> currentlyComparing = new HashSet<>();
    private final InstanceComparisonCacheWithStatistics cache;
    private boolean cacheResult = true;

    public DuplicateInstanceWasteAnalyzer() {
        this.cache = new DefaultInstanceCacheWithStatistics(new DummyInstanceComparisonCache());
    }

    public DuplicateInstanceWasteAnalyzer(InstanceComparisonCache cache) {
        this.cache = new DefaultInstanceCacheWithStatistics(cache);
    }

    @Override
    public List<Waste> findMemoryWaste(MemoryDump memoryDump) {
        log.info("Instance waste analysis.");
        List<Waste> wasteList = new ArrayList<>();

        List<InstanceDump> instances = new ArrayList<>(memoryDump.getUserInstances().values());
        int total = instances.size();
        int onePercent = total / 100;
        log.info("Starting duplicate instance waste analyzer. Using deep equals.");
        long doneCount = 0;
        List<InstanceDump> duplicates = new ArrayList<>();

        while (instances.size() > 0) {
            duplicates.clear();
            InstanceDump instance = instances.remove(instances.size() - 1);

            for (InstanceDump compareWith : instances) {
                if (this.instancesAreSame(instance, compareWith)) {
                    duplicates.add(compareWith);
                }
            }
            doneCount++;

            if (duplicates.size() > 0) {
                doneCount += duplicates.size();
                duplicates.add(instance);
                instances.removeAll(duplicates);
                wasteList.add(new DuplicateInstanceWaste(this, new ArrayList<>(duplicates)));
            }

            if (doneCount % onePercent == 0) {
                log.info("Done {}%.", (doneCount / ((double) total - 2)) * 100);
            }
        }

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
    }

    private boolean deepEquals(InstanceDump a, InstanceDump b, Set<InstancesIds> currentlyComparing) {
        if (a.getInstanceId().equals(b.getInstanceId())) {
            return true;
        }

        if (a.getInstanceFieldValues().size() != b.getInstanceFieldValues().size() || !this.instancesOfSameClass(a, b)) {
            return false;
        }

        Optional<Boolean> cacheResult = getResultFromCache(a, b);
        if (cacheResult.isPresent()) {
            return cacheResult.get();
        }

        InstancesIds ids = new InstancesIds(a.getInstanceId(), b.getInstanceId());
        if (currentlyComparing.contains(ids)) {
            this.cacheResult = false; // we cannot cache result based on this, could be false positive
            return true;
        } else {
            currentlyComparing.add(ids);
        }

        ClassDump classDump = a.getClassDump();

        boolean instancesSame = true;
        for (InstanceFieldDump<?> field : classDump.getInstanceFields()) {
            Object value = a.getInstanceFieldValues().get(field);
            Object value2 = b.getInstanceFieldValues().get(field);

            if (value instanceof InstanceDump && value2 instanceof InstanceDump) {
                instancesSame = deepEquals((InstanceDump) value, (InstanceDump) value2, currentlyComparing);
            } else if (value instanceof Value && value2 instanceof Value) {
                instancesSame = Objects.equals(((Value<?>) value).value, ((Value<?>) value2).value);
            } else {
                instancesSame = value.equals(value2);
            }

            if (!instancesSame) {
                break;
            }
        }

        instancesSame = instancesSame && classDump.getInstanceFields().size() > 0;
        if (this.cacheResult) {
            cacheResult(a, b, instancesSame);
        } else {
            this.cacheResult = true;
        }

        return instancesSame;
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

    private static class InstancesIds {
        Long idFirst;
        Long idSecond;

        public InstancesIds(Long idFirst, Long idSecond) {
            this.idFirst = idFirst;
            this.idSecond = idSecond;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InstancesIds that = (InstancesIds) o;
            return Objects.equals(idFirst, that.idFirst) && Objects.equals(idSecond, that.idSecond);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idFirst, idSecond);
        }
    }
}
