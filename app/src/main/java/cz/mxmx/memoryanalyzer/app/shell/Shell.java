package cz.mxmx.memoryanalyzer.app.shell;

import cz.mxmx.memoryanalyzer.model.InstanceDump;
import cz.mxmx.memoryanalyzer.model.InstanceFieldDump;
import cz.mxmx.memoryanalyzer.model.MemoryDump;

import java.util.Arrays;
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
                Arrays.stream(ids).forEach(id -> {
                    try {
                        InstanceDump instanceDump = instances.get(Long.parseLong(id));
                        if (instanceDump == null) {
                            System.out.println("Didn't find any instance with given id.");
                        }
                        else {
                            printInstanceDump(instanceDump);
                        }
                    } catch (NumberFormatException nfe) {
                        System.out.println("Not a number.");
                    }
                });
            }
        }
    }

    private void printInstanceDump(InstanceDump instanceDump) {
        System.out.printf("Id: %d%n", instanceDump.getInstanceId());
        StringBuilder sb = new StringBuilder();
        for (InstanceFieldDump<?> instanceField : instanceDump.getClassDump().getInstanceFields()) {
            Object value = instanceDump.getInstanceFieldValues().get(instanceField);

            sb.append(String.format("\t%s = `%s`\n", instanceField.getName(), value));
        }

        System.out.println(sb.toString());
    }
}
