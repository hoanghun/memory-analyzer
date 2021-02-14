package cz.mxmx.memoryanalyzer.model.raw;

/**
 * Raw "alloc site" data representation.
 */
public class RawAllocSite {
	private final byte arrayIndicator;
	private final int classSerialNum;
	private final int stackTraceSerialNum;
	private final int liveBytes;
	private final int liveInstances;
	private final int bytesAllocated;
	private final int instancesAllocated;

	/**
	 * Creates new alloc site data representation.
	 * @param arrayIndicator 1 if the type is an array or 0.
	 * @param classSerialNum Class serial number.
	 * @param stackTraceSerialNum Stack trace serial number.
	 * @param liveBytes Number of "live" bytes.
	 * @param liveInstances Number of "live" instances.
	 * @param bytesAllocated Number of allocated bytes.
	 * @param instancesAllocated Number of allocated instances.
	 */
	public RawAllocSite(byte arrayIndicator, int classSerialNum, int stackTraceSerialNum, int liveBytes, int liveInstances, int bytesAllocated, int instancesAllocated) {
		this.arrayIndicator = arrayIndicator;
		this.classSerialNum = classSerialNum;
		this.stackTraceSerialNum = stackTraceSerialNum;
		this.liveBytes = liveBytes;
		this.liveInstances = liveInstances;
		this.bytesAllocated = bytesAllocated;
		this.instancesAllocated = instancesAllocated;
	}

	public byte getArrayIndicator() {
		return arrayIndicator;
	}

	public int getClassSerialNum() {
		return classSerialNum;
	}

	public int getStackTraceSerialNum() {
		return stackTraceSerialNum;
	}

	public int getLiveBytes() {
		return liveBytes;
	}

	public int getLiveInstances() {
		return liveInstances;
	}

	public int getBytesAllocated() {
		return bytesAllocated;
	}

	public int getInstancesAllocated() {
		return instancesAllocated;
	}
}
