package cz.mxmx.memoryanalyzer.memorywaste;

import com.google.common.collect.Lists;
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
    private Map<Long, Set<Long>> currentlyComparing = new HashMap<>();

    @Override
    public List<Waste> findMemoryWaste(MemoryDump memoryDump) {
        log.info("Starting duplicate instance waste analyzer.");
        List<Waste> wasteList = new ArrayList<>();
        Set<InstanceDump> processedInstances = Collections.synchronizedSet(new HashSet<>());

        Map<Long, InstanceDump> userInstances = memoryDump.getUserInstances();
        int onePercent = userInstances.size() / 100;
        int currentPercent = 0;

        for (Long id : userInstances.keySet()) {
            InstanceDump instance = userInstances.get(id);

            processedInstances.add(instance);
            if (processedInstances.size() % onePercent == 0) {
                currentPercent++;
                log.info("Done {}%.", currentPercent);
            }

            memoryDump.getUserInstances().keySet().forEach(id2 -> {
                InstanceDump instance2 = memoryDump.getUserInstances().get(id2);

                if (id.equals(id2) || processedInstances.contains(instance2)) {
                    return;
                }

                if (this.instancesAreSame(instance, instance2)) {
                    this.mergeWasteList(wasteList, instance, instance2);
                }
            });
        }

        log.info("Finished duplicate instance waste analyses.");

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
        if (currentlyComparing.getOrDefault(a.getInstanceId(), Collections.emptySet()).contains(b.getInstanceId())) {
            return true;
        } else {
            currentlyComparing.computeIfAbsent(a.getInstanceId(), k -> new HashSet<>()).add(b.getInstanceId());
        }

        if (a.getInstanceId().equals(b.getInstanceId())) {
            return true;
        }

        if (!this.instancesOfSameClass(a, b)
                || a.getInstanceFieldValues().size() != b.getInstanceFieldValues().size()) {
            return false;
        }

        ClassDump classDump = a.getClassDump();

        for (InstanceFieldDump field : classDump.getInstanceFields()) {
            Object value = a.getInstanceFieldValues().get(field);
            Object value2 = b.getInstanceFieldValues().get(field);

            if (value instanceof InstanceDump && value2 instanceof InstanceDump) {
                if (!deepEquals((InstanceDump) value, (InstanceDump) value2, currentlyComparing)) {
                    return false;
                }
            } else if (value instanceof Value && value2 instanceof Value) {
                if (!Objects.equals(((Value<?>) value).value, ((Value<?>) value2).value)) {
                    return false;
                }
            } else if (!value.equals(value2)) {
                return false;
            }
        }

        return classDump.getInstanceFields().size() > 0;
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
