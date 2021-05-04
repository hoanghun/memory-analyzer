package cz.mxmx.memoryanalyzer.process;

import cz.mxmx.memoryanalyzer.model.*;
import cz.mxmx.memoryanalyzer.model.raw.*;
import cz.mxmx.memoryanalyzer.util.Normalization;
import edu.tufts.eaftan.hprofparser.parser.datastructures.Value;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic memory dump processor to handle translation of raw data into processed data.
 */
public class GenericMemoryDumpProcessor implements MemoryDumpProcessor {

    /**
     * Translation map from string into class representation.
     */
    private static final Map<String, Class<?>> TYPE_TRANSLATION_MAP = new HashMap<String, Class<?>>() {{
        put("object", Object.class);
        put(null, Void.class);
        put("boolean", Boolean.class);
        put("char", Character.class);
        put("float", Float.class);
        put("double", Double.class);
        put("byte", Byte.class);
        put("short", Short.class);
        put("int", Integer.class);
        put("long", Long.class);
    }};
    /**
     * Cache to stop re-creating of the memory dumps.
     */
    private final Map<RawMemoryDump, MemoryDump> cache = new HashMap<>();

    /**
     * Translates a class name into an actual type.
     *
     * @param classType Class name (type).
     * @return Translated type or null.
     */
    public static Class<?> getClass(String classType) {
        String key = classType == null ? null : classType.toLowerCase();
        return TYPE_TRANSLATION_MAP.get(key);
    }

    public static Comparator<InstanceFieldDump<?>> fieldsComparator() {
        return (o1, o2) -> {
            if (o1.getType().equals(o2.getType())) return 0;
            if (o1.getType().equals(TYPE_TRANSLATION_MAP.get("object"))) return 1;
            if (o2.getType().equals(TYPE_TRANSLATION_MAP.get("object"))) return -1;
            return 0;
        };
    }

    @Override
    public MemoryDump process(RawMemoryDump rawMemoryDump) {
        if (this.cache.containsKey(rawMemoryDump)) {
            return this.cache.get(rawMemoryDump);
        }

        DumpHeader dumpHeader = this.getDumpHeader(rawMemoryDump.getRawDumpHeader());
        Map<Long, ClassDump> classes = this.getClasses(rawMemoryDump);
        Map<Long, InstanceDump> instances = this.getInstances(rawMemoryDump, classes);
        Map<Long, ArrayDump<?>> primitiveArrays = this.getPrimitiveArrays(rawMemoryDump.getRawPrimitiveArrayDumps());
        Map<Long, InstanceArrayDump> instanceArrays = this.getInstanceArrays(rawMemoryDump.getRawObjectArrayDumps(), instances, classes);
        List<AllocSiteParent> allocSites = this.getAllocSites(rawMemoryDump.getRawAllocSiteParents());
        List<StackTrace> stackTraces = this.getStackTraces(rawMemoryDump.getRawStackTraces(), rawMemoryDump.getRawStackFrames(), rawMemoryDump.getStringMap(), classes);

        ProcessedMemoryDump processedMemoryDump = new ProcessedMemoryDump()
                .setDumpHeader(dumpHeader)
                .setInstances(instances)
                .setClasses(classes)
                .setPrimitiveArrays(primitiveArrays)
                .setInstanceArrays(instanceArrays)
                .setAllocSites(allocSites)
                .setStackTraces(stackTraces);

        this.cache.put(rawMemoryDump, processedMemoryDump);

        return processedMemoryDump;
    }

    /**
     * Translates raw alloc site parents into processed ones.
     *
     * @param rawAllocSiteParents Raw alloc site parents.
     * @return Processed alloc site parents.
     */
    private List<AllocSiteParent> getAllocSites(List<RawAllocSiteParent> rawAllocSiteParents) {
        List<AllocSiteParent> allocSiteParents = new ArrayList<>();

        for (RawAllocSiteParent rawAllocSiteParent : rawAllocSiteParents) {
            List<AllocSite> sites = new ArrayList<>();

            for (RawAllocSite rawSite : rawAllocSiteParent.getSites()) {
                sites.add(new AllocSite(
                        rawSite.getArrayIndicator() == 1,
                        rawSite.getClassSerialNum(),
                        rawSite.getStackTraceSerialNum(),
                        rawSite.getLiveBytes(),
                        rawSite.getLiveInstances(),
                        rawSite.getBytesAllocated(),
                        rawSite.getInstancesAllocated()
                ));
            }

            allocSiteParents.add(new AllocSiteParent(
                    rawAllocSiteParent.getBitMaskFlags(),
                    rawAllocSiteParent.getCutoffRatio(),
                    rawAllocSiteParent.getLiveBytes(),
                    rawAllocSiteParent.getLiveInstances(),
                    rawAllocSiteParent.getBytesAllocated(),
                    rawAllocSiteParent.getInstancesAllocated(),
                    sites
            ));
        }

        return allocSiteParents;
    }

    /**
     * Translates raw stack traces into processed ones.
     *
     * @param rawStackTraces Raw stack traces.
     * @param rawStackFrames Raw stack frames.
     * @param stringMap      Map of strings to translate String IDs into their values.
     * @param classes        Classes.
     * @return Processed stack traces.
     */
    private List<StackTrace> getStackTraces(List<RawStackTrace> rawStackTraces, List<RawStackFrame> rawStackFrames, Map<Long, String> stringMap, Map<Long, ClassDump> classes) {
        Map<Long, StackFrame> idToRawStackFrame = rawStackFrames.stream().collect(Collectors.toMap(
                RawStackFrame::getStackFrameId,
                rawStackFrame -> new StackFrame(
                        rawStackFrame.getStackFrameId(),
                        stringMap.get(rawStackFrame.getMethodNameStringId()),
                        stringMap.get(rawStackFrame.getMethodSigStringId()),
                        stringMap.get(rawStackFrame.getSourceFileNameStringId()),
                        classes.values().stream().filter(c -> c.getSerialNum() == rawStackFrame.getClassSerialNum()).findAny().orElse(null),
                        rawStackFrame.getLocation()
                )
        ));

        return rawStackTraces.stream()
                .map(rawStackTrace -> new StackTrace(
                        rawStackTrace.getStackTraceSerialNum(),
                        rawStackTrace.getThreadSerialNum(),
                        rawStackTrace.getNumFrames(),
                        Arrays.stream(rawStackTrace.getStackFrameIds()).mapToObj(idToRawStackFrame::get).collect(Collectors.toList()))
                )
                .collect(Collectors.toList());
    }

    /**
     * Translates raw instance arrays into processed ones.
     *
     * @param rawObjectArrayDumps Raw instance dumps.
     * @param instanceDumpMap     Instance map.
     * @param classDumpMap        Class map.
     * @return Translated instance arrays.
     */
    private Map<Long, InstanceArrayDump> getInstanceArrays(Map<Long, RawObjectArrayDump> rawObjectArrayDumps, Map<Long, InstanceDump> instanceDumpMap, Map<Long, ClassDump> classDumpMap) {
        Map<Long, InstanceArrayDump> arrays = new HashMap<>();

        for (Map.Entry<Long, RawObjectArrayDump> entry : rawObjectArrayDumps.entrySet()) {
            Long key = entry.getKey();
            RawObjectArrayDump value = entry.getValue();
            List<Object> instances = value.getItems().stream()
                    .map(instanceDumpMap::get)
                    .collect(Collectors.toList());

            arrays.put(key, new InstanceArrayDump(key, classDumpMap.get(value.getItemClassObjectId()), instances));
        }

        return arrays;
    }

    /**
     * Translates raw primitive arrays into processed ones.
     *
     * @param rawPrimitiveArrayDumps Raw primitive arrays.
     * @return Processed primitive arrays.
     */
    private Map<Long, ArrayDump<?>> getPrimitiveArrays(Map<Long, RawPrimitiveArrayDump> rawPrimitiveArrayDumps) {
        return rawPrimitiveArrayDumps.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        it -> new ArrayDump<>(it.getKey(), getClass(it.getValue().getItemClassObjectId()), it.getValue().getItems()))
                );
    }

    /**
     * Translates raw instances into processed ones.
     *
     * @param rawMemoryDump Raw memory dump to process.
     * @param classes       Classes.
     * @return Processed instances.
     */
    protected Map<Long, InstanceDump> getInstances(RawMemoryDump rawMemoryDump, Map<Long, ClassDump> classes) {
        Map<Long, InstanceDump> instances = new HashMap<>();

        for (Map.Entry<Long, RawInstanceDump> entry : rawMemoryDump.getRawInstanceDumps().entrySet()) {
            Long key = entry.getKey();
            RawInstanceDump value = entry.getValue();
            ClassDump classDump = classes.get(value.getObjectClassId());
            InstanceDump instanceDump = new InstanceDump(key, classDump);
            classDump.addInstance(instanceDump);
            instances.put(key, instanceDump);
        }

        this.processInstanceFields(instances, rawMemoryDump);

        return instances;
    }

    /**
     * Translates instance fields into processed ones.
     *
     * @param instances     Instances to process.
     * @param rawMemoryDump Memory dump to use.
     */
    private void processInstanceFields(Map<Long, InstanceDump> instances, RawMemoryDump rawMemoryDump) {
        Queue<Long> queue = new LinkedList<>(instances.keySet());

        while (!queue.isEmpty()) {
            final boolean[] skip = {false};

            Long key = queue.poll();
            InstanceDump value = instances.get(key);

            RawInstanceDump rawInstanceDump = rawMemoryDump.getRawInstanceDumps().get(key);
            Map<String, Object> instanceValues = rawInstanceDump.getInstanceValues();

            Map<InstanceFieldDump<?>, Object> fieldsToAdd = new HashMap<>();

            instanceValues.forEach((fieldName, fieldValue) -> {

                Optional<InstanceFieldDump<?>> any = Optional.empty();
                ClassDump classDump = value.getClassDump();

                while (classDump != null && !any.isPresent()) {
                    any = classDump.getInstanceFields().stream()
                            .filter(field -> field.getFullyQualifiedName().equals(fieldName))
                            .findAny();

                    classDump = classDump.getSuperClassDump();
                }

                any.ifPresent(instanceFieldDump -> {
                    if (instanceFieldDump.getType().equals(Object.class) && ((Value<?>) fieldValue).value instanceof Long && instances.get(((Long) (((Value<?>) fieldValue).value))) != null) {
                        Long instanceDumpKey = (Long) ((Value<?>) fieldValue).value;
                        InstanceDump instanceDump = instances.get(instanceDumpKey);
                        if (this.isString(instanceDump)) {
                            if (instanceDump.getInstanceFieldValues().isEmpty()) {
                                queue.add(key);
                                skip[0] = true;
                                return;
                            }

                            fieldsToAdd.put(instanceFieldDump, this.extractString(instanceDump, rawMemoryDump.getRawPrimitiveArrayDumps()));
                        } else {
                            fieldsToAdd.put(instanceFieldDump, instanceDump);
                        }
                    } else {
                        fieldsToAdd.put(instanceFieldDump, fieldValue);
                    }
                });
            });

            if (skip[0]) {
                continue;
            }

            fieldsToAdd.forEach(value::addInstanceField);
        }
    }

    /**
     * Checks if an instance is String.
     *
     * @param instanceDump Instance to check
     * @return True if the instance is {@link String}.
     */
    private boolean isString(InstanceDump instanceDump) {
        return instanceDump.getClassDump().getName().equals(String.class.getName());
    }

    /**
     * Extracts a string from arrays.
     *
     * @param instanceDump          Instance to process.
     * @param primitiveArrayDumpMap Primitive arrays.
     * @return String from the array.
     */
    private String extractString(InstanceDump instanceDump, Map<Long, RawPrimitiveArrayDump> primitiveArrayDumpMap) {
        String extractedString = null;

        Optional<Long> key = instanceDump.getInstanceFieldValues().entrySet().stream()
                .filter(it -> Objects.equals("value", it.getKey().getName()))
                .filter(it -> it.getValue() instanceof Value && ((Value<?>) it.getValue()).value instanceof Long)
                .map(it -> (Long) ((Value<?>) it.getValue()).value)
                .findFirst();

        Optional<RawPrimitiveArrayDump> rawPrimitiveArrayDump = key.map(primitiveArrayDumpMap::get);
        if (rawPrimitiveArrayDump.isPresent()) {
            if ("byte".equals(rawPrimitiveArrayDump.get().getItemType())) {
                List<Object> values = rawPrimitiveArrayDump.get().getItems();
                byte[] bytes = new byte[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    if (values.get(i) instanceof Value<?>) {
                        bytes[i] = (Byte) ((Value<?>) values.get(i)).value;
                    }
                }
                extractedString = new String(bytes);
            } else {
                extractedString = this.charArrayToString(rawPrimitiveArrayDump.get().getItems());
            }
        }

        return extractedString;
    }

    /**
     * Translates char array into string.
     *
     * @param list Values of the array.
     * @return Created string.
     */
    private String charArrayToString(List<Object> list) {
        StringBuilder sb = new StringBuilder();
        list.forEach(sb::append);
        return sb.toString();
    }

    /**
     * Translates raw classes into processed ones.
     *
     * @param rawMemoryDump Memory dump to process.
     * @return Translated processed classes.
     */
    protected Map<Long, ClassDump> getClasses(RawMemoryDump rawMemoryDump) {
        Map<Long, ClassDump> classes = new HashMap<>();
        Queue<Long> unprocessedClasses = new LinkedList<>();

        new TreeMap<>(rawMemoryDump.getRawClassDumps()).forEach((key, value) -> {
            if (value.getSuperClassObjectId() != 0L && !classes.containsKey(value.getSuperClassObjectId())) {
                unprocessedClasses.add(key);
                return;
            }

            ClassDump classDump = this.processClass(rawMemoryDump, key, classes.get(value.getSuperClassObjectId()));
            classes.put(key, classDump);
        });

        int controlCount = unprocessedClasses.size();

        while (!unprocessedClasses.isEmpty()) {
            Long key = unprocessedClasses.poll();
            RawClassDump value = rawMemoryDump.getRawClassDumps().get(key);

            if (value.getSuperClassObjectId() != 0L
                    && (!classes.containsKey(value.getSuperClassObjectId())
                    || unprocessedClasses.contains(key))) {
                unprocessedClasses.add(key);
                controlCount--;

                if (controlCount == 0) {
                    break;
                }

                continue;
            }

            classes.put(key, this.processClass(rawMemoryDump, key, classes.get(value.getSuperClassObjectId())));
            controlCount = unprocessedClasses.size();
        }

        this.processClassFields(classes, rawMemoryDump);

        return classes;
    }

    /**
     * Process class fields - instance fields, static fields and constant fields.
     *
     * @param classes       Classes to process.
     * @param rawMemoryDump Raw memory dump to use.
     */
    private void processClassFields(Map<Long, ClassDump> classes, RawMemoryDump rawMemoryDump) {
        classes.forEach((key, value) -> {
                    RawClassDump rawClassDump = rawMemoryDump.getRawClassDumps().get(key);
                    rawClassDump.getInstanceFields().forEach((name, strType) -> value.addInstanceField(rawClassDump.getClassObjectId() + "." + name, name, getClass(strType)));
                    value.getInstanceFields().sort(fieldsComparator());
                }
        );

        classes.forEach((key, value) -> rawMemoryDump.getRawClassDumps().get(key).getStaticFields()
                .forEach((name, val) -> value.addStaticField(name, getClass(((Value<?>) val).type.toString()), ((Value<?>) val).value))
        );

        classes.forEach((key, value) -> rawMemoryDump.getRawClassDumps().get(key).getStaticFields()
                .forEach((name, val) -> value.addStaticField(name, getClass(((Value<?>) val).type.toString()), ((Value<?>) val).value))
        );
    }

    /**
     * Translate a raw class into processed one.
     *
     * @param rawMemoryDump Raw memory class.
     * @param id            ID of the class.
     * @param parent        Class parent.
     * @return Processed class.
     */
    protected ClassDump processClass(RawMemoryDump rawMemoryDump, Long id, ClassDump parent) {
        RawLoadClassDump rawLoadClassDump = rawMemoryDump.getRawLoadClassDumps().get(id);
        ClassDump classDump = new ClassDump(id, Normalization.getNormalizedClassname(rawLoadClassDump.getClassName()), rawLoadClassDump.getClassSerialNum(), parent);

        if (parent != null) {
            parent.addChildClass(classDump);
        }

        return classDump;
    }

    /**
     * Translate a raw header into processed one,
     *
     * @param rawDumpHeader Raw header.
     * @return Processed header.
     */
    protected DumpHeader getDumpHeader(RawDumpHeader rawDumpHeader) {
        return new DumpHeader(
                rawDumpHeader.getFormat(),
                rawDumpHeader.getIdSize(),
                Normalization.millisecondsToDate(rawDumpHeader.getTime())
        );
    }
}
