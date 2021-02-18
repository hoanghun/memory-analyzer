package cz.mxmx.memoryanalyzer.memorywaste;

import cz.mxmx.memoryanalyzer.model.ClassDump;
import cz.mxmx.memoryanalyzer.model.InstanceDump;
import cz.mxmx.memoryanalyzer.model.InstanceFieldDump;
import cz.mxmx.memoryanalyzer.model.memorywaste.ReferenceCounter;
import cz.mxmx.memoryanalyzer.process.GenericMemoryDumpProcessor;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Type;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Value;
import org.junit.Assert;
import org.junit.Test;

public class ReferenceWasteAnalyzerTest {

    @Test
    public void testReferenceCounter() {
        ClassDump classDump = new ClassDump(0L, "cz.fav.zcu.Child", 1, null);

        int i;
        for (i= 0; i < 5; i++) {
            classDump.addInstanceField("ref" + i, GenericMemoryDumpProcessor.getClass("object"));
        }
        InstanceDump instance = new InstanceDump(0L, classDump);

        i = 0;
        for (InstanceFieldDump<?> fieldDump : classDump.getInstanceFields()) {
            instance.addInstanceField(fieldDump, new Value<>(Type.OBJ, i < 3 ? 0L : 1L));
            i++;
        }

        ReferenceCounter counter = ReferenceWasteAnalyzer.countReferences(instance);

        Assert.assertEquals(5, counter.getReferencesCount());
        Assert.assertEquals(3, counter.getNullReferencesCount());
    }
}
