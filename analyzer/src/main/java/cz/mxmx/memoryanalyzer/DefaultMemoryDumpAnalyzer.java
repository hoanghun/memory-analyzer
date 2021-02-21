package cz.mxmx.memoryanalyzer;

import cz.mxmx.memoryanalyzer.exception.MemoryDumpAnalysisException;
import cz.mxmx.memoryanalyzer.model.ClassDump;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import cz.mxmx.memoryanalyzer.model.raw.RawMemoryDump;
import cz.mxmx.memoryanalyzer.parse.RawRecordHandler;
import cz.mxmx.memoryanalyzer.parse.RecordHandler;
import cz.mxmx.memoryanalyzer.process.FilteredMemoryDumpProcessor;
import cz.mxmx.memoryanalyzer.process.GenericMemoryDumpProcessor;
import cz.mxmx.memoryanalyzer.process.MemoryDumpProcessor;
import cz.mxmx.memoryanalyzer.util.Normalization;
import edu.tufts.eaftan.hprofparser.parser.HprofParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default memory analyzer implementation.
 */
public class DefaultMemoryDumpAnalyzer implements MemoryDumpAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(DefaultMemoryDumpAnalyzer.class);

    /**
     * Memory handler to use in the Hprof Library.
     */
    private final RecordHandler recordHandler;

    /**
     * Path to the dump file.
     */
    private final String path;

    /**
     * Generic (raw) processor.
     */
    private final MemoryDumpProcessor genericProcessor = new GenericMemoryDumpProcessor();

    /**
     * Raw memory dump after loading.
     */
    private RawMemoryDump memoryDump;

    /**
     * Creates an analyzer of the given dump with default {@link RawRecordHandler}.
     *
     * @param path Path to the dump.
     */
    public DefaultMemoryDumpAnalyzer(String path) {
        this(path, new RawRecordHandler());
    }

    /**
     * Creates an analyzer of the given dump.
     *
     * @param path          Path to the dump.
     * @param recordHandler Record handler.
     */
    public DefaultMemoryDumpAnalyzer(String path, RecordHandler recordHandler) {
        this.path = path;
        this.recordHandler = recordHandler;
    }

    /**
     * Run an analysis of the given dump.
     *
     * @throws MemoryDumpAnalysisException Memory dump analysis problem.
     * @throws FileNotFoundException       Dump not found.
     */
    private void runAnalysis() throws MemoryDumpAnalysisException, FileNotFoundException {
        if (this.memoryDump == null) {
            HprofParser parser = new HprofParser(this.recordHandler);
            FileInputStream fs = new FileInputStream(path);
            DataInputStream in = new DataInputStream(new BufferedInputStream(fs));

            try {
                log.info("Starting parsing dump from {}", path);
                parser.parse(in);
                log.info("Finished parsing the dump.");
                in.close();
            } catch (IOException e) {
                throw new MemoryDumpAnalysisException(e);
            }

            this.memoryDump = this.recordHandler.getMemoryDump();
        }
    }

    @Override
    public Set<String> getNamespaces() throws FileNotFoundException, MemoryDumpAnalysisException {
        this.runAnalysis();
        log.info("Processing loaded raw dump.");
        MemoryDump dump = this.genericProcessor.process(this.memoryDump);
        log.info("Finished processing loaded raw dump.");

        Set<String> namespaces = dump.getClasses().values().stream()
                .map(ClassDump::getName)
                .collect(Collectors.toSet());

        return this.filterNamespaces(namespaces);
    }

    /**
     * Translates namespaces into their regexes.
     *
     * @param namespaces Namespaces.
     * @return Namespace regex representation.
     */
    private Set<String> filterNamespaces(Set<String> namespaces) {
        return namespaces
                .stream()
                .filter(namespace -> !namespace.contains("$") && !namespace.startsWith("["))
                .map(namespace -> namespace.contains(".") ? namespace.substring(0, namespace.lastIndexOf(".")) : namespace)
                .collect(Collectors.toSet());
    }


    @Override
    public MemoryDump analyze() throws FileNotFoundException, MemoryDumpAnalysisException {
        MemoryDumpProcessor processor = new GenericMemoryDumpProcessor();
        return analyzeMemoryDump(processor);
    }

    @Override
    public MemoryDump analyze(List<String> namespaces) throws FileNotFoundException, MemoryDumpAnalysisException {
        MemoryDumpProcessor processor = new FilteredMemoryDumpProcessor(this.genericProcessor, Normalization.stringToRegexNamespaces(namespaces), false);
        return analyzeMemoryDump(processor);
    }

    @Override
    public MemoryDump excludeAndAnalyze(List<String> namespaces) throws FileNotFoundException, MemoryDumpAnalysisException {
        MemoryDumpProcessor processor = new FilteredMemoryDumpProcessor(this.genericProcessor, Normalization.stringToRegexNamespaces(namespaces), true);
        return analyzeMemoryDump(processor);
    }

    private MemoryDump analyzeMemoryDump(MemoryDumpProcessor processor) throws FileNotFoundException, MemoryDumpAnalysisException {
        this.runAnalysis();

        log.info("Processing raw dump.");
        MemoryDump process = processor.process(this.memoryDump);
        log.info("Finished processing raw dump.");
        return process;
    }
}
