package cz.mxmx.memoryanalyzer.cache;

import cz.mxmx.memoryanalyzer.model.InstanceDump;

import java.util.Optional;

public class DummyInstanceComparisonCache implements InstanceComparisonCache {

    @Override
    public Optional<Boolean> instancesEqual(InstanceDump a, InstanceDump b) {
        return Optional.empty();
    }

    @Override
    public void cacheComparisonResult(InstanceDump a, InstanceDump b, boolean comparisonResult) {

    }
}
