package cz.mxmx.memoryanalyzer.memorywaste;

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
    private boolean useDeepEquals = true;

    public DuplicateInstanceWasteAnalyzer() {
    }

    public DuplicateInstanceWasteAnalyzer(boolean useDeepEquals) {
        this.useDeepEquals = useDeepEquals;
    }

    /**
     * Analyzes specified instances and finds duplicate instances.
     * Instances a,b are duplicate iff their fields are the same
     * including field references.
     *
     * @param memoryDump Memory dump to process.
     * @return list of instances and their duplicates.
     */
    @Override
    public List<Waste> findMemoryWaste(MemoryDump memoryDump) {
        log.info("Instance waste analysis.");
        List<Waste> wasteList = new ArrayList<>();

        List<InstanceDump> instances = new ArrayList<>(memoryDump.getUserInstances().values());
        int total = instances.size();
        int onePercent = total / 100;
        log.info("Starting duplicate instance waste analyzer. Using deep equals.");
        long doneCount = 0;
        List<InstanceDump> duplicates;

        for (int i = 0; i < instances.size() - 1; i++) {
            duplicates = new ArrayList<>();
            InstanceDump instance = instances.get(i);
            if (instance == null) continue;
            for (int j = i + 1; j < instances.size(); j++) {
                InstanceDump compareWith = instances.get(j);
                if (compareWith == null) continue;

                if (this.instancesAreSame(instance, compareWith)) {
                    duplicates.add(instances.set(j, null));
                }
            }
            doneCount++;

            if (duplicates.size() > 0) {
                doneCount += duplicates.size();
                duplicates.add(instance);
                wasteList.add(new DuplicateInstanceWaste(this, new ArrayList<>(duplicates)));
            }

            if (doneCount % onePercent == 0) {
                log.info("Done {}%.", (doneCount / ((double) total)) * 100);
            }
        }

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
        if (useDeepEquals) {
            final Set<InstancesIds> currentlyComparing = new HashSet<>();
            return deepEquals(instance, instance2, currentlyComparing);
        } else {
            return shallowEquals(instance, instance2);
        }
    }

    /**
     * Compares instances a and b based on their fields. If their fields are references, these
     * fields are recursively compared too. Instances have to be of the same class hierarchy
     * and have to have same amount of fields to even consider that they are duplicates.
     *
     * @param a                  instance A
     * @param b                  instance B
     * @param currentlyComparing set of currently comparing pairs to prevent cycles.
     * @return true if a and b are duplicates else false
     */
    private boolean deepEquals(InstanceDump a, InstanceDump b, Set<InstancesIds> currentlyComparing) {
        if (!(a.getClassDump() == b.getClassDump())) {
            return false;
        }

        InstancesIds ids = new InstancesIds(a.getInstanceId(), b.getInstanceId());
        if (currentlyComparing.contains(ids)) {
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

        return instancesSame;
    }

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
     * @param a instance dump a
     * @param b instance dumb b
     * @return true if objects have same references or string values.
     */
    @SuppressWarnings("unused")
    private boolean shallowEquals(InstanceDump a, InstanceDump b) {
        if (a.getInstanceFieldValues().size() != b.getInstanceFieldValues().size()) {
            return false;
        }

        if (this.instancesOfSameClass(a, b)) {
            ClassDump classDump = a.getClassDump();

            for (InstanceFieldDump<?> field : classDump.getInstanceFields()) {
                Object value = a.getInstanceFieldValues().get(field);
                Object value2 = b.getInstanceFieldValues().get(field);

                if (!value.equals(value2)) {
                    return false;
                }
            }
            return classDump.getInstanceFields().size() > 0;
        }

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
