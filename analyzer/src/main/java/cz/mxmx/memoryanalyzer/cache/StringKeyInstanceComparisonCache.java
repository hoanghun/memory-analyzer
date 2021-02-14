package cz.mxmx.memoryanalyzer.cache;

import cz.mxmx.memoryanalyzer.model.InstanceDump;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StringKeyInstanceComparisonCache implements InstanceComparisonCache {

    private final Map<String, Boolean> cache = new HashMap<>();

    @Override
    public Optional<Boolean> instancesEqual(InstanceDump a, InstanceDump b) {
        return Optional.ofNullable(cache.get(createKey(a, b)));
    }

    @Override
    public void cacheComparisonResult(InstanceDump a, InstanceDump b, boolean comparisonResult) {
        cache.put(createKey(a, b), comparisonResult);
    }

    private String createKey(InstanceDump a, InstanceDump b) {
        InstanceDump first = a.getInstanceId() < b.getInstanceId() ? a : b;
        InstanceDump second = first == a ? b : a;
        return first.getInstanceId() + ":" + second.getInstanceId();
    }
}
