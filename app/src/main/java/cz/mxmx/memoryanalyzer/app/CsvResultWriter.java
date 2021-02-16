package cz.mxmx.memoryanalyzer.app;

import cz.mxmx.memoryanalyzer.memorywaste.WasteAnalyzer;
import cz.mxmx.memoryanalyzer.memorywaste.WasteAnalyzerPipeline;
import cz.mxmx.memoryanalyzer.model.InstanceDump;
import cz.mxmx.memoryanalyzer.model.InstanceFieldDump;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import cz.mxmx.memoryanalyzer.model.memorywaste.Waste;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Result writer to the CSV format.
 */
public class CsvResultWriter implements ResultWriter {

	/**
	 * OS independent new line.
	 */
	private static final String NL = System.lineSeparator();

	/**
	 * Filename to save the result to.
	 */
	private final String filename;

	/**
	 * Namespace of the dump.
	 */
	private final String namespace;

	/**
	 * String builder to save the results into until they're saved into the file.
	 */
	private final StringBuilder sb;

	public CsvResultWriter(String filename, String namespace) {
		this.filename = filename;
		this.namespace = namespace;
		this.sb = new StringBuilder();
	}

	@Override
	public void write(MemoryDump memoryDump) {
		this.sb.append("Namespace ");
		this.sb.append(NL);
		this.sb.append(this.namespace);
		this.sb.append(NL);
		this.sb.append(NL);

		this.sb.append("Classes,Instances");
		this.sb.append(NL);

		this.sb.append(memoryDump.getClasses().size());
		this.sb.append(',');
		this.sb.append(memoryDump.getInstances().size());
		this.sb.append(NL);

		this.sb.append(memoryDump.getUserClasses().size());
		this.sb.append(',');
		this.sb.append(memoryDump.getUserInstances().size());
		this.sb.append(NL);
		this.sb.append(NL);
	}

	@Override
	public void write(List<Waste> wasteList, WasteAnalyzerPipeline wasteAnalyzer, boolean printFields, boolean verbose) {
		this.sb.append("Type,Count,Aggregation");
		if(printFields) {
			this.sb.append(",Fields");
		}

		this.sb.append(NL);

		this.sb.append("Total,");
		this.sb.append(wasteList.size());
		this.sb.append(",true");

		if(printFields) {
			this.sb.append(',');
		}

		this.sb.append(NL);

		Map<WasteAnalyzer, List<Waste>> classes = wasteList
				.stream()
				.collect(Collectors.groupingBy(Waste::getSourceWasteAnalyzer));

		classes
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Comparator.comparingInt(value -> -value.size())))
				.forEach(kv -> {
					WasteAnalyzer type = kv.getKey();
					List<Waste> list = kv.getValue();

					this.sb.append(wasteAnalyzer.getWasteTitle(type));
					this.sb.append(",");
					this.sb.append(list.size());
					this.sb.append(",true");

					if(printFields) {
						this.sb.append(',');
					}

					this.sb.append(NL);

					list
							.stream()
							.sorted()
							.map(waste -> String.format(
									"%s,%s,false%s%s",
									waste.getSource(),
									waste.getNominal(),
									printFields ? this.dumpInstanceFields(waste) : "",
									NL
							))
							.forEach(this.sb::append);

					this.sb.append(NL);
				});
	}

	/**
	 * Create a string representation of the instance fields.
	 * @param waste Memory waste.
	 * @return String representation of the instance fields.
	 */
	private String dumpInstanceFields(Waste waste) {
		StringBuilder sb = new StringBuilder();
		Optional<InstanceDump> first = waste.getAffectedInstances().stream().findFirst();

		sb.append(",");

		List<String> fields = new ArrayList<>();

		if (first.isPresent()) {
			for (InstanceFieldDump<?> instanceField : first.get().getClassDump().getInstanceFields()) {
				Object value = first.get().getInstanceFieldValues().get(instanceField);

				fields.add(String.format("'%s'='%s'", instanceField.getName(), value.toString().replace("'", "\\'")));
			}
		}

		sb.append(String.join("|", fields));

		return sb.toString();
	}

	@Override
	public void close() {
		try (PrintWriter writer = new PrintWriter(this.filename)) {
			writer.write(this.sb.toString());
		} catch (FileNotFoundException e) {
			System.out.println("Couldn't export the CSV file due to an error: " + e.getMessage());
		}
	}
}
