package cz.mxmx.memoryanalyzer.memorywaste;

import cz.mxmx.memoryanalyzer.model.ClassDump;
import cz.mxmx.memoryanalyzer.model.InstanceDump;
import cz.mxmx.memoryanalyzer.model.InstanceFieldDump;
import cz.mxmx.memoryanalyzer.process.GenericMemoryDumpProcessor;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Type;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Value;
import org.junit.Assert;
import org.junit.Test;

public class DeepEqualsTest {

    @Test
    public void simpleEqualsWithoutNesting() {
        long id = 0;
        ClassDump classDump = new ClassDump(0L, "cz.fav.zcu.Class", 1, null);
        classDump.addInstanceField("ref", GenericMemoryDumpProcessor.getClass("int"));

        InstanceFieldDump instanceFieldDump = classDump.getInstanceFields().get(0);
        InstanceDump first = new InstanceDump(id++, classDump);
        first.addInstanceField(instanceFieldDump, new Value<>(Type.INT, 3));

        InstanceDump second = new InstanceDump(id, classDump);
        second.addInstanceField(instanceFieldDump, new Value<>(Type.INT, 3));

        DuplicateInstanceWasteAnalyzer analyzer = new DuplicateInstanceWasteAnalyzer();
        Assert.assertTrue(analyzer.instancesAreSame(first, second));
    }

    @Test
    public void notEqualsWithNested() {
        long id = 0;
        ClassDump childClassDump = new ClassDump(0L, "cz.fav.zcu.Child", 1, null);

        childClassDump.addInstanceField("ref", GenericMemoryDumpProcessor.getClass("int"));
        InstanceFieldDump instanceFieldDump = childClassDump.getInstanceFields().get(0);
        InstanceDump firstChild = new InstanceDump(id++, childClassDump);
        firstChild.addInstanceField(instanceFieldDump, new Value<>(Type.INT, 3));

        InstanceDump secondChild = new InstanceDump(id++, childClassDump);
        secondChild.addInstanceField(instanceFieldDump, new Value<>(Type.INT, 2));

        ClassDump parentClassDump = new ClassDump(0L, "cz.fav.zcu.Parent", 1, null);
        parentClassDump.addInstanceField("ref", GenericMemoryDumpProcessor.getClass("object"));
        instanceFieldDump = parentClassDump.getInstanceFields().get(0);

        InstanceDump firstParent = new InstanceDump(id++, parentClassDump);
        firstParent.addInstanceField(instanceFieldDump, firstChild);

        InstanceDump secondParent = new InstanceDump(id, parentClassDump);
        secondParent.addInstanceField(instanceFieldDump, secondChild);

        DuplicateInstanceWasteAnalyzer analyzer = new DuplicateInstanceWasteAnalyzer();
        Assert.assertFalse(analyzer.instancesAreSame(firstParent, secondParent));
    }

    @Test
    public void equalsWithNested() {
        long id = 0;
        long classId = 0;
        ClassDump childClassDump = new ClassDump(classId++, "cz.fav.zcu.Child", 1, null);

        childClassDump.addInstanceField("ref", GenericMemoryDumpProcessor.getClass("int"));
        InstanceFieldDump instanceFieldDump = childClassDump.getInstanceFields().get(0);
        InstanceDump firstChild = new InstanceDump(id++, childClassDump);
        firstChild.addInstanceField(instanceFieldDump, new Value<>(Type.INT, 3));

        InstanceDump secondChild = new InstanceDump(id++, childClassDump);
        secondChild.addInstanceField(instanceFieldDump, new Value<>(Type.INT, 3));

        ClassDump parentClassDump = new ClassDump(classId, "cz.fav.zcu.Parent", 1, null);
        parentClassDump.addInstanceField("ref", GenericMemoryDumpProcessor.getClass("object"));
        instanceFieldDump = parentClassDump.getInstanceFields().get(0);

        InstanceDump firstParent = new InstanceDump(id++, parentClassDump);
        firstParent.addInstanceField(instanceFieldDump, firstChild);

        InstanceDump secondParent = new InstanceDump(id, parentClassDump);
        secondParent.addInstanceField(instanceFieldDump, secondChild);

        DuplicateInstanceWasteAnalyzer analyzer = new DuplicateInstanceWasteAnalyzer();
        Assert.assertTrue(analyzer.instancesAreSame(firstParent, secondParent));
    }

    @Test
    public void equalsWithNestedAndCycle() {
        long id = 0;
        long classId = 0;
        ClassDump childClassDump = new ClassDump(classId++, "cz.fav.zcu.Child", 1, null);

        childClassDump.addInstanceField("ref", GenericMemoryDumpProcessor.getClass("object"));
        InstanceDump firstChild = new InstanceDump(id++, childClassDump);
        InstanceDump secondChild = new InstanceDump(id++, childClassDump);

        ClassDump parentClassDump = new ClassDump(classId, "cz.fav.zcu.Parent", 1, null);
        parentClassDump.addInstanceField("ref", GenericMemoryDumpProcessor.getClass("object"));
        InstanceFieldDump instanceFieldDump = parentClassDump.getInstanceFields().get(0);

        InstanceDump firstParent = new InstanceDump(id++, parentClassDump);
        firstParent.addInstanceField(instanceFieldDump, firstChild);

        InstanceDump secondParent = new InstanceDump(id, parentClassDump);
        secondParent.addInstanceField(instanceFieldDump, secondChild);

        InstanceFieldDump instanceFieldDumpForChild = childClassDump.getInstanceFields().get(0);
        firstChild.addInstanceField(instanceFieldDumpForChild, firstParent);
        secondChild.addInstanceField(instanceFieldDumpForChild, secondParent);

        DuplicateInstanceWasteAnalyzer analyzer = new DuplicateInstanceWasteAnalyzer();
        Assert.assertTrue(analyzer.instancesAreSame(firstParent, secondParent));
    }

    @Test
    public void notEqualsWithNestedAndCycle() {
        long id = 0;
        long classId = 0;
        ClassDump childClassDump = new ClassDump(classId++, "cz.fav.zcu.Child", 1, null);

        childClassDump.addInstanceField("ref", GenericMemoryDumpProcessor.getClass("object"));
        childClassDump.addInstanceField("intos", GenericMemoryDumpProcessor.getClass("int"));

        InstanceFieldDump instanceFieldDump = childClassDump.getInstanceFields().get(1);
        InstanceDump firstChild = new InstanceDump(id++, childClassDump);
        InstanceDump secondChild = new InstanceDump(id++, childClassDump);

        firstChild.addInstanceField(instanceFieldDump, new Value<>(Type.INT, 4));
        secondChild.addInstanceField(instanceFieldDump, new Value<>(Type.INT, 3));

        ClassDump parentClassDump = new ClassDump(classId, "cz.fav.zcu.Parent", 1, null);
        parentClassDump.addInstanceField("ref", GenericMemoryDumpProcessor.getClass("object"));
        instanceFieldDump = parentClassDump.getInstanceFields().get(0);

        InstanceDump firstParent = new InstanceDump(id++, parentClassDump);
        firstParent.addInstanceField(instanceFieldDump, firstChild);

        InstanceDump secondParent = new InstanceDump(id, parentClassDump);
        secondParent.addInstanceField(instanceFieldDump, secondChild);

        InstanceFieldDump instanceFieldDumpForChild = childClassDump.getInstanceFields().get(0);
        firstChild.addInstanceField(instanceFieldDumpForChild, firstParent);
        secondChild.addInstanceField(instanceFieldDumpForChild, secondParent);

        DuplicateInstanceWasteAnalyzer analyzer = new DuplicateInstanceWasteAnalyzer();
        Assert.assertFalse(analyzer.instancesAreSame(firstParent, secondParent));
    }
}
