package cz.mxmx.memoryanalyzer.app;

import cz.mxmx.memoryanalyzer.MemoryDumpAnalyzer;
import cz.mxmx.memoryanalyzer.model.MemoryDump;

public class Shell {
    private MemoryDump memoryDump;
    private MemoryDumpAnalyzer memoryDumpAnalyzer;

    public Shell(MemoryDump memoryDump, MemoryDumpAnalyzer memoryDumpAnalyzer) {
        this.memoryDump = memoryDump;
        this.memoryDumpAnalyzer = memoryDumpAnalyzer;
    }

    public void run() {

    }
}
