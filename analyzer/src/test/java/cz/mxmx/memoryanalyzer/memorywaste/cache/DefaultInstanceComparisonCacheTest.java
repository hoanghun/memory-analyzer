package cz.mxmx.memoryanalyzer.memorywaste.cache;

import cz.mxmx.memoryanalyzer.cache.DefaultInstanceComparisonCache;
import cz.mxmx.memoryanalyzer.cache.InstanceComparisonCache;

public class DefaultInstanceComparisonCacheTest extends InstanceComparisonCacheTest {

    @Override
    public InstanceComparisonCache createCache() {
        return new DefaultInstanceComparisonCache();
    }
}
