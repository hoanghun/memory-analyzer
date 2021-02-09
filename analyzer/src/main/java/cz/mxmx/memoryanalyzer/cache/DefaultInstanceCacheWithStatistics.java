package cz.mxmx.memoryanalyzer.cache;

import cz.mxmx.memoryanalyzer.model.InstanceDump;

import java.util.Optional;

public class DefaultInstanceCacheWithStatistics implements InstanceComparisonCacheWithStatistics {
    private final InstanceComparisonCache cache;

    private long cacheHit = 0;
    private long cacheMiss = 0;

    public DefaultInstanceCacheWithStatistics(InstanceComparisonCache cache) {
        this.cache = cache;
    }

    @Override
    public Optional<Boolean> instancesEqual(InstanceDump a, InstanceDump b) {
        Optional<Boolean> result = cache.instancesEqual(a, b);
        if (result.isPresent()) {
            cacheHit++;
        } else {
            cacheMiss++;
        }

        return result;
    }

    @Override
    public void cacheComparisonResult(InstanceDump a, InstanceDump b, boolean comparisonResult) {
        cache.cacheComparisonResult(a, b, comparisonResult);
    }

    @Override
    public long getCacheHitCount() {
        return cacheHit;
    }

    @Override
    public long getCacheMissCount() {
        return cacheMiss;
    }
}
