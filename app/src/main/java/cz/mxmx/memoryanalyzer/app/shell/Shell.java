package cz.mxmx.memoryanalyzer.app.shell;

import cz.mxmx.memoryanalyzer.model.InstanceDump;
import cz.mxmx.memoryanalyzer.model.InstanceFieldDump;
import cz.mxmx.memoryanalyzer.model.MemoryDump;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Value;

import java.util.Map;
import java.util.Scanner;

public class Shell {
    private final Map<Long, InstanceDump> instances;

    public Shell(MemoryDump memoryDump) {
        this.instances = memoryDump.getInstances();
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) break;
                String command = scanner.nextLine();
                if (command.equals("stop")) break;

                String[] ids = command.split("[\\s,]+");
                for (String id : ids) {
                    try {
                        InstanceDump instanceDump = instances.get(Long.parseLong(id));
                        if (instanceDump == null) {
                            System.out.println("Didn't find any instance with given id.");
                        } else {
                            printInstanceDump(instanceDump);
                        }
                    } catch (NumberFormatException nfe) {
                        System.out.println("Not a number.");
                    }
                }
            }
        }
    }

    private void printInstanceDump(InstanceDump instanceDump) {
        System.out.printf("id: %d%n", instanceDump.getInstanceId());
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("class: %s\n", instanceDump.getClassDump().getName()));

        if (instanceDump.getClassDump().getInstanceFields().size() == 0) {
            sb.append("No fields.");
        }

        for (InstanceFieldDump<?> instanceField : instanceDump.getClassDump().getInstanceFields()) {
            Object value = instanceDump.getInstanceFieldValues().get(instanceField);

            String template = ": %s";
            String type = "";
            if (value instanceof Value) {
                type = String.format(template, ((Value<?>) value).type);
            } else if (value instanceof InstanceDump) {
                type = String.format(template, ((InstanceDump) value).getClassDump().getName());
            } else if (value instanceof String) {
                type = String.format(template, "String");
            }

            sb.append(String.format("\t%s %s = `%s`\n", instanceField.getName(), type, value));
        }

        System.out.println(sb.toString());
    }
}
