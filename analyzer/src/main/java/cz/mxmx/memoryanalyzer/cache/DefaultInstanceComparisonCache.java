package cz.mxmx.memoryanalyzer.cache;

import cz.mxmx.memoryanalyzer.model.InstanceDump;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultInstanceComparisonCache implements InstanceComparisonCache {

    private final Map<Long, Map<Long, Boolean>> cache = new HashMap<>();

    @Override
    public Optional<Boolean> instancesEqual(InstanceDump a, InstanceDump b) {
        InstanceDump first = a.getInstanceId() < b.getInstanceId() ? a : b;
        InstanceDump second = first == a ? b : a;

        return Optional.ofNullable(cache.get(first.getInstanceId()))
                .map(cachedComparison -> cachedComparison.get(second.getInstanceId()));
    }

    @Override
    public void cacheComparisonResult(InstanceDump a, InstanceDump b, boolean comparisonResult) {
        InstanceDump first = a.getInstanceId() < b.getInstanceId() ? a : b;
        InstanceDump second = first == a ? b : a;

        cacheResult(first.getInstanceId(), second.getInstanceId(), comparisonResult);
    }


    private void cacheResult(long instanceKey, long comparedTo, boolean comparisonResult) {
        Map<Long, Boolean> cachedComparison = cache.computeIfAbsent(instanceKey, a -> new HashMap<>());
        cachedComparison.put(comparedTo, comparisonResult);
    }
}
