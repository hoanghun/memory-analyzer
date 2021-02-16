package cz.mxmx.memoryanalyzer.app;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Strings;
import cz.mxmx.memoryanalyzer.DefaultMemoryDumpAnalyzer;
import cz.mxmx.memoryanalyzer.MemoryDumpAnalyzer;
import cz.mxmx.memoryanalyzer.exception.MemoryDumpAnalysisException;
import cz.mxmx.memoryanalyzer.memorywaste.ReferenceAndDuplicateWasteAnalyzerPipeline;
import cz.mxmx.memoryanalyzer.memorywaste.WasteAnalyzerPipeline;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import cz.mxmx.memoryanalyzer.model.memorywaste.Waste;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class App {
	private static final Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		new App(args);
	}

	public App(String[] args) {
		Options options = new Options();

		Option listOptions = new Option("l", "list", false, "list namespaces and exit");
		listOptions.setRequired(false);
		options.addOption(listOptions);

		Option printFieldsOptions = new Option("f", "fields", false, "print the fields' values of the affected instances");
		printFieldsOptions.setRequired(false);
		options.addOption(printFieldsOptions);

		Option pathOption = new Option("p", "path", true, "input dump file path");
		pathOption.setRequired(true);
		options.addOption(pathOption);

		Option namespacesOption = new Option("n", "namespace", true, "namespace to analyze");
		namespacesOption.setRequired(false);
		options.addOption(namespacesOption);

		Option helpOption = new Option("h", "help", false, "print help");
		helpOption.setRequired(false);
		options.addOption(helpOption);

		Option csvOption = new Option("c", "csv", false, "write the result also to an csv file");
		csvOption.setRequired(false);
		options.addOption(csvOption);

		Option excludeOption = new Option("e", "exclude", true, "Exclude namespaces");
		excludeOption.setRequired(false);
		options.addOption(excludeOption);

		Option interactiveOption = new Option("i", "interactive", false, "Interactive shell.");
		interactiveOption.setRequired(false);
		options.addOption(interactiveOption);

		Option verboseOption = new Option("v", "verbose", false, "Verbose mode, prints ids of duplicates.");
		verboseOption.setRequired(false);
		options.addOption(verboseOption);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
			String inputFilePath = cmd.getOptionValue("path");
			String namespace = cmd.getOptionValue("namespace");
			String excludeNamespace = cmd.getOptionValue("exclude");
			boolean list = cmd.hasOption("list");
			boolean help = cmd.hasOption("help");
			boolean fields = cmd.hasOption("fields");
			boolean csv = cmd.hasOption("csv");
			boolean interactive = cmd.hasOption("interactive");
			boolean verbose = cmd.hasOption("verbose");

			List<ResultWriter> resultWriters = new ArrayList<>();
			if (csv) {
				resultWriters.add(new CsvResultWriter("result.csv", namespace));
			}

			DefaultMemoryDumpAnalyzer analyzer = new DefaultMemoryDumpAnalyzer(inputFilePath);
			if (interactive) {
				MemoryDump memoryDump = analyzer.analyze();

			} else if (list && !Strings.isNullOrEmpty(inputFilePath)) {
				Runnable measure = this.measure();

				log.info("Loading namespaces from {}...\n\n", inputFilePath);
				Set<String> namespaces = new TreeSet<>(this.getNamespaces(analyzer));

				System.out.println("List of namespaces in the given memory dump:");
				namespaces.forEach(ns -> System.out.format("\t%s\n", ns));

				measure.run();
			} else if (!Strings.isNullOrEmpty(namespace) && !Strings.isNullOrEmpty(inputFilePath)) {
				resultWriters.add(new ConsoleResultWriter(namespace));
				Runnable measure = this.measure();

				log.info("Analyzing classes from namespace `{}` in `{}`...\n\n", namespace, inputFilePath);
				MemoryDump memoryDump = this.getMemoryDump(analyzer, namespace);
				this.processMemoryDump(memoryDump, fields, verbose, resultWriters);

				measure.run();
				resultWriters.forEach(ResultWriter::close);
			} else if (excludeNamespace != null && !Strings.isNullOrEmpty(inputFilePath)) {
				resultWriters.add(new ConsoleResultWriter(excludeNamespace));
				Runnable measure = this.measure();

				log.info("Analyzing classes from excluding namespace `{}` in `{}`...\n\n", excludeNamespace, inputFilePath);
				MemoryDump memoryDump = this.getMemoryDumpExcludingNamespace(analyzer, excludeNamespace);
				this.processMemoryDump(memoryDump, fields, verbose, resultWriters);

				measure.run();
				resultWriters.forEach(ResultWriter::close);
			} else if (help) {
				formatter.printHelp("memory-analyzer", options);
			} else {
				log.info("No action defined. See --help for more info.");
			}

			System.exit(0);

		} catch (ParseException | MemoryDumpAnalysisException e) {
			log.error("Error: " + e.getMessage());
			formatter.printHelp("memory-analyzer", options);
			System.exit(1);
		} catch (FileNotFoundException e) {
			log.error("Error: Couldn't find the specified input file.");
		} catch (Exception e) { // print out anything else
			log.error(e.getMessage());
		}
	}

	/**
	 * Get namespaces from the given analyzer.
	 * @param analyzer Analyzer to use.
	 * @return Namespaces from the dump.
	 * @throws FileNotFoundException Dump not found.
	 * @throws MemoryDumpAnalysisException An error during processing.
	 */
	private Set<String> getNamespaces(MemoryDumpAnalyzer analyzer) throws FileNotFoundException, MemoryDumpAnalysisException {
		return analyzer.getNamespaces();
	}

	/**
	 * Get a dump from the given analyzer.
	 * @param analyzer Analyzer to use.
	 * @param namespace Namespaces to use as a filter.
	 * @return Processed dump.
	 * @throws FileNotFoundException Dump not found.
	 * @throws MemoryDumpAnalysisException An error during processing.
	 */
	private MemoryDump getMemoryDump(MemoryDumpAnalyzer analyzer, String namespace) throws FileNotFoundException, MemoryDumpAnalysisException {
		return analyzer.analyze(Lists.newArrayList(namespace));
	}

	private MemoryDump getMemoryDumpExcludingNamespace(MemoryDumpAnalyzer analyzer, String namespace) throws FileNotFoundException, MemoryDumpAnalysisException {
		return analyzer.excludeAndAnalyze(Collections.singletonList(namespace));
	}

	/**
	 * Run a processing of the given memory dump.
	 * @param memoryDump Memory dump.
	 * @param printFields True if the values should be printed out.
	 * @param resultWriters Result writer.
	 */
	private void processMemoryDump(MemoryDump memoryDump, boolean printFields, boolean verbose, List<ResultWriter> resultWriters) {
		resultWriters.forEach(writer -> writer.write(memoryDump));
		WasteAnalyzerPipeline wasteAnalyzer = new ReferenceAndDuplicateWasteAnalyzerPipeline();

		log.info("Starting waste analysis. Using {}.", wasteAnalyzer.getClass().getName());
		List<Waste> memoryWaste = wasteAnalyzer.findMemoryWaste(memoryDump);

		log.info("Finished waste analysis.");
		resultWriters.forEach(writer -> writer.write(memoryWaste, wasteAnalyzer, printFields, verbose));
	}

	/**
	 * Starts stopwatch and returns a callable; when the result is called, a time difference is printed to STOUT.
	 * @return Callable to stop the stopwatch and print out the result.
	 */
	private Runnable measure() {
		Instant now = Instant.now();

		return () -> {
			Duration duration = Duration.between(now, Instant.now());
			System.out.format("Duration: %s\n", duration);
		};
	}

}
