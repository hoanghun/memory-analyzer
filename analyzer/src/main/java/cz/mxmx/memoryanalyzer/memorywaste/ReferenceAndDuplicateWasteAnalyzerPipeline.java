package cz.mxmx.memoryanalyzer.memorywaste;

import java.util.HashMap;
import java.util.Map;

/**
 * Pipeline which includes reference and instance duplication analysis
 */
public class ReferenceAndDuplicateWasteAnalyzerPipeline extends WasteAnalyzerPipeline {

    /**
     * Analyzers registered here will be automatically used to process a memory dump.
     */
    private static final Map<WasteAnalyzer, String> ANALYZERS = new HashMap<WasteAnalyzer, String>() {{
        put(new DuplicateInstanceWasteAnalyzer(), "Duplicate instances");
//        put(new ReferenceWasteAnalyzer(), "References analysis");
    }};

    public ReferenceAndDuplicateWasteAnalyzerPipeline() {
        super(ANALYZERS, true);
    }
}
