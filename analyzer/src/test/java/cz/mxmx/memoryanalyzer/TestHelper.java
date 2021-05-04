package cz.mxmx.memoryanalyzer;

public class TestHelper {
	public static final String PATH = "../sandbox/data/test-heapdump-10.hprof";
	public static final String PATH_WITH_NULL_VALUES = "../sandbox/data/null-refs-10.hprof";
	public static final String PATH_WITH_INHERITANCE_DUMP = "../sandbox/data/inheritance-test.hprof";
	public static final String NAMESPACE = "cz.mxmx.memoryanalyzer.example";

	public static MemoryDumpAnalyzer getMemoryDumpAnalyzer() {
		return new DefaultMemoryDumpAnalyzer(PATH);
	}

	public static MemoryDumpAnalyzer getMemoryDumpAnalyzerNullRefs() {
		return new DefaultMemoryDumpAnalyzer(PATH_WITH_NULL_VALUES);
	}

	public static MemoryDumpAnalyzer getMemoryAnalyzerInheritance() {
		return new DefaultMemoryDumpAnalyzer(PATH_WITH_INHERITANCE_DUMP);
	}
}
