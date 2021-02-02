package cz.mxmx.memoryanalyzer.model.memorywaste;

import cz.mxmx.memoryanalyzer.memorywaste.WasteAnalyzer;
import cz.mxmx.memoryanalyzer.model.InstanceDump;

import java.util.Collections;
import java.util.List;

public class ReferencesWaste implements Waste {

    /**
     * Title template of the reference waste.
     */
    private static final String TITLE_TEMPLATE = "References waste analysis.";

    /**
     * Description template of the memory waste.
     */
    private static final String DESC_TEMPLATE = "Total references count %d, of that %d are null references.";

    /**
     * The source analyzer.
     */
    private final WasteAnalyzer sourceWasteAnalyzer;

    private final long referencesCount;
    private final long nullReferencesCount;

    /**
     * Creates a representation of the duplicate waste type.
     * @param sourceWasteAnalyzer Source analyzer of this waste.
     * @param referencesCount total count of references found
     * @param nullReferencesCount total count of null references found
     */
    public ReferencesWaste(WasteAnalyzer sourceWasteAnalyzer, long referencesCount, long nullReferencesCount) {
        this.sourceWasteAnalyzer = sourceWasteAnalyzer;
        this.referencesCount = referencesCount;
        this.nullReferencesCount = nullReferencesCount;
    }

    @Override
    public Long estimateWastedBytes() {
        return null;
    }

    @Override
    public String getTitle() {
        return TITLE_TEMPLATE;
    }

    @Override
    public String getDescription() {
        return String.format(DESC_TEMPLATE, referencesCount, nullReferencesCount);
    }

    @Override
    public List<InstanceDump> getAffectedInstances() {
        return Collections.emptyList();
    }

    @Override
    public void addAffectedInstance(InstanceDump instanceDump) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WasteAnalyzer getSourceWasteAnalyzer() {
        return sourceWasteAnalyzer;
    }

    @Override
    public String getNominal() {
        return Double.toString(nullReferencesCount / (double)referencesCount);
    }

    @Override
    public String getSource() {
        return "References.";
    }

    @Override
    public int compareTo(Waste o) {
        return 0;
    }
}
