package cz.mxmx.memoryanalyzer.process;

import cz.mxmx.memoryanalyzer.model.InstanceFieldDump;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class InstanceDumpFieldTest {

    private void assertObjectsLast(List<InstanceFieldDump<?>> fields, int objectsCounts) {
        for (int i = 0; i < objectsCounts; i++) {
            Assert.assertNotEquals(GenericMemoryDumpProcessor.getClass("object"), fields.get(i).getType());
            Assert.assertEquals(GenericMemoryDumpProcessor.getClass("object"), fields.get(fields.size() - 1 - i).getType());
        }
    }

    @Test
    public void instanceDumpFieldOrdering() {
        List<InstanceFieldDump<?>> fields = new ArrayList<>();
        fields.add(createField("first", "object"));
        fields.add(createField("second", "object"));
        fields.add(createField("third", "int"));
        fields.add(createField("fourth", "long"));
        fields.add(createField("fifth", "boolean"));
        fields.add(createField("sixth", null));

        fields.sort(GenericMemoryDumpProcessor.fieldsComparator());
        assertObjectsLast(fields, 2);
    }

    @Test
    public void instanceDumpFieldOrdering2() {
        List<InstanceFieldDump<?>> fields = new ArrayList<>();
        fields.add(createField("third", "int"));
        fields.add(createField("fourth", "long"));
        fields.add(createField("fifth", "boolean"));
        fields.add(createField("sixth", null));
        fields.add(createField("first", "object"));
        fields.add(createField("second", "object"));

        fields.sort(GenericMemoryDumpProcessor.fieldsComparator());
        assertObjectsLast(fields, 2);
    }

    @Test
    public void instanceDumpFieldOrdering3() {
        List<InstanceFieldDump<?>> fields = new ArrayList<>();
        fields.add(createField("third", "int"));
        fields.add(createField("fourth", "long"));
        fields.add(createField("second", "object"));
        fields.add(createField("fifth", "boolean"));
        fields.add(createField("first", "object"));
        fields.add(createField("sixth", null));

        fields.sort(GenericMemoryDumpProcessor.fieldsComparator());
        assertObjectsLast(fields, 2);
    }

    private InstanceFieldDump<?> createField(String fieldName, String type) {
        return new InstanceFieldDump<>(fieldName, GenericMemoryDumpProcessor.getClass(type));
    }
}
