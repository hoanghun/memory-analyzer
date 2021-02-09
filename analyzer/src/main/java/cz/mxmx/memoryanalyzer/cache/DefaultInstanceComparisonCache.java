package cz.mxmx.memoryanalyzer.cache;

import cz.mxmx.memoryanalyzer.model.InstanceDump;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultInstanceComparisonCache implements InstanceComparisonCache {

    private final Map<Long, Map<Long, Boolean>> cache = new HashMap<>();
    private long cacheHit = 0;
    private long cacheMiss = 0;

    @Override
    public Optional<Boolean> instancesEqual(InstanceDump a, InstanceDump b) {
        InstanceDump first = a.getInstanceId() < b.getInstanceId() ? a : b;
        InstanceDump second = first == a ? b : a;

        Optional<Boolean> result =  Optional.ofNullable(cache.get(first.getInstanceId()))
                .map(cachedComparison -> cachedComparison.get(second.getInstanceId()));
        if (result.isPresent()) {
            cacheHit++;
        } else {
            cacheMiss++;
        }
        return result;
    }

    @Override
    public void cacheComparisonResult(InstanceDump a, InstanceDump b, boolean comparisonResult) {
        InstanceDump first = a.getInstanceId() < b.getInstanceId() ? a : b;
        InstanceDump second = first == a ? b : a;

        cacheResult(first.getInstanceId(), second.getInstanceId(), comparisonResult);
    }

    @Override
    public long getCacheHitCount() {
        return cacheHit;
    }

    @Override
    public long getCacheMissCount() {
        return cacheMiss;
    }

    private void cacheResult(long instanceKey, long comparedTo, boolean comparisonResult) {
        Map<Long, Boolean> cachedComparison = cache.computeIfAbsent(instanceKey, a -> new HashMap<>());
        cachedComparison.put(comparedTo, comparisonResult);
    }
}
