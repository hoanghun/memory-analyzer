package cz.mxmx.memoryanalyzer.memorywaste;

import com.beust.jcommander.internal.Lists;
import cz.mxmx.memoryanalyzer.MemoryDumpAnalyzer;
import cz.mxmx.memoryanalyzer.TestHelper;
import cz.mxmx.memoryanalyzer.exception.MemoryDumpAnalysisException;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import cz.mxmx.memoryanalyzer.model.memorywaste.Waste;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.List;

public class InheritanceTest {
    private MemoryDumpAnalyzer analyzer;

    @Before
    public void setUp() {
        this.analyzer = TestHelper.getMemoryAnalyzerInheritance();
    }


    @Test
    public void testInheritanceSize() throws FileNotFoundException, MemoryDumpAnalysisException {
        MemoryDump memoryDump = this.analyzer.analyze(Lists.newArrayList(TestHelper.NAMESPACE));
        DuplicateInstanceWasteAnalyzer duplicateAnalyzer = new DuplicateInstanceWasteAnalyzer();

        List<Waste> wasteList = duplicateAnalyzer.findMemoryWaste(memoryDump);

        Assert.assertEquals(2, wasteList.size());
        Assert.assertEquals(2, wasteList.get(0).getAffectedInstances().size());
        Assert.assertEquals(2, wasteList.get(0).getAffectedInstances().get(0).getInstanceFieldValues().size());
    }
}
