package cz.mxmx.memoryanalyzer.parse;

import cz.mxmx.memoryanalyzer.model.raw.*;
import edu.tufts.eaftan.hprofparser.parser.datastructures.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Raw record processor.
 */
public class RawRecordHandler extends RecordHandler {

	/**
	 * Type ID to name map.
	 */
	private static final Map<Integer, String> TYPE_TRANSLATION_MAP = new HashMap<Integer, String>() {{
		put(2, "object");
		put(3, null);
		put(4, "boolean");
		put(5, "char");
		put(6, "float");
		put(7, "double");
		put(8, "byte");
		put(9, "short");
		put(10, "int");
		put(11, "long");
	}};

	private RawDumpHeader rawDumpHeader;
	private final Map<Long, String> stringMap = new HashMap<>();
	private final Map<Long, ClassInfo> classMap = new HashMap<>();
	private final Map<Long, RawLoadClassDump> rawLoadClassDumps = new HashMap<>();
	private final Map<Long, RawClassDump> rawClassDumps = new HashMap<>();
	private final Map<Long, RawInstanceDump> rawInstanceDumps = new HashMap<>();
	private final Map<Long, RawPrimitiveArrayDump> rawPrimitiveArrayDumps = new HashMap<>();
	private final Map<Long, RawObjectArrayDump> rawObjectArrayDumps = new HashMap<>();
	private final List<RawHeapSummary> rawHeapSummaries = new ArrayList<>();
	private final List<RawAllocSiteParent> rawAllocSiteParents = new ArrayList<>();
	private final List<RawStackTrace> rawStackTraces = new ArrayList<>();
	private final List<RawStackFrame> rawStackFrames = new ArrayList<>();

	/**
	 * Translates the type into its string name.
	 * @param type Type to translate.
	 * @return String name.
	 */
	private static String getBasicType(byte type) {
		return TYPE_TRANSLATION_MAP.get(Byte.valueOf(type).intValue());
	}

	@Override
	public RawMemoryDump getMemoryDump() {
		return new RawMemoryDump()
				.setRawDumpHeader(this.rawDumpHeader)
				.setStringMap(this.stringMap)
				.setClassMap(this.classMap)
				.setRawLoadClassDumps(this.rawLoadClassDumps)
				.setRawClassDumps(this.rawClassDumps)
				.setRawInstanceDumps(this.rawInstanceDumps)
				.setRawPrimitiveArrayDumps(this.rawPrimitiveArrayDumps)
				.setRawObjectArrayDumps(this.rawObjectArrayDumps)
				.setRawHeapSummaries(this.rawHeapSummaries)
				.setRawAllocSiteParents(this.rawAllocSiteParents)
				.setRawStackTraces(this.rawStackTraces)
				.setRawStackFrames(this.rawStackFrames);
	}

	@Override
	public void header(String format, int idSize, long time) {
		this.rawDumpHeader = new RawDumpHeader(format, idSize, time);
	}

	@Override
	public void stringInUTF8(long id, String data) {
		this.stringMap.put(id, data);
	}

	@Override
	public void loadClass(int classSerialNum, long classObjId, int stackTraceSerialNum, long classNameStringId) {
		this.rawLoadClassDumps.put(classObjId, new RawLoadClassDump(classObjId, this.stringMap.get(classNameStringId), classSerialNum));
	}

	@Override
	public void allocSites(short bitMaskFlags, float cutoffRatio, int totalLiveBytes, int totalLiveInstances, long totalBytesAllocated, long totalInstancesAllocated, AllocSite[] sites) {
		List<RawAllocSite> allocSites = Arrays
				.stream(sites)
				.map(s -> new RawAllocSite(
						s.arrayIndicator,
						s.classSerialNum,
						s.stackTraceSerialNum,
						s.numLiveBytes,
						s.numLiveInstances,
						s.numBytesAllocated,
						s.numInstancesAllocated)
				).collect(Collectors.toList());

		this.rawAllocSiteParents.add(
				new RawAllocSiteParent(
						bitMaskFlags,
						cutoffRatio,
						totalLiveBytes,
						totalLiveInstances,
						totalBytesAllocated,
						totalInstancesAllocated,
						allocSites
				)
		);
	}

	@Override
	public void heapSummary(int totalLiveBytes, int totalLiveInstances, long totalBytesAllocated, long totalInstancesAllocated) {
		this.rawHeapSummaries.add(new RawHeapSummary(totalLiveBytes, totalLiveInstances, totalBytesAllocated, totalInstancesAllocated));
	}

	@Override
	public void classDump(long classObjId, int stackTraceSerialNum, long superClassObjId, long classLoaderObjId, long signersObjId, long protectionDomainObjId, long reserved1, long reserved2, int instanceSize, Constant[] constants, Static[] statics, InstanceField[] instanceFields) {
		RawClassDump dummy = new RawClassDump(classObjId, superClassObjId, instanceSize);
		this.rawClassDumps.put(classObjId, dummy);
		int length = constants.length;

		int i;
		for (i = 0; i < length; ++i) {
			Constant c = constants[i];
			dummy.addConstantField(c.constantPoolIndex, c.value);
		}

		length = statics.length;

		for (i = 0; i < length; ++i) {
			Static s = statics[i];
			dummy.addStaticField(this.stringMap.get(s.staticFieldNameStringId), s.value);
		}

		length = instanceFields.length;

		for (i = 0; i < length; ++i) {
			InstanceField field = instanceFields[i];
			dummy.addInstanceField(this.stringMap.get(field.fieldNameStringId), field.type.toString());
		}

		this.classMap.put(classObjId, new ClassInfo(classObjId, superClassObjId, instanceSize, instanceFields));
	}

	@Override
	public void instanceDump(long objId, int stackTraceSerialNum, long classObjId, Value<?>[] instanceFieldValues) {
		RawInstanceDump rawInstanceDump = new RawInstanceDump(objId, classObjId);
		this.rawInstanceDumps.put(objId, rawInstanceDump);

		if (instanceFieldValues.length > 0) {
			int i = 0;
			long nextClass = classObjId;

			while (nextClass != 0L) {
				ClassInfo ci = this.classMap.get(nextClass);
				nextClass = ci.superClassObjId;
				InstanceField[] var11 = ci.instanceFields;

				for (InstanceField field : var11) {
					rawInstanceDump.addInstanceValue(ci.classObjId + "." + this.stringMap.get(field.fieldNameStringId), instanceFieldValues[i]);
					++i;
				}
			}
		}

	}

	@Override
	public void objArrayDump(long objId, int stackTraceSerialNum, long elemClassObjId, long[] elements) {
		RawObjectArrayDump arrayDummy = new RawObjectArrayDump(objId, elemClassObjId);
		this.rawObjectArrayDumps.put(objId, arrayDummy);

		for (long element : elements) {
			arrayDummy.addItem(element);
		}

	}

	@Override
	public void primArrayDump(long objId, int stackTraceSerialNum, byte elemType, Value<?>[] elements) {
		RawPrimitiveArrayDump arrayDummy = new RawPrimitiveArrayDump(objId, getBasicType(elemType));
		this.rawPrimitiveArrayDumps.put(objId, arrayDummy);

		for (Value<?> elem : elements) {
			arrayDummy.addItem(elem);
		}

	}

	@Override
	public void stackFrame(long stackFrameId, long methodNameStringId, long methodSigStringId, long sourceFileNameStringId, int classSerialNum, int location) {
		this.rawStackFrames.add(new RawStackFrame(stackFrameId, methodNameStringId, methodSigStringId, sourceFileNameStringId, classSerialNum, location));
	}

	@Override
	public void stackTrace(int stackTraceSerialNum, int threadSerialNum, int numFrames, long[] stackFrameIds) {
		this.rawStackTraces.add(new RawStackTrace(stackTraceSerialNum, threadSerialNum, numFrames, stackFrameIds));
	}
}
