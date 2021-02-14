package cz.mxmx.memoryanalyzer.memorywaste;

import cz.mxmx.memoryanalyzer.model.InstanceArrayDump;
import cz.mxmx.memoryanalyzer.model.InstanceDump;
import cz.mxmx.memoryanalyzer.model.InstanceFieldDump;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import cz.mxmx.memoryanalyzer.model.memorywaste.ListOfDuplicatesWaste;
import cz.mxmx.memoryanalyzer.model.memorywaste.Waste;

import java.util.*;

/**
 * Analyzer to find ineffective list usage.
 */
public class ListOfDuplicatesAnalyzer extends ListAnalyzer implements WasteAnalyzer {

    @Override
    public List<Waste> findMemoryWaste(MemoryDump memoryDump) {
        List<Waste> wasteList = new ArrayList<>();

        memoryDump.getUserInstances().forEach((id, instance) ->
                instance.getInstanceFieldValues().forEach((field, value) -> {
                    if (value instanceof InstanceDump) {
                        if (this.isList((InstanceDump) value)) {
                            this.findWastedList(memoryDump, field, (InstanceDump) value, wasteList);
                        }
                    }
                }));

        return wasteList;
    }

    /**
     * Finds ineffective usage in the given list.
     *
     * @param memoryDump Memory dump to process.
     * @param field      Field in which is the list stored.
     * @param value      List instance itself.
     * @param wasteList  List with results.
     */
    private void findWastedList(MemoryDump memoryDump, InstanceFieldDump<?> field, InstanceDump value, List<Waste> wasteList) {
        InstanceArrayDump elements = this.getElements(memoryDump, value);

        if (elements != null && elements.getValues() != null) {
            long differentValues = findDifferentValues(elements);
            long nonNullCount = elements.getValues().stream().filter(Objects::nonNull).count();

            if (differentValues == 1 && nonNullCount > 1) {
                wasteList.add(new ListOfDuplicatesWaste(this, value, field, nonNullCount));
            }
        }
    }

    /**
     * Returns how many different values are within the given array instance.
     *
     * @param instanceArrayDump Array instance to check.
     * @return Number of different values (not including null).
     */
    private long findDifferentValues(InstanceArrayDump instanceArrayDump) {
        Set<Object> objects = new HashSet<>(instanceArrayDump.getValues());
        objects.remove(null);
        return objects.size();
    }
}
