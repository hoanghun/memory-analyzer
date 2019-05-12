package cz.mxmx.memoryanalyzer.model;

import java.util.List;

public class AllocSiteParent {
	private final short bitMaskFlags;
	private final float cutoffRatio;
	private final int liveBytes;
	private final int liveInstances;
	private final long bytesAllocated;
	private final long instancesAllocated;
	private final List<AllocSite> sites;

	public AllocSiteParent(short bitMaskFlags, float cutoffRatio, int liveBytes, int liveInstances, long bytesAllocated, long instancesAllocated, List<AllocSite> sites) {
		this.bitMaskFlags = bitMaskFlags;
		this.cutoffRatio = cutoffRatio;
		this.liveBytes = liveBytes;
		this.liveInstances = liveInstances;
		this.bytesAllocated = bytesAllocated;
		this.instancesAllocated = instancesAllocated;
		this.sites = sites;
	}

	public short getBitMaskFlags() {
		return bitMaskFlags;
	}

	public float getCutoffRatio() {
		return cutoffRatio;
	}

	public int getLiveBytes() {
		return liveBytes;
	}

	public int getLiveInstances() {
		return liveInstances;
	}

	public long getBytesAllocated() {
		return bytesAllocated;
	}

	public long getInstancesAllocated() {
		return instancesAllocated;
	}

	public List<AllocSite> getSites() {
		return sites;
	}
}