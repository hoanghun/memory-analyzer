package cz.mxmx.memoryanalyzer.app;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Strings;
import cz.mxmx.memoryanalyzer.DefaultMemoryDumpAnalyzer;
import cz.mxmx.memoryanalyzer.MemoryDumpAnalyzer;
import cz.mxmx.memoryanalyzer.exception.MemoryDumpAnalysisException;
import cz.mxmx.memoryanalyzer.memorywaste.DefaultWasteAnalyzerPipeline;
import cz.mxmx.memoryanalyzer.memorywaste.WasteAnalyzer;
import cz.mxmx.memoryanalyzer.memorywaste.WasteAnalyzerPipeline;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import cz.mxmx.memoryanalyzer.model.memorywaste.Waste;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class App {
	public static final List<String> NAMESPACES = Lists.newArrayList("com.intellij");
//	public static final List<String> NAMESPACES = Lists.newArrayList("cz.mxmx");
	public static final String PATH = "sandbox/data/heapdump-intellij-idea.hprof";

	public static void main(String[] args) throws IOException, MemoryDumpAnalysisException {
		new App(args);
	}

	public App(String[] args) {
		Options options = new Options();

		Option listOptions = new Option("l", "list", false, "list namespaces and exit");
		listOptions.setRequired(false);
		options.addOption(listOptions);

		Option pathOption = new Option("p", "path", true, "input dump file path");
		pathOption.setRequired(true);
		options.addOption(pathOption);

		Option namespacesOption = new Option("n", "namespace", true, "namespace to analyze");
		namespacesOption.setRequired(false);
		options.addOption(namespacesOption);

		Option helpOption = new Option("h", "help", false, "print help");
		helpOption.setRequired(false);
		options.addOption(helpOption);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
			String inputFilePath = cmd.getOptionValue("path");
			String namespace = cmd.getOptionValue("namespace");
			boolean list = cmd.hasOption("list");
			boolean help = cmd.hasOption("help");

			if(list) {
				DefaultMemoryDumpAnalyzer analyzer = new DefaultMemoryDumpAnalyzer(inputFilePath);
				System.out.format("Loading namespaces from %s...\n\n", inputFilePath);
				Set<String> namespaces = new TreeSet<>(this.getNamespaces(analyzer));

				System.out.println("List of namespaces in the given memory dump:");
				namespaces.forEach(ns -> {
					System.out.format("\t%s\n", ns);
				});

			}
			else if(!Strings.isNullOrEmpty(namespace)) {
				DefaultMemoryDumpAnalyzer analyzer = new DefaultMemoryDumpAnalyzer(inputFilePath);
				System.out.format("Analyzing classes from namespace `%s` in `%s`...\n\n", namespace, inputFilePath);
				MemoryDump memoryDump = this.getMemoryDump(analyzer, namespace);
				this.processMemoryDump(memoryDump, namespace);
			} else if(help) {
				formatter.printHelp("memory-analyzer", options);
			} else {
				System.out.println("No action defined. See --help for more info.");
			}

			System.exit(0);

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("memory-analyzer", options);

			System.exit(1);
		} catch (FileNotFoundException | MemoryDumpAnalysisException e) {
			e.printStackTrace();
		}
	}

	private Set<String> getNamespaces(MemoryDumpAnalyzer analyzer) throws FileNotFoundException, MemoryDumpAnalysisException {
		return analyzer.getNamespaces();
	}

	private MemoryDump getMemoryDump(MemoryDumpAnalyzer analyzer, String namespace) throws FileNotFoundException, MemoryDumpAnalysisException {
		return analyzer.analyze(Lists.newArrayList(namespace));
	}

	private void processMemoryDump(MemoryDump memoryDump, String namespace) {
		System.out.println("Done, found:");
		System.out.println("\tClasses: " + memoryDump.getClasses().size());
		System.out.println("\tInstances: " + memoryDump.getInstances().size());
		System.out.println("Namespace " + namespace);
		System.out.println("\tClasses: " + memoryDump.getUserClasses().size());
		System.out.println("\tInstances: " + memoryDump.getUserInstances().size());

		System.out.println();
		System.out.println("Analyzing memory waste...");
		WasteAnalyzerPipeline wasteAnalyzer = new DefaultWasteAnalyzerPipeline();
		List<Waste> memoryWaste = wasteAnalyzer.findMemoryWaste(memoryDump);
		System.out.println("Done, found " + memoryWaste.size() + " possible ways to save memory:");

		Map<WasteAnalyzer, List<Waste>> classes = memoryWaste.stream().collect(Collectors.groupingBy(Waste::getSourceWasteAnalyzer));
		classes.forEach((type, list) -> {
				System.out.format("\t%s (%d):\n", wasteAnalyzer.getWasteTitle(type), list.size());
				list
					.stream()
					.map(waste -> String.format("\t\t%s: %s", waste.getTitle(), waste.getDescription()))
					.sorted()
					.forEach(System.out::println);
				System.out.println();
			});
	}

}
