package cz.mxmx.memoryanalyzer.memorywaste;

import cz.mxmx.memoryanalyzer.model.InstanceArrayDump;
import cz.mxmx.memoryanalyzer.model.InstanceDump;
import cz.mxmx.memoryanalyzer.model.InstanceFieldDump;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import cz.mxmx.memoryanalyzer.model.memorywaste.ListOfNullsWaste;
import cz.mxmx.memoryanalyzer.model.memorywaste.Waste;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzer to find ineffective list usage.
 */
public class ListWasteAnalyzer extends ListAnalyzer implements WasteAnalyzer {

    /**
     * Ineffective list usage threshold above which the list is marked as ineffective.
     */
    private static final double THRESHOLD = 0.5;

    @Override
    public List<Waste> findMemoryWaste(MemoryDump memoryDump) {
        List<Waste> wasteList = new ArrayList<>();

        memoryDump.getUserInstances().forEach((id, instance) ->
                instance.getInstanceFieldValues().forEach((field, value) -> {
                    if (value instanceof InstanceDump) {
                        if (this.isList((InstanceDump) value)) {
                            this.findWastedList(memoryDump, (InstanceDump) value, wasteList, instance, field);
                        }
                    }
                }));

        return wasteList;
    }

    /**
     * Search for a wasted list in the given parameters.
     *
     * @param memoryDump        Memory dump to look in.
     * @param value             Instance of the list itself.
     * @param wasteList         List with the results to put a new created Waste in.
     * @param instance          Instance with the field.
     * @param instanceFieldDump Field where the list is stored in.
     */
    private void findWastedList(MemoryDump memoryDump, InstanceDump value, List<Waste> wasteList, InstanceDump instance, InstanceFieldDump instanceFieldDump) {
        InstanceArrayDump elements = this.getElements(memoryDump, value);

        if (elements != null && elements.getValues() != null) {
            long nullCount = findNullWastedList(elements);
            if (nullCount > elements.getValues().size() * THRESHOLD) {
                wasteList.add(new ListOfNullsWaste(this, value, elements.getValues(), nullCount, instance, instanceFieldDump));
            }
        }
    }

    /**
     * Find the number of nulls within the given array.
     *
     * @param instanceArrayDump Array to check.
     * @return Number of nulls within the array.
     */
    private long findNullWastedList(InstanceArrayDump instanceArrayDump) {
        final long[] nullCount = {0};

        instanceArrayDump.getValues().forEach(val -> {
            if (val == null) {
                nullCount[0]++;
            }
        });

        return nullCount[0];
    }
}
