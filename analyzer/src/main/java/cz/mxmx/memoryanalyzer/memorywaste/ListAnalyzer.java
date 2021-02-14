package cz.mxmx.memoryanalyzer.memorywaste;

import cz.mxmx.memoryanalyzer.model.*;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Value;

import java.util.AbstractList;
import java.util.Map;

public abstract class ListAnalyzer {

    /**
     * Returns the elements of the given instance from the memory dump.
     *
     * @param memoryDump Memory dump to search in.
     * @param value      Value to look for.
     * @return Found values or null.
     */
    protected InstanceArrayDump getElements(MemoryDump memoryDump, InstanceDump value) {
        Map.Entry<InstanceFieldDump<?>, Object> elementField = value.getInstanceFieldValues().entrySet().stream()
                .filter((field) -> field.getKey().getName().equals("elementData"))
                .findAny().orElse(null);

        if (elementField != null) {
            Long arrayId = (Long) ((Value<?>) elementField.getValue()).value;
            return memoryDump.getInstanceArrays().get(arrayId);
        }

        return null;
    }

    /**
     * Checks whether the given instance is a list (instance of {@link AbstractList}).
     *
     * @param value Instance to check.
     * @return True if the given instance is a list, otherwise false.
     */
    protected boolean isList(InstanceDump value) {
        ClassDump parent = value.getClassDump();
        do {
            if (parent.getName().equals(AbstractList.class.getName())) {
                return true;
            }

            parent = parent.getSuperClassDump();
        } while (parent != null && parent.getName() != null && !parent.getName().equals(Object.class.getName()));

        return false;
    }
}
