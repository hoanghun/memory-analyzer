package cz.mxmx.memoryanalyzer.memorywaste;

import com.google.common.collect.Lists;
import cz.mxmx.memoryanalyzer.MemoryDumpAnalyzer;
import cz.mxmx.memoryanalyzer.TestHelper;
import cz.mxmx.memoryanalyzer.exception.MemoryDumpAnalysisException;
import cz.mxmx.memoryanalyzer.model.memorywaste.ReferencesWaste;
import cz.mxmx.memoryanalyzer.model.memorywaste.Waste;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

import static cz.mxmx.memoryanalyzer.TestHelper.NAMESPACE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ReferenceAndDuplicateWasteAnalyzerPipelineTest {

    private MemoryDumpAnalyzer analyzer;
    private ReferenceAndDuplicateWasteAnalyzerPipeline wasteAnalyzerPipeline;

    @Before
    public void setUp() {
        this.analyzer = TestHelper.getMemoryDumpAnalyzerNullRefs();
        this.wasteAnalyzerPipeline = new ReferenceAndDuplicateWasteAnalyzerPipeline();
    }

    @Test
    public void testSize() throws FileNotFoundException, MemoryDumpAnalysisException {
        List<Waste> memoryWaste = this.wasteAnalyzerPipeline.findMemoryWaste(this.analyzer.analyze(Lists.newArrayList(NAMESPACE)));
        assertThat(memoryWaste.size(), is(11));
    }

    @Test
    public void testIndividualSize() throws FileNotFoundException, MemoryDumpAnalysisException {
        List<Waste> memoryWaste = this.wasteAnalyzerPipeline.findMemoryWaste(this.analyzer.analyze(Lists.newArrayList(NAMESPACE)));

        for (Waste waste : memoryWaste) {
            if (waste.getSourceWasteAnalyzer() instanceof DuplicateInstanceWasteAnalyzer) {
                assertThat(waste.getAffectedInstances().size(), is(10));
            }
        }

        Optional<Waste> optionalReferencesWaste = memoryWaste.stream().filter(waste -> waste.getSourceWasteAnalyzer() instanceof ReferenceWasteAnalyzer).findFirst();
        Assert.assertNotEquals(Optional.empty(), optionalReferencesWaste);
        if (optionalReferencesWaste.isPresent()) {
            ReferencesWaste referencesWaste = (ReferencesWaste) optionalReferencesWaste.get();
            Assert.assertEquals(326, referencesWaste.getReferencesCount());
            Assert.assertEquals(100, referencesWaste.getNullReferencesCount());
        }
    }
}
